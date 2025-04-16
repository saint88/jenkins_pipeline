timeout(300) {
    node('maven') {
        currentBuild.description  = """
        BRANCH=${REFSPEC}
        Owner=${env.BUILD_USER}
        """

        stage('Checkout') {
            dir('api-tests') {
                checkout scm
            }
        }

        stage('Running tests') {
            exitCode = sh(
                script: 'sudo docker run --rm --name=ui_tests -t localhost:5005/ui_tests:1.0.0',
                returnStatus: true
            )
            
            if(exitCode > 0) {
                currentBuild.status = 'UNSTABLE'
            }
        }

        stage('Work with regexp') {
            def matcher = """test 12 string
            test 12 string
            """ =~ /.*?(\d+).*/
            echo matcher[0][1]
            echo matcher[1][1]

            def map = [:]
            map['key1'] = 'val1'
            map['key2'] = 'val2'

            map.each {k, v -> {
                echo "key=$k"
                echo "val=$v"
            }}
        }

        script('Publish allure report') {
            dir('api-tests') {
                allure([
                    includeProperties: false,
                    jdk: '',
                    properties: [],
                    reportBuildPolicy: 'ALWAYS',
                    results: [[path: './allure-results']]
                ])
            }
        }

        def configScriptPath = './config/config.py'

        stage('Generate job.ini config') {
            dir('api-tests') {
                withCredentials([usernamePassword(credentialsId: 'jobs_builder_creds', usernameVariable: 'username', passwordVariable: 'password')]) {
                    sh "USER=${username} PASSWORD=${password}  python3 ${configScriptPath}"
                }
            }
        }

        stage('Start update jobs') {
            dir('api-tests') {
                sh "jenkins-jobs --conf ./config/job.ini update ./jobs/"
            }
        }
}