FROM navikt/java:8
COPY target/test-rest-sts.jar /app/app.jar
ENV LOGGBACK_REMOTE=true
ENV JAVA_OPTS=-Dlogback.configurationFile=logback-remote.xml
# Expose default http jetty port
EXPOSE 8080