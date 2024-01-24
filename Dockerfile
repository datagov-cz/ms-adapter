FROM openjdk:21-slim-bookworm as build
WORKDIR /opt/ms-adapter/
COPY ./ ./
RUN ./mvnw package

FROM openjdk:21-slim-bookworm
COPY --from=build --chown=user /opt/ms-adapter/dist /opt/ms-adapter/
