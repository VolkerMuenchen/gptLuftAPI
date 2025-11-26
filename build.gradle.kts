plugins {
    application
    java
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    implementation("com.fasterxml.jackson.core:jackson-core:2.17.0")
}

application {
    mainClass.set("net.envinet.pm25.Main")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.register<JavaExec>("sensorFinder") {
    group = "application"
    description = "Findet die 5 nächstgelegenen Sensoren zu einer Sensor-ID"

    mainClass.set("net.envinet.pm25.SensorFinder")
    classpath = sourceSets["main"].runtimeClasspath

    // Default-ID, falls keine Property gesetzt ist
    val id = project.findProperty("sensorId")?.toString() ?: "81607"
    args(id)
}
