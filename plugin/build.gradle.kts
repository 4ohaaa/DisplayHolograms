plugins {
    id("com.gradleup.shadow")
}

base {
    archivesName.set("displayholograms-plugin")
}

dependencies {
    implementation(project(":api"))
}

tasks {
    shadowJar {
        archiveClassifier.set("")
    }
    
    build {
        dependsOn(shadowJar)
    }

    processResources {
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
