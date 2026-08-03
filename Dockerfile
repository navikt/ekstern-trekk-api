FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:8fd780f0952ed36011557eace4cab512bd1f26d153a07d8d20db59f94f9e737a
COPY build/libs/app.jar /app/app.jar
COPY build/resources/main/db/migration /app/db/migration
WORKDIR /app
USER nonroot
ENTRYPOINT ["java", "-jar", "app.jar"]
