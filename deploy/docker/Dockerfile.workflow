# 需先在项目根目录执行：mvn package -DskipTests
# 构建：docker build -f deploy/docker/Dockerfile.workflow -t oa-workflow-service:local .
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY oa-workflow-service/target/oa-workflow-service-*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
