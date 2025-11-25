plugins {
    java
    application
}

group = "net.envinet"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.0")
    implementation("org.openjfx:javafx-controls:21.0.1")
    implementation("org.openjfx:javafx-web:21.0.1")
    implementation("org.apache.xmlgraphics:batik-transcoder:1.17")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
}

application {
    mainClass.set("net.envinet.pm25.Main")
}

tasks.test {
    useJUnitPlatform()
}
