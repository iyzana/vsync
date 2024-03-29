plugins {
    kotlin("jvm") version "1.9.23"

    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("io.gitlab.arturbosch.detekt") version "1.23.6"
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

    implementation("com.sparkjava:spark-kotlin:1.0.0-alpha")
    implementation("com.google.code.gson:gson:2.10.1")

    implementation("ch.qos.logback:logback-classic:1.4.5")
    implementation("io.github.oshai:kotlin-logging-jvm:6.0.3")

    implementation("com.mohamedrejeb.ksoup:ksoup-html:0.3.1")

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
