pipeline {
    triggers {
        pollSCM '* * * * *'
        cron('H 2 * * *')
    }
    options {
        disableConcurrentBuilds()
        buildDiscarder(logRotator(daysToKeepStr: '40'))
        skipDefaultCheckout()
        timestamps()
        timeout(time: 60, unit: 'MINUTES')
        ansiColor('xterm')
    }
    agent {
        label 'docker'
    }
    environment {
        LIB_NAME = 'confluence-client'
        TRY_COUNT = 5
    }
    parameters {
        choice(name: 'DO_CLEAN', choices: 'auto\ntrue\nfalse\n', description: 'Whether or not to clean the workspace.')
        booleanParam(name: 'DO_TAG', defaultValue: true, description: 'Whether or not to tag in the remote repository.')
        booleanParam(name: 'DO_BUILD', defaultValue: true, description: 'Whether or not to perform a build.')
        booleanParam(name: 'DO_DEP_CHECK', defaultValue: false, description: 'Whether or not to perform a dependency vulnerability check.')
        booleanParam(name: 'DO_LIC_CHECK', defaultValue: false, description: 'Whether or not to perform a license compliance check.')
        booleanParam(name: 'DO_SONAR_SCAN', defaultValue: true, description: 'Whether or not to perform the SonarQube analysis.')
        booleanParam(name: 'DO_PUBLISH', defaultValue: true, description: 'Whether or not to publish the artifact.')
        booleanParam(name: 'DEBUG', defaultValue: false, description: 'Whether or not to enable DEBUG mode.')
        booleanParam(name: 'TRACE', defaultValue: false, description: 'Whether or not to enable TRACE mode.')
    }
    stages {
        stage('Pre') {
            parallel {
                stage('Version: docker') { steps { sh 'docker -v' } }
                stage('Version: git') { steps { sh 'git --version' } }
                stage('Version: make') { steps { sh 'make -v' } }
                stage('Version: curl') { steps { sh 'curl --version' } }
                stage('Determine Clean') {
                    steps {
                        script {
                            if (currentBuild.previousBuild == null) {
                                echo "No previous build. Setting FIRST_BUILD_OF_DAY=true"
                                env.FIRST_BUILD_OF_DAY = true
                            } else {
                                long previousBuildDays = currentBuild.previousBuild.startTimeInMillis / (1000 * 60 * 60 * 24)
                                long currentBuildDays = currentBuild.startTimeInMillis / (1000 * 60 * 60 * 24)
                                if (previousBuildDays != currentBuildDays) {
                                    echo "Previous build was before today. Setting FIRST_BUILD_OF_DAY=true"
                                    env.FIRST_BUILD_OF_DAY = true
                                } else {
                                    echo "Previous build was today. Setting FIRST_BUILD_OF_DAY=false"
                                    env.FIRST_BUILD_OF_DAY = false
                                }
                            }

                            env.DO_CLEAN = params.DO_CLEAN == 'true' || (params.DO_CLEAN != 'false' && env.FIRST_BUILD_OF_DAY)
                            echo "DO_CLEAN resolved to: ${env.DO_CLEAN}"
                        }
                    }
                }
                stage('Set Env Vars') {
                    steps {
                        script {
                            env.DEBUG = params.DEBUG ? "yes" : "no"
                            env.TRACE = params.TRACE ? "yes" : "no"
                        }
                    }
                }
            }
        }
        stage('Clean') {
            when { expression { env.DO_CLEAN != 'false' } }
            steps {
                deleteDir()
            }
        }
        stage('Checkout') {
            steps {
                retry(env.TRY_COUNT) {
                    timeout(time: 45, unit: 'SECONDS') {
                        checkout scm
                    }
                }
            }
        }
        stage('Fetch Building Blocks') {
            steps {
                retry(env.TRY_COUNT) {
                    timeout(time: 45, unit: 'SECONDS') {
                        sh 'make bblocks'
                    }
                }
            }
        }
        stage('Version') {
            steps {
                script {
                    env.MAJOR_VERSION = readFile file: '.version', encoding: 'UTF-8'
                    env.BUILD_VERSION = env.MAJOR_VERSION + '.' + env.BUILD_NUM
                }
                buildName "${env.BUILD_VERSION}"
                echo "${env.BBLOCK_NAME} ${env.BUILD_VERSION}"
            }
        }
        stage('Tag') {
            when { expression { params.DO_TAG } }
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: env.GIT_CREDS_ID, keyFileVariable: 'GIT_KEY')]) {
                    withEnv(["TAG=${env.LIB_NAME}-${env.BUILD_VERSION}"]) {
                        sh "git tag ${TAG}"
                        retry(env.TRY_COUNT) {
                            sh "git push origin ${TAG}"
                        }
                    }
                }
            }
        }
        stage("Build & Static Analysis") {
            parallel {
                stage('Build & Unit Tests') {
                    when { expression { params.DO_BUILD } }
                    steps {
                        sh 'rm -rf target'
                        withMaven(jdk: 'jdk-1.8', maven: 'apache-maven-3.6.2') {
                            sh 'mvn clean install'
                        }
                    }
                    post {
                        always {
                            archiveArtifacts artifacts: "target/${env.LIB_NAME}.jar", fingerprint: true
                            archiveArtifacts artifacts: 'target/**/TEST-*.xml', fingerprint: true, allowEmptyArchive: true
                            junit allowEmptyResults: true, testResults: 'target/**/TEST-*.xml'
                        }
                    }
                }
            }
        }
        stage('SonarQube Analysis') {
            when { expression { params.DO_SONAR_SCAN } }
            steps {
                withCredentials([string(credentialsId: env.SONAR_TOKEN_CREDS_ID, variable: 'SONAR_TOKEN')]) {
                    withMaven(jdk: 'jdk-1.8', maven: 'apache-maven-3.6.2') {
                        sh 'mvn sonar:sonar'
                    }
                }
            }
        }
        stage('Publish') {
            when { expression { params.DO_PUBLISH } }
            steps {
                withMaven(jdk: 'jdk-1.8', maven: 'apache-maven-3.6.2') {
                    sh 'mvn deploy'
                }
            }
        }
    }
}
