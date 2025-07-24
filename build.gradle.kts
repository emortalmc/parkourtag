plugins {
    java
    id("com.gradleup.shadow") version "9.0.0-rc1"
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
    implementation("dev.emortal.minestom:game-sdk:c30a7db")

    implementation("dev.hollowcube:polar:1.14.6")
    implementation("net.kyori:adventure-text-minimessage:4.23.0")

    // jolt-jni
    implementation("com.github.stephengold:jolt-jni-Windows64:2.0.1")
    runtimeOnly("com.github.stephengold:jolt-jni-Linux64:2.0.1:ReleaseSp")
    runtimeOnly("com.github.stephengold:jolt-jni-Windows64:2.0.1:ReleaseSp")
    implementation("io.github.electrostat-lab:snaploader:1.1.1-stable")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
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
