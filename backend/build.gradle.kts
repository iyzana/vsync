plugins {
    id("org.jetbrains.kotlin.jvm") version "1.3.70"

    application
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

repositories {
    jcenter()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("com.sparkjava:spark-kotlin:1.0.0-alpha")

    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("io.github.microutils:kotlin-logging:1.7.9")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

application {
    mainClassName = "de.randomerror.ytsync.AppKt"
}
