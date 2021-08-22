FROM bitnami/gradle:7.2.0-debian-10-r4@sha256:f1247d9cf09b555de789b7a816aeba2feba336425d904553ec9fe58a2d55d3f5 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build
 
FROM adoptopenjdk/openjdk11:jre-11.0.11_9-alpine@sha256:5e58309a0699a54ddfd0d1968d186909d1e9a4b4e1c7c7d534c7c0d876f8d074
RUN apk add dumb-init
RUN mkdir /app
RUN mkdir /app/config
RUN addgroup --system javauser && adduser -S -s /bin/false -G javauser javauser
COPY --from=build /home/gradle/src/fxdemo-1.0.jar /app/java-application.jar
COPY config/* /app/config/
WORKDIR /app
RUN chown -R javauser:javauser /app
USER javauser
CMD "dumb-init" "java" "-jar" "java-application.jar" "-h" "$HOST" "-v" "$VPN" "-u" "$USER" "-p" "$PASSWORD" "-t" "$ROOT_TOPIC" "-i" "$SYMBOLS" "-f" "$FREQUENCY"