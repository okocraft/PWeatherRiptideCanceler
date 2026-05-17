plugins {
    `java-library`
    alias(libs.plugins.paperweight.userdev)
    alias(libs.plugins.run.paper)
    alias(libs.plugins.jcommon)
}

group = "net.okocraft.pweatherriptidecanceler"
version = "2.0-SNAPSHOT"
val mcVersion = libs.versions.paper.get().replaceAfter(".build", "").removeSuffix(".build")
val fullVersion = "${version}-mc${mcVersion}"

dependencies {
    paperweight.paperDevBundle(libs.versions.paper.get())
}

jcommon {
    javaVersion = JavaVersion.VERSION_25
}

tasks {
    processResources {
        filesMatching(listOf("plugin.yml")) {
            expand("projectVersion" to fullVersion, "minecraftVersion" to mcVersion)
        }
    }

    jar {
        archiveFileName = "PWeatherRiptideCanceler-${fullVersion}.jar"
    }

    runServer {
        minecraftVersion(mcVersion)
        systemProperty("com.mojang.eula.agree", "true")
        systemProperty("paper.disablePluginRemapping", "true")
    }
}
