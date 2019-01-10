# unless you are creating a generic Dockerfile that must stay up-to-date with the base image,
# provide specific tag.
FROM navikt/java:8
LABEL maintainer="hilde.kveim@nav.no"
COPY target/test-rest-sts.jar /app/app.jar
ENV LOGGBACK_REMOTE=true
ENV JAVA_OPTS=-Dlogback.configurationFile=logback-remote.xml
# Expose default http jetty port
EXPOSE 8080