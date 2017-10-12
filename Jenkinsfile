pipeline {
    agent { label 'ant' }

    environment {
        BUILD_TARGET = 'default'
        BUILD_FILE = 'Code/importerjson/build.xml'
    }

    options {
        gitLabConnection('gitlab')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }
    triggers {
        gitlab(triggerOnPush: true, triggerOnMergeRequest: true, branchFilterType: 'All')
    }

    post {
        success {
            archiveArtifacts artifacts: 'Code/importerjson/dist/**', excludes: 'Code/importerjson/dist/javadoc/**', onlyIfSuccessful: true
            publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, keepAll: false, reportDir: 'Code/importerjson/dist/javadoc', reportFiles: 'index.html', reportName: 'Javadoc', reportTitles: ''])
            updateGitlabCommitStatus name: env.JOB_NAME, state: 'success'
        }
        failure {
            updateGitlabCommitStatus name: env.JOB_NAME, state: 'failed'
        }
    }

    stages {
        stage('Build') {
            steps {
                withCredentials([file(credentialsId: 'monetdb-import-properties', variable: 'BUILD_PROPERTIES')]) {
                    withAnt(installation: 'Ant 1.10.1', jdk: 'JDK 8') {
                        sh "ant -buildfile $BUILD_FILE -propertyfile $BUILD_PROPERTIES $BUILD_TARGET"
                    }
                }
            }
        }
        stage('Validate') {
            agent {
                docker {
                    image 'python:2.7-alpine'
                    args '-u root'
                }
            }
            environment {
                GIT_PYTHON_REFRESH = 'quiet'
            }
            steps {
                sh 'pip install pylint gitpython pymonetdb requests'
                sh 'pylint Scripts/*.py'
                sh 'cd Scripts && python validate_schema.py --log WARNING --branch $BRANCH_NAME'
            }
        }
    }
}
