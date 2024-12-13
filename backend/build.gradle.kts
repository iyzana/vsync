plugins {
    kotlin("jvm") version "2.1.0"

    application
    id("com.gradleup.shadow") version "8.3.5"
    id("io.gitlab.arturbosch.detekt") version "1.23.7"
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

    implementation("io.javalin:javalin:6.3.0")
    implementation("com.google.code.gson:gson:2.11.0")

    implementation("ch.qos.logback:logback-classic:1.5.12")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")

    implementation("com.mohamedrejeb.ksoup:ksoup-html:0.4.1")

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
