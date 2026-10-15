# ======================================================================
#  Owner IntelliPortal 镜像构建
#  运行镜像：eclipse-temurin:25-jre
#  堆参数  ：-Xms512m -Xmx512m    时区：Asia/Shanghai
# ======================================================================

# ---------------------------- 构建阶段 ----------------------------
FROM eclipse-temurin:25-jdk AS builder

ARG MAVEN_VERSION=3.9.11
ENV MAVEN_HOME=/opt/maven
ENV PATH="${MAVEN_HOME}/bin:${PATH}"

RUN set -eux; \
    apt-get update; \
    apt-get install -y --no-install-recommends curl ca-certificates unzip; \
    rm -rf /var/lib/apt/lists/*; \
    curl -fsSL -o /tmp/maven.zip \
      "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${MAVEN_VERSION}/apache-maven-${MAVEN_VERSION}-bin.zip"; \
    unzip -q /tmp/maven.zip -d /opt; \
    mv "/opt/apache-maven-${MAVEN_VERSION}" "${MAVEN_HOME}"; \
    rm -f /tmp/maven.zip; \
    mvn -v

WORKDIR /build

# 先只拷贝 pom，利用 Docker 层缓存加速依赖下载
COPY pom.xml ./
COPY owner-common/pom.xml    owner-common/pom.xml
COPY owner-framework/pom.xml owner-framework/pom.xml
COPY owner-system/pom.xml    owner-system/pom.xml
COPY owner-server/pom.xml    owner-server/pom.xml
RUN mvn -B -ntp -DskipTests dependency:go-offline || true

# 再拷贝源码完成构建
COPY . .
RUN mvn -B -ntp clean package -DskipTests

# ---------------------------- 运行阶段 ----------------------------
FROM eclipse-temurin:25-jre

LABEL maintainer="Owner IntelliPortal Team" \
      description="Owner IntelliPortal Server"

ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    SPRING_PROFILES_ACTIVE=docker \
    JAVA_OPTS="-Xms512m -Xmx512m \
      -XX:+UseG1GC -XX:MaxGCPauseMillis=200 \
      -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m \
      -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs \
      -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"

RUN set -eux; \
    ln -snf "/usr/share/zoneinfo/${TZ}" /etc/localtime; \
    echo "${TZ}" > /etc/timezone

WORKDIR /app
RUN mkdir -p /app/logs

COPY --from=builder /build/owner-server/target/owner-server.jar /app/app.jar

EXPOSE 41452

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/41452' || exit 1

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar /app/app.jar"]
