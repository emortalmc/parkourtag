plugins {
    java
    id("com.gradleup.shadow") version "9.2.2"
}

group = "dev.emortal.minestom.parkourtag"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()

    maven("https://repo.emortal.dev/snapshots")
    maven("https://repo.emortal.dev/releases")

    maven("https://jitpack.io")
    maven("https://packages.confluent.io/maven/")
}

dependencies {
    implementation("dev.emortal.minestom:game-sdk:3847948")

    implementation("dev.hollowcube:polar:1.15.0")
    compileOnly("it.unimi.dsi:fastutil:8.5.18")
    implementation("net.kyori:adventure-text-minimessage:4.25.0")

    // jolt-jni
    implementation("com.github.stephengold:jolt-jni-Windows64:3.4.0")
    runtimeOnly("com.github.stephengold:jolt-jni-Linux64:3.4.0:ReleaseSp")
    runtimeOnly("com.github.stephengold:jolt-jni-Windows64:3.4.0:ReleaseSp")
    implementation("io.github.electrostat-lab:snaploader:1.1.1-stable")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks {
    shadowJar {
        mergeServiceFiles()

        manifest {
            attributes(
                "Main-Class" to "dev.emortal.minestom.parkourtag.Main",
                "Multi-Release" to true
            )
        }
    }

    withType<AbstractArchiveTask> {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }

    build {
        dependsOn(shadowJar)
    }
}
