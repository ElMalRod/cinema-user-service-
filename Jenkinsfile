pipeline {
    agent any

    environment {
        SERVICE_NAME  = 'cinema-users-service'
        SERVICE_PORT  = '8082'
        KAFKA_SERVERS = '18.188.55.33:9092'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Tests') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Code Coverage') {
            steps {
                sh 'mvn verify'
                script {
                    echo 'JaCoCo coverage checks passed (LINE >= 85% and BRANCH >= 85%).'
                }
            }
        }

        stage('Build JAR') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh "docker build -t ${SERVICE_NAME}:latest ."
            }
        }

        stage('Transfer Image') {
            steps {
                withCredentials([
                    string(credentialsId: 'EC2_SERVICES_HOST', variable: 'HOST')
                ]) {
                    sshagent(['SSH_DEPLOY_KEY']) {
                        sh '''
                            rm -f cinema-users-service.tar.gz || true

                            docker save cinema-users-service:latest | gzip > cinema-users-service.tar.gz

                            scp -o StrictHostKeyChecking=no \
                                cinema-users-service.tar.gz \
                                ubuntu@"$HOST":~/
                        '''
                    }
                }
            }
        }

        stage('Deploy') {
            steps {
                withCredentials([
                    string(credentialsId: 'EC2_SERVICES_HOST', variable: 'HOST'),
                    string(credentialsId: 'DB_URL_USERS', variable: 'DB_URL'),
                    string(credentialsId: 'DB_USERNAME', variable: 'DB_USER'),
                    string(credentialsId: 'DB_PASSWORD', variable: 'DB_PASS')
                ]) {
                    sshagent(['SSH_DEPLOY_KEY']) {
                        sh '''
                            ssh -o StrictHostKeyChecking=no ubuntu@"$HOST" "
                                set -e

                                docker load < ~/cinema-users-service.tar.gz

                                rm -f ~/cinema-users-service.tar.gz

                                docker stop cinema-users-service || true
                                docker rm cinema-users-service || true

                                docker run -d \
                                    --name cinema-users-service \
                                    --restart unless-stopped \
                                    --network cinema-network \
                                    -p 8082:8082 \
                                    -e SERVER_PORT=8082 \
                                    -e DB_URL='$DB_URL' \
                                    -e DB_USERNAME='$DB_USER' \
                                    -e DB_PASSWORD='$DB_PASS' \
                                    -e SPRING_KAFKA_BOOTSTRAP_SERVERS='18.188.55.33:9092' \
                                    -e AUTH_SERVICE_URL='http://cinema-auth-service:8081' \
                                    -e AUTH_SERVICE_BASE_URL='http://cinema-auth-service:8081' \
                                    cinema-users-service:latest
                            "
                        '''
                    }
                }
            }
        }

        stage('Health Check') {
            steps {
                withCredentials([
                    string(credentialsId: 'EC2_SERVICES_HOST', variable: 'HOST')
                ]) {
                    sshagent(['SSH_DEPLOY_KEY']) {
                        sh '''
                            ssh -o StrictHostKeyChecking=no ubuntu@"$HOST" "
                                sleep 20

                                docker ps | grep cinema-users-service

                                docker logs --tail 50 cinema-users-service
                            "
                        '''
                    }
                }
            }
        }
    }

    post {
        success {
            echo 'cinema-users-service deployed successfully'
        }

        failure {
            echo 'Pipeline failed for cinema-users-service'
        }

        always {
            sh 'rm -f cinema-users-service.tar.gz || true'
        }
    }
}