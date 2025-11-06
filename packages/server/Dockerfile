FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src

# 使用国内镜像加速 Maven 依赖与插件下载
RUN mkdir -p /root/.m2 \
    && echo '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"\
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"\
    xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0\
                        https://maven.apache.org/xsd/settings-1.0.0.xsd">\
  <mirrors>\
    <mirror>\
      <id>aliyunmaven</id>\
      <mirrorOf>*</mirrorOf>\
      <name>Aliyun Maven</name>\
      <url>https://maven.aliyun.com/repository/public</url>\
    </mirror>\
    <mirror>\
      <id>tencent</id>\
      <mirrorOf>*</mirrorOf>\
      <name>Tencent Cloud</name>\
      <url>https://mirrors.cloud.tencent.com/nexus/repository/maven-public/</url>\
    </mirror>\
  </mirrors>\
  <profiles>\
    <profile>\
      <id>enable-http</id>\
      <repositories>\
        <repository>\
          <id>central</id>\
          <url>https://repo1.maven.org/maven2</url>\
        </repository>\
      </repositories>\
      <pluginRepositories>\
        <pluginRepository>\
          <id>central</id>\
          <url>https://repo1.maven.org/maven2</url>\
        </pluginRepository>\
      </pluginRepositories>\
    </profile>\
  </profiles>\
  <activeProfiles>\
    <activeProfile>enable-http</activeProfile>\
  </activeProfiles>\
</settings>' > /root/.m2/settings.xml

RUN mvn -s /root/.m2/settings.xml -B -e -DskipTests clean package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# 安装 wget 用于健康检查
RUN apk add --no-cache wget
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

