plugins {
    `maven-publish`
}

base {
    archivesName.set("displayholograms-api")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
