import groovy.json.JsonSlurperClassic

timeout(360) {
    node('maven') {
        stage('Checkout') {
            checkout scm
        }

        def yamConfig = readYaml text: $YAML_CONFIG

        stage('Running tests') {
            def exitCode = sh(
                script: "docker run --network=host ...",
                returnStatus: true
            )

            if(exitCode > 0) {
                currentBuild.status = 'UNSTABLE'
            }
        }

        stage('Publish allure') {

        }

        stage('Send notification') {
            def report = readFile './allure-report/widgets/summary.json'
            def slurped = new JsonSlurperClassic().parseText(report)

            getNotifyMessage(slurped)
        }
    }
}