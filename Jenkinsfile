pipeline {
    agent any

    environment {
        // 可按需配置 Maven / Node 版本或使用 Jenkins 全局工具
        MAVEN_OPTS = '-Dmaven.test.failure.ignore=false'
    }

    options {
        timestamps()
        ansiColor('xterm')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Backend: Maven Test & Package') {
            steps {
                dir('') {
                    sh '''
                    set -e
                    mvn -version
                    # 先执行单元测试，再打包后端各模块
                    mvn -pl oa-common,oa-user-service,oa-workflow-service,oa-portal,oa-gateway -am test
                    mvn -pl oa-common,oa-user-service,oa-workflow-service,oa-portal,oa-gateway -am package -DskipTests
                    '''
                }
            }
        }

        stage('Frontend: Build') {
            steps {
                dir('oa-web') {
                    sh '''
                    set -e
                    node -v || echo "node not found, please configure Node in Jenkins agent"
                    npm install
                    npm run build
                    '''
                }
            }
        }

        stage('Docker Build (optional)') {
            when {
                expression { return fileExists('deploy/docker-compose.yml') && fileExists('deploy/docker-compose.oa.yml') }
            }
            steps {
                dir('deploy') {
                    sh '''
                    set -e
                    docker -v || echo "docker not found, skip docker build"
                    if command -v docker >/dev/null 2>&1; then
                      docker compose -f docker-compose.yml -f docker-compose.oa.yml build
                    fi
                    '''
                }
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
        }
        success {
            echo 'Build & tests succeeded.'
        }
        failure {
            echo 'Build or tests failed.'
        }
    }
}

