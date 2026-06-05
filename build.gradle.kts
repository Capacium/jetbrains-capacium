plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.4"
    id("org.jetbrains.changelog") version "2.2.1"
}

group = "xyz.capacium"
version = "0.1.0"

repositories {
    mavenCentral()
}

intellij {
    version.set("2024.1")
    type.set("IC")
    plugins.set(listOf())
}

changelog {
    version.set("0.1.0")
    path.set("${project.projectDir}/CHANGELOG.md")
    header.set(provider { "[${version.get()}]" })
    itemPrefix.set("-")
    keepUnreleasedSection.set(true)
}

tasks {
    patchPluginXml {
        sinceBuild.set("241")
        untilBuild.set("242.*")

        pluginDescription.set(providers.fileContents(
            layout.projectDirectory.file("src/main/resources/META-INF/plugin-description.html")
        ).asText.map { it.lines().joinToString("\n") })

        changeNotes.set(providers.fileContents(
            layout.projectDirectory.file("src/main/resources/META-INF/plugin-changelog.html")
        ).asText.map { it.lines().joinToString("\n") })
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
