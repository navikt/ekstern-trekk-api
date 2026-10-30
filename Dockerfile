FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:96c314aa943f23ddf001bc1a33939faac8ae3e56838ecf1d5000d8c0fb1689e6
COPY build/libs/app.jar /app/app.jar
COPY build/resources/main/db/migration /app/db/migration
WORKDIR /app
USER nonroot
ENTRYPOINT ["java", "-jar", "app.jar"]
