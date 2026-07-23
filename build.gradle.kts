import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    id("org.springframework.boot") version "4.1.0" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    alias(libs.plugins.spotless) apply false
}

allprojects {
    group = "com.bankplatform"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "com.diffplug.spotless")

    configure<SpotlessExtension> {
        java {
            target("src/**/*.java")
            googleJavaFormat("1.22.0").aosp()
            importOrder()
            removeUnusedImports()
            trimTrailingWhitespace()
            endWithNewline()
        }
    }
}
