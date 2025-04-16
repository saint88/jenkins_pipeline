timeout(300) {
    node('python') {
        currentBuild.description  = """
        BRANCH=${REFSPEC}
        Owner=${BUILD_USER}
        """

        stage('Checkout') {
            dir('api-tests') {
                checkout scm
            }
        }

        def jobs = [:]
        def yamConfig = readYaml text: $YAML_CONFIG

        for(def type in yamConfig['TESTS_TYPE']) {
            jobs['type'] = node('python') {
                stage("Running ${type} tests") {
                    sh "docker run ...."
                }
            }
        }

        parallel jobs

        stage('Copy allure artifacts') {

            jobs.each { k, job -> {
                dir('allure-results') {
                    copyArtifacts filter: 'allure-reports.zip', fingerprintArtifacts: true, projectName: job.JOB_NAME, selector: specific(job.BUILD_NUMBER)
                }
            }}
            
        }
    }
}


def getNotifyMessage(statistic) {
    def message = "========= Report ==========\n"
    
    statistic.each {k, v ->
        message += "\t${k}: ${v}\n"
    }

    withCredentials(string([credentialsId: chat_id, var: chat_id]), string([credentialsId: token, var: botToken])) {
        sh "curl -s -X POST -H 'Content-Type: application/json' -d '{\"chat_id\": \"${chat_id}\", \"text\": \"${message}\"}' https://api.telegram.org/bot${botToken}/sendMessage"
    }
}