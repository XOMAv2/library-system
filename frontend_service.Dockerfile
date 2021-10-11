FROM clojure:openjdk-11-lein-slim-buster AS utils
WORKDIR /usr/src/utilities/
COPY ./utilities/ ./
RUN lein install

FROM theasp/clojurescript-nodejs:shadow-cljs-alpine as build
COPY --from=utils /root/.m2/ /root/.m2/
WORKDIR /usr/src/frontend_service/
COPY ./frontend_service/ ./
RUN npm install &&\
    npx shadow-cljs release app

FROM node:alpine
WORKDIR /usr/src/frontend_service/
COPY --from=build /usr/src/frontend_service/resources/public ./
RUN npm install serve
CMD ["npx", "serve"]