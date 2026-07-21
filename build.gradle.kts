plugins {
    id("org.springframework.boot") version "4.1.0" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
    group = "com.bankplatform"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}
