FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:de29e1096f77c928f4bd889312c60bea619bf786a17829dd9136d6282296a760
COPY build/libs/app.jar /app/app.jar
COPY build/resources/main/db/migration /app/db/migration
WORKDIR /app
USER nonroot
ENTRYPOINT ["java", "-jar", "app.jar"]
