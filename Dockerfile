# Build stage
FROM maven:3.6.0-jdk-11-slim AS build-stage

ENV DEBIAN_FRONTEND noninteractive

COPY ./src /usr/src/cleanstone/src
COPY ./pom.xml /usr/src/cleanstone/pom.xml
WORKDIR /usr/src/cleanstone
RUN mvn clean package

# Prod stage
FROM openjdk:11-jre-slim

LABEL maintainer="hi@bluemedia.dev"

ENV DEBIAN_FRONTEND noninteractive

RUN apt update -y && \
    apt upgrade -y && \
    apt clean && \
    rm -rf /var/lib/apt/lists/* && \
    mkdir /conf

COPY --from=build-stage /usr/src/cleanstone/target/*dependencies.jar /opt/cleanstone/cleanstone.jar
WORKDIR /opt/cleanstone

ENV CLEANSTONE_CONFIG /conf

RUN useradd --system --shell /usr/sbin/nologin cleanstone
RUN chown -R cleanstone:cleanstone /opt/cleanstone

USER cleanstone
CMD ["java", "-jar", "/opt/cleanstone/cleanstone.jar"]