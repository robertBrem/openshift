@Library('semantic_releasing') _

podTemplate() {
    applyProperties()

    def currentVersion = 'latest'
    def project = 'backend'

    node(nodeName) {
        stage('Checkout') {
            cleanWs()
            doCheckout('https://github.com/robertBrem/openshift')
        }

        milestone 1
        lock(resource: 'git-tag', inversePrecedence: true) {
            milestone 2
            stage('Set new version') {
                currentVersion = semanticReleasing()
                currentBuild.displayName = currentVersion
                mvn("versions:set -DnewVersion=${currentVersion}")
            }

            stage('Tag this version') {
                createTag(currentVersion, repo)
            }
        }

        stage('Build Backend') {
            mvn('clean package')
            publishUnitTests()
        }

        stage('Build Image') {
            openshift.withProject(project) {
                openshift.apply(readFile('buildconfig.yml'))
            }
            openshift.withProject(project) {
                def build = openshift.startBuild("${project}-docker --from-file=target/backend.war")
                waitForBuildToComplete(build)
                openshift.tag("myproject/${project}:latest myproject/${project}:${currentVersion}")
            }
        }

        milestone 3
        lock(resource: 'openshift-en-env', inversePrecedence: true) {
            milestone 4
            stage('Deploy on Dev') {
                openshift.withProject(project) {
                    def dc = openshift.selector('dc', appName).object()
                    def oldResourceVersion = dc.metadata.resourceVersion.toInteger()
                    sh "sed -i -e 's/version: todo/version: ${currentVersion}/' deploymentconfig.yml"
                    sh "sed -i -e 's/value: \"todo\"/value: \"${currentVersion}\"/' deploymentconfig.yml"
                    sh "sed -i -e 's/resourceVersion: todo/resourceVersion: \"${oldResourceVersion}\"/' deploymentconfig.yml"
                    sh "sed -i -e 's~172.30.1.1:5000/myproject/backend:todo~docker-172.30.1.1:5000/myproject/backend:${currentVersion}~' deploymentconfig.yml"
                    openshift.apply(readFile('deploymentconfig.yml'))
                    try {
                        openshift.selector('dc', appName).rollout().latest()
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
                withEnv(['HOST=en.agate.ch']) {
                    // mvn('clean integration-test failsafe:integration-test failsafe:verify')
                    // publishITests()
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
