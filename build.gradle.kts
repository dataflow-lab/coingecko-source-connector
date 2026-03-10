plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.avro)
}

group = "com.stitch80"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
}

dependencies {
    compileOnly(libs.kafka.connect.api)
    compileOnly(libs.kafka.clients)

    implementation(libs.okhttp)
    implementation(libs.avro)
    implementation(libs.confluent.avro.serializer)
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.databind)
    implementation(libs.logback)
    implementation(libs.kotlin.logging)

    testImplementation(libs.kafka.connect.api)
    testImplementation(libs.kafka.clients)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.okhttp.mockwebserver)
    testRuntimeOnly(libs.junit.platform.launcher)
}

kotlin {
    jvmToolchain(17)
}

avro {
    setCreateSetters(false)
    setFieldVisibility("PRIVATE")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.register<Jar>("connectJar") {
    group = "build"
    description = "Builds a fat JAR containing the connector and all runtime dependencies for Kafka Connect deployment"
    archiveClassifier.set("connect")
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith("jar") }
            .map { zipTree(it) }
    })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.register<Sync>("connectPlugin") {
    group = "build"
    description = "Assembles the Kafka Connect plugin directory with fat JAR, manifest, and assets"
    dependsOn("connectJar")
    from(tasks.named("connectJar"))
    from("src/main/resources/manifest.json")
    from("src/main/resources/assets") { into("assets") }
    into(layout.buildDirectory.dir("connect-plugin/stitch80-kafka-connect-coingecko"))
}
