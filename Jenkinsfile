pipeline {
    agent any

    triggers {
        pollSCM('H/2 * * * *')
    }

    options {
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
        skipDefaultCheckout(true)
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.RELEASE_ID = "${env.BUILD_NUMBER}-${sh(returnStdout: true, script: 'git rev-parse --short=8 HEAD').trim()}"
                }
            }
        }

        stage('Web validation and static build') {
            steps {
                sh 'npm ci --no-audit --no-fund'
                sh 'npm run test'
                sh 'npm run build:static'
            }
        }

        stage('Java test and package') {
            steps {
                dir('java') {
                    sh 'mvn --batch-mode --no-transfer-progress clean verify'
                    sh 'mvn --batch-mode --no-transfer-progress -DskipTests install'
                }
            }
        }

        stage('Archive') {
            steps {
                archiveArtifacts artifacts: 'java/skillport-server/target/skillport-server-*.jar,java/skillport-bridge/target/skillport-bridge-*.jar',
                        fingerprint: true
            }
        }

        stage('Build production image') {
            when {
                expression {
                    def branchName = env.BRANCH_NAME ?: env.GIT_BRANCH
                    branchName == 'main' || branchName == 'origin/main'
                }
            }
            steps {
                dir('java') {
                    sh '''mvn --batch-mode --no-transfer-progress \
                      -f skillport-server/pom.xml -DskipTests \
                      -Djib.to.image=10.43.50.40:5000/skillport/server:${RELEASE_ID} \
                      -Djib.allowInsecureRegistries=true \
                      com.google.cloud.tools:jib-maven-plugin:3.5.2:build'''
                }
            }
        }

        stage('Deploy production') {
            when {
                expression {
                    def branchName = env.BRANCH_NAME ?: env.GIT_BRANCH
                    branchName == 'main' || branchName == 'origin/main'
                }
            }
            steps {
                sh '''#!/usr/bin/env bash
                    set -Eeuo pipefail
                    /usr/local/sbin/skillport-k3s-deploy \
                      "10.43.50.40:5000/skillport/server:${RELEASE_ID}" \
                      "$WORKSPACE/java/skillport-bridge/target/skillport-bridge-1.0.0-SNAPSHOT.jar" \
                      "$RELEASE_ID"
                '''
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: 'java/**/target/surefire-reports/*.xml'
        }
        success {
            echo "SkillPort build ${BUILD_NUMBER} completed"
        }
        unsuccessful {
            echo "SkillPort build ${BUILD_NUMBER} failed; an unhealthy deployment is rolled back automatically"
        }
    }
}
