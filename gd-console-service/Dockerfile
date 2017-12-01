FROM eclipse-temurin:17-jdk-jammy

## 创建目录，并使用它作为工作目录
RUN mkdir -p /gd-console-service
WORKDIR /gd-console-service
## 将后端项目的 Jar 文件，复制到镜像中
COPY ./target/gd-console-service.jar app.jar

## 设置 TZ 时区
## 设置 JAVA_OPTS 环境变量，可通过 docker run -e "JAVA_OPTS=" 进行覆盖
ENV TZ=Asia/Shanghai JAVA_OPTS="-Xms1023m -Xmx1024m"


EXPOSE 8861 8002


## 启动后端项目
#CMD java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar app.jar
#CMD java --add-opens java.base/java.lang=ALL-UNNAMED -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8002 ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar app.jar
#CMD java --add-opens java.base/java.lang=ALL-UNNAMED -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=0.0.0.0:8002 ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar app.jar
CMD java --add-opens java.base/java.lang=ALL-UNNAMED -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=*:8002 ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar app.jar

## java --add-opens java.base/java.lang=ALL-UNNAMED -jar your_application.jar
