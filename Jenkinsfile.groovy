@Library('semantic_releasing') _

podTemplate() {
    properties([
            buildDiscarder(
                    logRotator(artifactDaysToKeepStr: '',
                            artifactNumToKeepStr: '',
                            daysToKeepStr: '',
                            numToKeepStr: '30'
                    )
            ),
            pipelineTriggers([])
    ])


    def currentVersion = 'latest'
    def project = 'backend'
    def repo = 'https://github.com/robertBrem/openshift'

    node('maven') {
        stage('Checkout') {
            git url: repo
        }

        milestone 1
        lock(resource: 'git-tag', inversePrecedence: true) {
            milestone 2
            stage('Set new version') {
                currentVersion = semanticReleasing()
                currentBuild.displayName = currentVersion
                sh "mvn versions:set -DnewVersion=${currentVersion}"
            }

            stage('Tag this version') {
                sh "git config user.email \"jenkins@khinkali.ch\""
                sh "git config user.name \"Jenkins\""
                sh "git tag -a ${currentVersion} -m \"${currentVersion}\""
                withCredentials([usernamePassword(credentialsId: 'github', passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
                    sh "git push https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/robertbrem/openshift.git --tags"
                }
            }
        }

        stage('Build Backend') {
            sh 'mvn clean package'
            junit allowEmptyResults: true, testResults: '**/target/surefire-reports/TEST-*.xml'
        }

        stage('sonar analysis') {
            withCredentials([string(credentialsId: 'sonar-key', variable: 'SONAR_KEY')]) {
                withSonarQubeEnv('Sonar') {
                    container('maven') {
                        sh """
                            mvn sonar:sonar \
                             -Dsonar.host.url=http://sonar:9000 \
                             -Dsonar.login=${SONAR_KEY} \
                             -Dsonar.exclusions=**/Jenkinsfile.groovy,**/target/**
                           """
                    }
                }
            }
        }

        stage("Quality Gate") {
            timeout(time: 5, unit: 'MINUTES') {
                def qg = waitForQualityGate()
                if (qg.status != 'OK') {
                    error "Pipeline aborted due to quality gate failure: ${qg.status}"
                }
            }
        }

        stage('Build Image') {
            //  openshift.withProject('myproject') {
            //      openshift.apply(readFile('imageconfig.yml'))
            //  }
            // openshift.withProject('myproject') {
            //     openshift.apply(readFile('buildconfig.yml'))
            // }
            openshift.withProject('myproject') {
                def build = openshift.startBuild("${project}-docker --from-file=target/backend.war")
                build.untilEach(1) {
                    return (it.object().status.phase == 'Complete')
                }

                openshift.tag("myproject/${project}:latest myproject/${project}:${currentVersion}")
            }
        }

        milestone 3
        lock(resource: 'openshift-en-env', inversePrecedence: true) {
            milestone 4
            stage('Deploy on Dev') {
                openshift.withProject('myproject') {
                    def dc = openshift.selector('dc', project).object()
                    def oldResourceVersion = dc.metadata.resourceVersion.toInteger()
                    sh "sed -i -e 's/version: todo/version: ${currentVersion}/' deploymentconfig.yml"
                    sh "sed -i -e 's/value: \"todo\"/value: \"${currentVersion}\"/' deploymentconfig.yml"
                    sh "sed -i -e 's/resourceVersion: todo/resourceVersion: \"${oldResourceVersion}\"/' deploymentconfig.yml"
                    sh "sed -i -e 's~172.30.1.1:5000/myproject/backend:todo~172.30.1.1:5000/myproject/backend:${currentVersion}~' deploymentconfig.yml"
                    openshift.apply(readFile('deploymentconfig.yml'))
                    try {
                        openshift.selector('dc', project).rollout().latest()
                    } catch (e) {
                        echo "rollout latest failed: ${e.getMessage()}"
                    }

                    def labels = [app: 'backend', version: currentVersion]
                    waitForPods(openshift, labels)

                    sh "sed -i -e 's/resourceVersion: \"${oldResourceVersion}\"/resourceVersion: todo/' deploymentconfig.yml"

                    openshift.apply(readFile('service.yml'))
                    openshift.apply(readFile('route.yml'))
                }
            }

            stage('System Tests') {
                withEnv(['HOST=backend', 'PORT=8080']) {
                    sh "mvn clean integration-test failsafe:integration-test failsafe:verify"
                    junit allowEmptyResults: true, testResults: '**/target/failsafe-reports/TEST-*.xml'
                }
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
    // waitForPods(openshift, [app: 'backend', version: currentVersion])
}
