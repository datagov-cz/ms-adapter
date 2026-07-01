FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /opt/ms-adapter/
COPY ./ ./
RUN chmod u+x ./mvnw && ./mvnw package

FROM eclipse-temurin:25-jre-noble
COPY --from=build /opt/ms-adapter/dist /opt/ms-adapter/
WORKDIR /opt/ms-adapter/
