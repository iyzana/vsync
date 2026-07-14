plugins {
    kotlin("jvm") version "2.4.10"

    application
    id("com.gradleup.shadow") version "9.5.1"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
}

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("io.javalin:javalin:7.2.2")
    implementation("com.google.code.gson:gson:2.14.0")

    implementation("ch.qos.logback:logback-classic:1.5.38")
    implementation("io.github.oshai:kotlin-logging-jvm:8.0.4")

    implementation("com.mohamedrejeb.ksoup:ksoup-html:0.6.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

application {
    mainClass.set("de.randomerror.ytsync.AppKt")
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom("$projectDir/detekt.yml")
}
