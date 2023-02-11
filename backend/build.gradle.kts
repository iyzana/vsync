plugins {
    id("org.jetbrains.kotlin.jvm") version "1.8.10"

    application
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("io.gitlab.arturbosch.detekt").version("1.22.0")
}

kotlin {
    jvmToolchain(17)
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
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

application {
    mainClass.set("de.randomerror.ytsync.AppKt")
}

detekt {
    buildUponDefaultConfig = true
    config = files("$projectDir/detekt.yml")
}