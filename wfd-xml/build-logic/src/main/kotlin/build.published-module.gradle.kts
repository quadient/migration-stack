plugins {
    java
    `maven-publish`
}

publishing {
    publications {
        create<MavenPublication>("library") {
            from(components["java"])
        }
    }

    repositories {
        maven {
            url = file("build/repo").toURI()
        }
    }
}