# ===== 构建阶段 =====
FROM maven:3.9-amazoncorretto-17 AS builder

# 配置阿里云 Maven 镜像源
COPY docker/settings.xml /root/.m2/settings.xml

WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

# ===== 运行阶段 =====
FROM amazoncorretto:17-alpine

WORKDIR /app
COPY --from=builder /build/target/draw-guess-server-2.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
