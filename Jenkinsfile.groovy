@Library('semantic_releasing') _

podTemplate() {
    applyProperties()

    node(nodeName) {
        stage('Checkout') {
            cleanWs()
            doCheckout("https://git.isceco.admin.ch/${repo}.git")
        }

        milestone 1
        lock(resource: 'agate-git-tag', inversePrecedence: true) {
            milestone 2
            stage('Set new version') {
                env.VERSION = semanticReleasing()
                currentBuild.displayName = env.VERSION
                mvn("versions:set -DnewVersion=${env.VERSION}")
            }

            stage('Tag this version') {
                createTag(env.VERSION, repo)
            }
        }

        gitlabCommitStatus("build") {

            stage('Build Frontend') {
                def directory = 'src/main/frontend'
                npm('install', directory)
                npm('run build', directory)
            }

            stage('Build Backend') {
                mvn('clean package')
                publishUnitTests()
            }

            parallel(
                    'sonar': {
                        stage('Sonar') {
                            sonarScan(env.VERSION, 'agate18')
                        }
                        stage("Quality Gate") {
                            timeout(time: 5, unit: 'MINUTES') {
                                def qg = waitForQualityGate()
                                if (qg.status != 'OK') {
                                    error "Pipeline aborted due to quality gate failure: ${qg.status}"
                                }
                            }
                        }
                    },
                    'nexus': {
                        stage('Nexus IQ Scan') {
                            nexusIQ('agate18', 'portal.war')
                        }
                    }
            )

            stage('Build Image') {
                openshift.withProject(project) {
                    openshift.apply(readFile('buildconfig.yml'))
                }
                openshift.withProject(project) {
                    def build = openshift.startBuild("${appName}-docker --from-file=target/portal.war")
                    waitForBuildToComplete(build)
                    openshift.tag("${project}/${appName}:latest ${project}/${appName}:${env.VERSION}")
                }
            }
        }

        milestone 3
        lock(resource: 'openshift-en-env', inversePrecedence: true) {
            milestone 4
            gitlabCommitStatus("Test") {
                stage('Deploy on Dev') {
                    openshift.withProject(project) {
                        def dc = openshift.selector('dc', appName).object()
                        def oldResourceVersion = dc.metadata.resourceVersion.toInteger()
                        sh "sed -i -e 's/version: todo/version: ${env.VERSION}/' deploymentconfig.yml"
                        sh "sed -i -e 's/value: \"todo\"/value: \"${env.VERSION}\"/' deploymentconfig.yml"
                        sh "sed -i -e 's/resourceVersion: todo/resourceVersion: \"${oldResourceVersion}\"/' deploymentconfig.yml"
                        sh "sed -i -e 's~docker-registry.default.svc:5000/agate/backend:todo~docker-registry.default.svc:5000/agate/backend:${env.VERSION}~' deploymentconfig.yml"
                        openshift.apply(readFile('deploymentconfig.yml'))
                        try {
                            openshift.selector('dc', appName).rollout().latest()
                        } catch (e) {
                            echo "rollout latest failed: ${e.getMessage()}"
                        }

                        def labels = [app: 'backend', version: env.VERSION]
                        waitForPods(openshift, labels)

                        sh "sed -i -e 's/resourceVersion: \"${oldResourceVersion}\"/resourceVersion: todo/' deploymentconfig.yml"

                        openshift.apply(readFile('service.yml'))
                        openshift.apply(readFile('route.yml'))
                    }
                }

                //     parallel(
                //             'system tests': {
                stage('System Tests') {
                    withEnv(['HOST=en.agate.ch']) {
                        // mvn('clean integration-test failsafe:integration-test failsafe:verify')
                        // publishITests()
                    }
                }
                //           },
                //        'ui tests': {
                stage('UI Tests') {
                    withCredentials([usernamePassword(credentialsId: 'testuser', passwordVariable: 'password', usernameVariable: 'username')]) {
                        sh "sed -i -e 's/user: \"todo\"/user: \"${username}\"/' globals.js"
                        sh "sed -i -e 's/password: \"todo\"/password: \"${password}\"/' globals.js"
                    }
                    npm('install nightwatch -g')
                    nightwatch('UIT --env integration')
                    publishUITests()
                }
                //        },
                //    'performance tests': {

                stage('Upload To Nexus') {
                    nexusPublisher nexusInstanceId: 'nexus', nexusRepositoryId: 'releases',
                            packages: [[$class         : 'MavenPackage', mavenAssetList: [[classifier: '', extension: '', filePath: "${WORKSPACE}/target/portal.war"]],
                                        mavenCoordinate: [artifactId: "agate18", groupId: 'ch.admin.wbf', packaging: 'war', version: "${env.VERSION}"]]]
                }

                stage('Performance Tests') {
                    cleanWs()
                    doCheckout("https://git.isceco.admin.ch/blw/agate-testing.git")
                    mvn("clean gatling:integration-test")
                    archiveArtifacts artifacts: 'target/gatling/**/*.*', fingerprint: true
                    sh '''
                        mkdir target/site
                        cp -r target/gatling/healthsimulation*/* target/site
                        cd target/site
                        zip -r jsar.zip .
                        mv jsar.zip ../..
                       '''
                }
            }

            stage('Build Report Image') {
                openshift.withProject(project) {
                    def build = openshift.startBuild('gatling-docker --from-file=jsar.zip')
                    waitForBuildToComplete(build)
                    openshift.tag("${project}/gatling:latest ${project}/gatling:${env.VERSION}")
                }
            }

            stage('Deploy Testing on Dev') {
                openshift.withProject(project) {
                    def dc = openshift.selector('dc', 'gatling').object()
                    def oldResourceVersion = dc.metadata.resourceVersion.toInteger()
                    sh "sed -i -e 's/version: todo/version: ${env.VERSION}/' deploymentconfig.yml"
                    sh "sed -i -e 's/value: \"todo\"/value: \"${env.VERSION}\"/' deploymentconfig.yml"
                    sh "sed -i -e 's/resourceVersion: todo/resourceVersion: \"${oldResourceVersion}\"/' deploymentconfig.yml"
                    sh "sed -i -e 's~docker-registry.default.svc:5000/agate/gatling:todo~docker-registry.default.svc:5000/agate/gatling:${env.VERSION}~' deploymentconfig.yml"
                    openshift.apply(readFile('deploymentconfig.yml'))
                    openshift.selector('dc', appName).rollout().latest()

                    def labels = [app: 'backend', version: env.VERSION]
                    waitForPods(openshift, labels)

                    sh "sed -i -e 's/resourceVersion: \"${oldResourceVersion}\"/resourceVersion: todo/' deploymentconfig.yml"
                }
            }

            //       }
            //   )

            stage('Tag image with tested') {
                openshift.withProject(project) {
                    openshift.tag("${project}/${appName}:${env.VERSION} ${project}/${appName}:tested")
                }
                cleanWs()
            }


        }
    }
}

def waitForPods(def openshift, def labels) {
    timeout(5) {
        def pods = openshift.selector('pod', labels)
        def podObject
        while (podObject == null || !podObject.status.containerStatuses[0].ready) {
            try {
                podObject = pods.object()
            } catch (ex) {
                // forget
            }
            sleep 1
        }
    }
    // Evtl. funktioniert die saubere Lösung ja wiedermal in einer späteren Version
    // waitForPods(openshift, [app: 'backend', version: env.VERSION])
}
