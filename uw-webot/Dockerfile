FROM mcr.microsoft.com/playwright/java:v1.58.0 AS builder
WORKDIR /builder
COPY target/*.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM mcr.microsoft.com/playwright/java:v1.58.0
WORKDIR /application
COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./

ENV JAVA_OPTS="" SPRING_OPTS=""

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar application.jar ${SPRING_OPTS}"]