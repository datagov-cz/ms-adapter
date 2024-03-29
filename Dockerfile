FROM openjdk:21-slim-bookworm as build
WORKDIR /opt/ms-adapter/
COPY ./ ./
RUN chmod u+x ./mvnw && ./mvnw package

FROM openjdk:21-slim-bookworm
COPY --from=build /opt/ms-adapter/dist /opt/ms-adapter/
WORKDIR /opt/ms-adapter/