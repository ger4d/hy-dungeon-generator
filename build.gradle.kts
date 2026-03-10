plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.3.0"
}

group = property("pluginGroup") as String
version = property("pluginVersion") as String

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.hytale.com/release")
    }
    maven {
        url = uri("https://maven.hytale.com/pre-release")
    }
}

dependencies {
    // Hytale Server API — compileOnly, never bundle
    compileOnly("com.hypixel.hytale:Server:2026.03.05-9fdc5985d")
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release = 25
    }

    shadowJar {
        archiveBaseName.set("DungeonGen")
        archiveClassifier.set("")
        mergeServiceFiles()
    }

    build {
        dependsOn(shadowJar)
    }
}

// Deploy assets to server
tasks.register<Copy>("deployAssets") {
    from("assets")
    into("${rootProject.projectDir}/../server/Server/mods/DungeonGenAssets/")
    doLast {
        println("Synced Assets to server/Server/mods/DungeonGenAssets/")
    }
}

// Deploy JAR + assets to server
tasks.register<Copy>("deploy") {
    dependsOn(tasks.shadowJar)
    dependsOn("deployAssets")
    from(tasks.shadowJar.get().archiveFile)
    into("${rootProject.projectDir}/../server/Server/mods")

    doLast {
        println("Deployed ${tasks.shadowJar.get().archiveFile.get().asFile.name} to server/Server/mods/")
    }
}
