# 基于主机预构建的 JAR 打包 Docker 镜像
# 在运行 docker build 前，先在主机上执行:
#   mvn clean package -DskipTests
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN mkdir -p /app/uploads
COPY smart-health-consultation/target/smart-health-consultation-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
