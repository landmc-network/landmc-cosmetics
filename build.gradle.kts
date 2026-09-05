plugins {
    base
}

group = providers.gradleProperty("group").get()
version = providers.gradleProperty("version").get()

val githubUser: String? = providers.gradleProperty("gpr.user").orNull
    ?: System.getenv("GITHUB_ACTOR")
val githubToken: String? = providers.gradleProperty("gpr.token").orNull
    ?: System.getenv("GITHUB_TOKEN")

subprojects {
    apply(plugin = "java-library")

    group = rootProject.group
    version = rootProject.version

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion = JavaLanguageVersion.of(25)
    }

    repositories {
        // Declared by the settings file; nothing project-specific belongs here.
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        // Paper 26.2, Velocity 4 and the platform modules for both are compiled for Java 25;
        // an older --release cannot read them.
        options.release = 25
        options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked", "-parameters"))
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            events("failed", "skipped")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }
}

project(":cosmetics-api") {
    apply(plugin = "maven-publish")

    extensions.configure<JavaPluginExtension> {
        withSourcesJar()
    }

    extensions.configure<PublishingExtension> {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])

                pom {
                    name = "cosmetics-api"
                    description = "LandMC cosmetic wire format"
                    url = "https://github.com/landmc-network/landmc-cosmetics"
                }
            }
        }

        // Only registered when credentials are present, so publishToMavenLocal works on a
        // developer machine with no GitHub configuration at all.
        if (githubUser != null && githubToken != null) {
            repositories {
                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/landmc-network/landmc-cosmetics")
                    credentials {
                        username = githubUser
                        password = githubToken
                    }
                }
            }
        }
    }
}
