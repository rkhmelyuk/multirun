fun properties(key: String) = project.findProperty(key).toString()

plugins {
    id("org.jetbrains.intellij") version "1.16.1"
}

group = properties("pluginGroup")
version = properties("pluginVersion")

repositories {
    mavenLocal()
    mavenCentral()
    maven(
        url = "https://oss.sonatype.org/content/repositories/snapshots/"
    )
    maven(
        url = "https://packages.jetbrains.team/maven/p/intellij-plugin-verifier/intellij-plugin-structure"
    )
    maven(
        url = "https://www.jetbrains.com/intellij-repository/releases"
    )
}


intellij {
    pluginName.set(properties("pluginName"))
    version.set(properties("platformVersion"))
    type.set(properties("platformType"))
    downloadSources.set(properties("platformDownloadSources").toBoolean())
    updateSinceUntilBuild.set(true)
}


tasks {
    // Set the JVM compatibility versions
    properties("javaVersion").let {
        withType<JavaCompile> {
            sourceCompatibility = it
            targetCompatibility = it
        }
    }

    patchPluginXml {
        version.set(properties("pluginVersion"))
        sinceBuild.set(properties("pluginSinceBuild"))
        untilBuild.set(properties("pluginUntilBuild"))

    }


    wrapper {
        gradleVersion = properties("gradleVersion")
    }

    runIde {
        jvmArgs(
            "-Xmx6G"
        )
    }


    publishPlugin {
        //dependsOn("patchChangelog")
        token.set(System.getenv("PUBLISH_TOKEN"))

        // pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel

        channels.set(listOf(properties("pluginVersion")))
    }

    buildSearchableOptions {
        // Allows building the plugin while a sandbox is running
        enabled = false
    }

}
