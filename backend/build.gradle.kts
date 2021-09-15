plugins {
    id("org.jetbrains.kotlin.jvm") version "1.5.30"

    application
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

repositories {
    jcenter()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("com.sparkjava:spark-kotlin:1.0.0-alpha")
    implementation("com.google.code.gson:gson:2.8.8")

    implementation("ch.qos.logback:logback-classic:1.2.6")
    implementation("io.github.microutils:kotlin-logging:2.0.11")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

application {
    mainClassName = "de.randomerror.ytsync.AppKt"
}
