#!/bin/bash
APP_HOME=$(cd "$(dirname "$0")/.." && pwd)
LIB_DIR="$APP_HOME/lib"

# 找最近修改的 jar（如果只有一个也适用）
JAR=$(ls -1t "$LIB_DIR"/*.jar 2>/dev/null | head -n1)

# 或者按模式匹配（如果你知道 artifactId-version 格式）
# JAR=$(echo "$LIB_DIR"/my-artifact-*.jar | awk '{print $1}')

if [ -z "$JAR" ] || [ ! -f "$JAR" ]; then
  echo "ERROR: no jar found in $LIB_DIR"
  exit 1
fi

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

-Dspark.master=${SPARK_MASTER} \

-Doss.endpoint=${OSS_ENDPOINT} \
-Doss.accessKeyId=${OSS_ACCESS_KEY_ID} \
-Doss.accessKeySecret=${OSS_ACCESS_KEY_SECRET} \

-Dspark.catalog.uri= jdbc:mysql:///${DB_MYSQL_URL}/dev_alinesno_infra_data_lake_v100?characterEncoding=UTF-8&serverTimezone=GMT%2B8&allowMultiQueries=true \
-Dspark.catalog.jdbc.user=${DB_MYSQL_USRENAME} \
-Dspark.catalog.jdbc.password=${DB_MYSQL_PASSWORD}}"

echo "Using JAR: $JAR"
nohup java $JAVA_OPTS -Xms512m -Xmx1g -jar "$JAR" >> "$APP_HOME/logs/app.log" 2>&1 &
echo $! > "$APP_HOME/run/app.pid"