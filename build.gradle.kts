plugins {
    id("com.gradleup.shadow") version "9.4.1" apply false
}

allprojects {
    group = "me.coha"
    version = "1.0"

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

subprojects {
    apply(plugin = "java")

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    dependencies {
        "compileOnly"("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
        
        "compileOnly"("org.projectlombok:lombok:1.18.34")
        "annotationProcessor"("org.projectlombok:lombok:1.18.34")
    }
}