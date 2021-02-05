pipeline {
    agent { label 'ant' }

    environment {
        BUILD_TARGET = 'clean default'
        BUILD_FILE = 'Code/importerjson/build.xml'
        GITLAB_TOKEN = credentials('monetdb-import-gitlab-token')
        SCANNER_HOME = tool name: 'SonarQube Scanner 3', type: 'hudson.plugins.sonar.SonarRunnerInstallation'
        VALIDATE_IMAGE = "${env.DOCKER_REGISTRY}/gros-monetdb-import-validate"
    }

    options {
        gitLabConnection('gitlab')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }
    triggers {
        gitlab(triggerOnPush: true, triggerOnMergeRequest: true, branchFilterType: 'All', secretToken: env.GITLAB_TOKEN)
        cron('H H * * H/3')
    }

    post {
        failure {
            updateGitlabCommitStatus name: env.JOB_NAME, state: 'failed'
        }
        aborted {
            updateGitlabCommitStatus name: env.JOB_NAME, state: 'canceled'
        }
        always {
            archiveArtifacts artifacts: 'Code/importerjson/dist/**', excludes: 'Code/importerjson/dist/javadoc/**', onlyIfSuccessful: true
            publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, keepAll: false, reportDir: 'Code/importerjson/dist/javadoc', reportFiles: 'index.html', reportName: 'Javadoc', reportTitles: ''])
            publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, includes: 'junit/**/*,html/**/*', keepAll: false, reportDir: 'Code/importerjson/build/test', reportFiles: 'html/htmlReport.html', reportName: 'JUnit Results', reportTitles: ''])
            publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, keepAll: false, reportDir: 'Code/importerjson/build/test/jacoco', reportFiles: 'index.html', reportName: 'JaCoCo coverage', reportTitles: ''])
            publishHTML([allowMissing: false, alwaysLinkToLastBuild: true, keepAll: false, reportDir: 'owasp-dep/', reportFiles: 'dependency-check-report.html', reportName: 'Dependencies', reportTitles: ''])
            junit 'Code/importerjson/build/test/results/*.xml'
        }
    }

    stages {
        stage('Start') {
            when {
                expression {
                    currentBuild.rawBuild.getCause(hudson.triggers.TimerTrigger$TimerTriggerCause) == null
                }
            }
            steps {
                updateGitlabCommitStatus name: env.JOB_NAME, state: 'running'
            }
        }
        stage('Build') {
            steps {
                checkout scm
                withCredentials([file(credentialsId: 'monetdb-import-properties', variable: 'BUILD_PROPERTIES')]) {
                    withAnt(installation: 'Ant 1.10.1', jdk: 'JDK 8') {
                        sh "ant -buildfile $BUILD_FILE -propertyfile $BUILD_PROPERTIES $BUILD_TARGET"
                    }
                }
            }
        }
        stage('Dependency Check') {
            steps {
                dir('security') {
                    checkout changelog: false, poll: false, scm: [$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'CleanBeforeCheckout']], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/ICTU/security-tooling']]]
                    sh 'sed -i "s/\\r$//" *.sh'
                    sh 'cp ../Code/importerjson/suppression.xml suppression.xml'
                    sh 'mkdir -p -m 0777 "$HOME/OWASP-Dependency-Check/data/cache"'
                    sh 'mkdir -p -m 0777 "$WORKSPACE/owasp-dep"'
                    sh 'bash ./security_dependencycheck.sh "$WORKSPACE" "$WORKSPACE/owasp-dep" --exclude "**/.git/**" --exclude "**/coverage/**" --exclude "**/build/**" --exclude "**/dist/**"'
                }
            }
        }
        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh '${SCANNER_HOME}/bin/sonar-scanner -Dsonar.projectKey=monetdb-import:$BRANCH_NAME -Dsonar.projectName="MonetDB importer $BRANCH_NAME"'
                }
            }
        }
        stage('Build validate') {
            agent { label 'docker' }
            steps {
                sh 'docker build -t $VALIDATE_IMAGE Scripts'
                sh 'docker push $VALIDATE_IMAGE'
            }
        }
        stage('Validate') {
            agent {
                docker {
                    image '$VALIDATE_IMAGE'
                    args '-u root'
                }
            }
            environment {
                GIT_PYTHON_REFRESH = 'quiet'
            }
            steps {
                withCredentials([file(credentialsId: 'monetdb-import-settings', variable: 'VALIDATE_SETTINGS')]) {
                    sh 'pip install -r Scripts/requirements.txt'
                    sh 'pip install pylint'
                    sh 'pylint --disable=duplicate-code Scripts/*.py'
                    script {
                        def ret = sh script: 'cd Scripts && cp $VALIDATE_SETTINGS settings.cfg && python validate_schema.py --log WARNING --branch $BRANCH_NAME', returnStatus: true
                        if (ret == 2) {
                            currentBuild.result = 'UNSTABLE'
                        }
                        else if (ret != 0) {
                            currentBuild.result = 'FAILURE'
                            error("Validate state failed with exit code ${ret}")
                        }
                    }
                }
            }
        }
        stage('Status') {
            when {
                expression {
                    currentBuild.rawBuild.getCause(hudson.triggers.TimerTrigger$TimerTriggerCause) == null
                }
            }
            steps {
                updateGitlabCommitStatus name: env.JOB_NAME, state: 'success'
            }
        }
    }
}
