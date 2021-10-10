FROM clojure:openjdk-11-lein-slim-buster AS utils
ARG APPNAME

WORKDIR /usr/src/utilities/
COPY ./utilities/ ./
RUN lein do install, deps

FROM clojure:openjdk-11-lein-slim-buster AS build
ARG APPNAME

COPY --from=utils /root/.m2/ /root/.m2/
WORKDIR /usr/src/$APPNAME/
COPY ./$APPNAME/project.clj ./
RUN lein deps

COPY ./$APPNAME/ ./
RUN lein uberjar &&\
    mv ./target/uberjar/*-standalone.jar app-standalone.jar &&\
    rm -rf ./target

FROM openjdk:11-jre-slim-buster
ARG APPNAME

WORKDIR /usr/src/
COPY ./services_uri.edn ./

WORKDIR /usr/src/$APPNAME/
COPY --from=build /usr/src/$APPNAME/app-standalone.jar /usr/src/$APPNAME/config.edn ./
CMD ["java", "-jar", "app-standalone.jar", "docker"]