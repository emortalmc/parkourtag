plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "dev.emortal.minestom.parkourtag"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()

    maven("https://repo.emortal.dev/snapshots")
    maven("https://repo.emortal.dev/releases")

    maven("https://jitpack.io")
    maven("https://packages.confluent.io/maven/")
}

dependencies {
    implementation("com.github.EmortalMC:TNT:4ef1b53482")

    implementation("dev.emortal.minestom:core:c302f8f")
    implementation("net.kyori:adventure-text-minimessage:4.12.0")

    implementation("dev.emortal.minestom:game-sdk:818e2c2")
    implementation("dev.emortal.api:kurushimi-sdk:848d117") {
        exclude(group = "dev.emortal.api", module = "common-proto-sdk")
    }
}

tasks {
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        mergeServiceFiles()

        manifest {
            attributes(
                "Main-Class" to "dev.emortal.minestom.parkourtag.Entrypoint",
                "Multi-Release" to true
            )
        }
    }

    withType<AbstractArchiveTask> {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }

    build { dependsOn(shadowJar) }
}