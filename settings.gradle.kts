rootProject.name = "ekstern-trekk-api"

dependencyResolutionManagement {
    versionCatalogs {
        create("testLibs") {
            from(files("gradle/testLibs.versions.toml"))
        }
    }
}
