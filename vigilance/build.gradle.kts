import gg.essential.gradle.multiversion.StripReferencesTransform.Companion.registerStripReferencesAttribute

plugins {
    id("java")
    kotlin("jvm")
    id("gg.essential.defaults")
    id("gg.essential.defaults.maven-publish")
}

group = "gg.skytils"
version = "0.0.1"

repositories {
    mavenCentral()
}

val common = registerStripReferencesAttribute("common") {
    excludes.add("net.minecraft")
}

dependencies {
    compileOnly("gg.essential:universalcraft-1.8.9-forge:369") {
        attributes { attribute(common, true) }
    }
    api("gg.essential:vigilance:306")
    api("gg.essential:elementa-unstable-layoutdsl:676")
}

java.toolchain {
    languageVersion = JavaLanguageVersion.of(8)
}