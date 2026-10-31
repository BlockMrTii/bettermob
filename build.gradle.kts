plugins {
    `java-library`
    `maven-publish`
    id("com.gradleup.shadow") version "9.2.2"
}

group = "eu.northsoft"
version = "1.2.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.extendedclip.com/releases/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.17")
    compileOnly("me.clip:placeholderapi:2.12.3")
    implementation("org.bstats:bstats-bukkit:3.2.1")

    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    testImplementation("com.sk89q.worldguard:worldguard-bukkit:7.0.17")
    testImplementation("me.clip:placeholderapi:2.12.3")
    testImplementation("org.mockito:mockito-core:5.20.0")
    testImplementation("com.networknt:json-schema-validator:3.0.8")
    testImplementation("tools.jackson.dataformat:jackson-dataformat-yaml:3.2.3")
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.test {
    useJUnitPlatform()
    // optional reflection checks against the real model plugins:
    // ./gradlew test -Dmodelengine.jar=<jar> -Dbettermodel.jars=<api jar>:<bukkit-api jar>
    listOf("modelengine.jar", "bettermodel.jars").forEach { key ->
        System.getProperty(key)?.let { systemProperty(key, it) }
    }
}

tasks.register("printVersion") {
    val v = project.version.toString()
    doLast { println(v) }
}

tasks.processResources {
    filteringCharset = "UTF-8"
}

tasks.shadowJar {
    archiveClassifier.set("")
    dependencies {
        include(dependency("org.bstats:.*"))
    }
    relocate("org.bstats", "eu.northsoft.bettermob.libs.bstats")
}

tasks.jar {
    archiveClassifier.set("plain")
    // keep build/libs clean: only the shaded plugin jar lands there
    destinationDirectory.set(layout.buildDirectory.dir("plain"))
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "bettermob"
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/HyperGaming99/bettermob")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
