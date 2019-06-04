pipeline {
    agent any
    tools {
        maven 'default'
    }
    environment {
        COMMIT_HASH_SHORT = gitVars 'commitHashShort'
        ZONE = 'fss'
    }
    stages {
        stage('initialize'){
            steps {
                ciSkip 'check'
                script {
                    pom = readMavenPom file: 'pom.xml'
                    applicationName = "${pom.artifactId}"
                    applicationVersion = "${pom.version}"
                    env.APPLICATION_NAME = "${applicationName}"
                    env.APPLICATION_VERSION = "${applicationVersion}.${env.BUILD_ID}-${env.COMMIT_HASH_SHORT}"
                   
                    changeLog = utils.gitVars(env.APPLICATION_NAME).changeLog.toString()
                    slackStatus status: 'started', changeLog: "${changeLog}"
                }
            }
        }
        stage('build') {
            steps {
                sh 'mvn -B -DskipTests clean package'
            }
        }
        stage('run tests (unit & intergration)') {
            steps {
                sh 'mvn verify'
                slackStatus status: 'passed'
            }
        }

        stage('push docker image') {
            steps {
                dockerUtils 'createPushImage'
            }
        }
        stage('validate & upload nais.yaml to nexus') {
            steps {
                nais action: 'validate'
                nais action: 'upload'
            }
        }
        stage('deploy to nais test') {
            environment {
                FASIT_ENV = 't4'
                NAMESPACE = 't4'
            }
            steps {
                script {
                    deployApplication()
                    waitForCallback()
                }
            }
        }
        stage('deploy to nais preprod') {        	
            environment {
                FASIT_ENV = 'q1'
                NAMESPACE = 'default'
            }
            steps {
                script {
                    deployApplication()
                    waitForCallback()
                }
            }
        }
       /* skal aldri deployes til prod!! */
    }
    post {
        always {
            ciSkip 'postProcess'
            archiveArtifacts artifacts: '**/target/*.jar', allowEmptyArchive: true
            junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
            junit testResults: '**/target/failsafe-reports/*.xml', allowEmptyResults: true
            script {
                if (currentBuild.result == 'ABORTED') {
                    slackStatus status: 'aborted'
                }
            }
            dockerUtils 'pruneBuilds'
            deleteDir()
        }
        success {
            slackStatus status: 'success'
        }
        failure {
            slackStatus status: 'failure'
        }
    }
}

void deployApplication() {
    def jiraIssueId = nais action: 'jiraDeploy'
    slackStatus status: 'deploying', jiraIssueId: "${jiraIssueId}"
    return jiraIssueId
}

void waitForCallback() {
    try {
        timeout(time: 1, unit: 'HOURS') {
            input id: "deploy", message: "Waiting for remote Jenkins server to deploy the application..."
        }
    } catch (Exception exception) {
        currentBuild.description = "Deploy failed, see " + currentBuild.description
        throw exception
    }
}
