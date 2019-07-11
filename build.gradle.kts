import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.41"
    application
}

group = "de.randomerror"
version = "1.0-SNAPSHOT"

application {
    mainClassName = "de.randomerror.chatnoaudio.AppKt"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.seleniumhq.selenium:selenium-java:3.141.59")
//    implementation("org.seleniumhq.selenium:selenium-htmlunit-driver:2.52.0")
    implementation("com.google.zxing:javase:3.3.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
