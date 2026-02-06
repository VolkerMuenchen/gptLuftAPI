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

    // ✅ JUnit 5
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")

    // ✅ Wichtig für Gradle 9.x:
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}

application {
    mainClass.set("net.envinet.pm25.Main")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

// ✅ Gradle soll JUnit 5 verwenden
tasks.test {
    useJUnitPlatform()
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
