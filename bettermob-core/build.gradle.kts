// Platform-neutral core. No Bukkit/Paper/Fabric dependencies allowed here.
dependencies {
    "api"(project(":bettermob-api"))
    "api"("org.yaml:snakeyaml:2.4")

    "testImplementation"(platform("org.junit:junit-bom:6.1.3"))
    "testImplementation"("org.junit.jupiter:junit-jupiter")
    "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
}
