import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("org.jetbrains.intellij.platform") version "2.2.0"
}

group = "xyz.capacium"
version = "0.1.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // Target: IntelliJ IDEA Community 2024.1+ (also covers PyCharm, GoLand, etc.)
        intellijIdeaCommunity("2024.1.4")
        bundledPlugin("com.intellij.java")
        // YAML support for capability.yaml validation
        bundledPlugin("org.jetbrains.plugins.yaml")
        instrumentationTools()
    }
}

intellijPlatform {
    pluginConfiguration {
        name = "Capacium"
        version = "0.1.0"
        ideaVersion {
            sinceBuild = "241"
            untilBuild = "251.*"
        }
    }
    signing {
        // Signing configured via CI environment variables
        certificateChainFile = file(System.getenv("PLUGIN_CERT_CHAIN_FILE") ?: "certs/chain.crt")
        privateKeyFile = file(System.getenv("PLUGIN_PRIVATE_KEY_FILE") ?: "certs/private.pem")
        password = System.getenv("PLUGIN_KEY_PASSWORD") ?: ""
    }
    publishing {
        token = System.getenv("JETBRAINS_MARKETPLACE_TOKEN") ?: ""
    }
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
}
