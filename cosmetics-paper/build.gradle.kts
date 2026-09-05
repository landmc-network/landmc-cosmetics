plugins {
    alias(libs.plugins.shadow)
}

dependencies {
    compileOnly(libs.paper.api)
    // paper-plugin.yml is hand-written; Paper has no annotation processor for it.
    


    implementation(project(":cosmetics-api"))
    implementation(libs.platform.api)
    implementation(libs.platform.common)
    implementation(libs.platform.config)
    implementation(libs.platform.messaging)
    implementation(libs.platform.paper)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.paper.api)
    testRuntimeOnly(libs.slf4j.simple)
    testRuntimeOnly(libs.junit.platform.launcher)
}

configurations.runtimeClasspath {
    // Paper provides these. Jedis drags in slf4j-api 1.7.x, which inside the plugin jar
    // would shadow the server's 2.x and break logging.
    exclude(group = "com.google.code.gson")
    exclude(group = "org.slf4j", module = "slf4j-api")
    exclude(group = "net.kyori")
}

tasks.shadowJar {
    archiveFileName = "landmc-cosmetics-paper.jar"

    val shaded = "pl.landmc.cosmetics.paper.libs"
    listOf(
        "eu.okaeri",
        "dev.rollczi.litecommands",
        "com.eternalcode.multification",
        "redis.clients",
        "org.json",
        "org.apache.commons.pool2",
        "org.yaml.snakeyaml",
    ).forEach { relocate(it, "$shaded.$it") }

    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/versions/**/module-info.class")
    exclude("org/jetbrains/annotations/**", "org/intellij/lang/**")

    mergeServiceFiles()
}

tasks.processResources {
    // paper-plugin.yml is hand-written but its version comes from the build, so the descriptor
    // and the jar cannot disagree.
    val properties = mapOf("version" to project.version)
    inputs.properties(properties)
    filesMatching("paper-plugin.yml") {
        expand(properties)
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
