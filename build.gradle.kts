plugins {
    id("com.gradleup.shadow") version "9.2.2" apply false
}

allprojects {
    group = "eu.northsoft"
    version = "1.2.0"
}

subprojects {
    apply(plugin = "java-library")

    repositories {
        mavenCentral()
        maven("https://repo.extendedclip.com/releases/")
        maven("https://maven.enginehub.org/repo/")
        maven("https://repo.papermc.io/repository/maven-public/")
    }

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    tasks.withType<ProcessResources>().configureEach {
        filteringCharset = "UTF-8"
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

tasks.register("printVersion") {
    val v = project.version.toString()
    doLast { println(v) }
}
