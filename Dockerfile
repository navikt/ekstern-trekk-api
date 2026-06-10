FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:8249a3d85d9151f3ac6e12e42862220dc5f2653f031a2c0fe5f10da67f3239ac
COPY build/libs/app.jar /app/app.jar
COPY build/resources/main/db/migration /app/db/migration
WORKDIR /app
USER nonroot
ENTRYPOINT ["java", "-jar", "app.jar"]
