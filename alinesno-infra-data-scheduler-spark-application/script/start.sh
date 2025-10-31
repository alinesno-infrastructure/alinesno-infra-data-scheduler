#!/bin/bash
APP_HOME=$(cd "$(dirname "$0")/.." && pwd)
LIB_DIR="/lib"

mkdir -p "$APP_HOME/logs"
mkdir -p "$APP_HOME/run"

# 应用Jar包
JAR=${APP_HOME}${LIB_DIR}/${JAR_NAME}

export JASYPT_ENCRYPTOR_PASSWORD=${JASYPT_ENCRYPTOR_PASSWORD}

# Java启动参数
JAVA_OPTS="-XX:+IgnoreUnrecognizedVMOptions \
--add-opens=java.base/java.lang=ALL-UNNAMED \
--add-opens=java.base/java.lang.invoke=ALL-UNNAMED \
--add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
--add-opens=java.base/java.io=ALL-UNNAMED \
--add-opens=java.base/java.net=ALL-UNNAMED \
--add-opens=java.base/java.nio=ALL-UNNAMED \
--add-opens=java.base/java.util=ALL-UNNAMED \
--add-opens=java.base/java.util.concurrent=ALL-UNNAMED \
--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED \
--add-opens=java.base/jdk.internal.ref=ALL-UNNAMED \
--add-opens=java.base/sun.nio.ch=ALL-UNNAMED \
--add-opens=java.base/sun.nio.cs=ALL-UNNAMED \
--add-opens=java.base/sun.security.action=ALL-UNNAMED \
--add-opens=java.base/sun.util.calendar=ALL-UNNAMED \
-Djdk.reflect.useDirectMethodHandle=false \
-Dspark.executor-jar.spark-sql-job-jar=${SPARK_SQL_EXECUTOR_JAR} \
-Dspark.spark-home=${SPARK_HOME} \
-Dspark.admin-users=${SPARK_ADMIN_USERS} \
-Dspark.master=${SPARK_MASTER} \
-Dspark.oss.endpoint=ENC(${OSS_ENDPOINT}) \
-Dspark.oss.accessKeyId=ENC(${OSS_ACCESS_KEY_ID}) \
-Dspark.oss.accessKeySecret=ENC(${OSS_ACCESS_KEY_SECRET})"

echo "Using JAR: $JAR"
nohup java $JAVA_OPTS -Xms1g -Xmx2g -jar "$JAR" >> "$APP_HOME/logs/app.log" 2>&1 &
echo $! > "$APP_HOME/run/app.pid"