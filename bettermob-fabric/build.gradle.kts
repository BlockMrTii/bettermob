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
    include("org.yaml:snakeyaml:2.4")

    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.processResources {
    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to project.version))
    }
}
