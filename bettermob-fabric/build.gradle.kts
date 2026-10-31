plugins {
    id("fabric-loom")
}

base {
    archivesName.set("bettermob-fabric")
}

dependencies {
    "minecraft"("com.mojang:minecraft:1.21.11")
    "mappings"("net.fabricmc:yarn:1.21.11+build.6:v2")
    "modImplementation"("net.fabricmc:fabric-loader:0.20.1")
    "modImplementation"("net.fabricmc.fabric-api:fabric-api:0.141.6+1.21.11")

    implementation(project(":bettermob-core"))
    include(project(":bettermob-core"))
    include(project(":bettermob-api"))
}

tasks.processResources {
    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to project.version))
    }
}
