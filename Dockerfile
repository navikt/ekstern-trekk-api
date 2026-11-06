FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-25
COPY build/libs/app.jar /app/app.jar
COPY build/resources/main/db/migration /app/db/migration
WORKDIR /app
USER nonroot
ENTRYPOINT ["java", "-jar", "app.jar"]
