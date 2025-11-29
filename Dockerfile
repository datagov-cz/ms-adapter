FROM eclipse-temurin:21 AS build
WORKDIR /opt/ms-adapter/
COPY ./ ./
RUN chmod u+x ./mvnw && ./mvnw package

FROM eclipse-temurin:21
COPY --from=build /opt/ms-adapter/dist /opt/ms-adapter/
WORKDIR /opt/ms-adapter/
