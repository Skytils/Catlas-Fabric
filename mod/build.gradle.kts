/*
 * Skytils - Hypixel Skyblock Quality of Life Mod
 * Copyright (C) 2020-2024 Skytils
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.task.RemapJarTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.security.MessageDigest

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("gg.essential.defaults")
    id("gg.essential.multi-version")
    signing
    `maven-publish`
}

version = "0.1.1+${platform}"
group = "gg.skytils"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.essential.gg/repository/maven-public/")
    maven("https://repo.essential.gg/repository/maven-releases/")
    maven("https://repo.hypixel.net/repository/Hypixel/")
    maven("https://jitpack.io") {
        mavenContent {
            includeGroupAndSubgroups("com.github")
        }
    }
    maven("https://maven.dediamondpro.dev/releases") {
        mavenContent {
            includeGroup("dev.dediamondpro")
        }
    }
}

loom {
    silentMojangMappingsLicense()
    runConfigs {
        getByName("client") {
            isIdeConfigGenerated = true
            property("elementa.dev", "true")
            property("elementa.debug", "true")
            property("elementa.invalid_usage", "warn")
            property("asmhelper.verbose", "true")
            property("mixin.debug.verbose", "true")
            property("mixin.debug.export", "true")
            property("mixin.dumpTargetOnFailure", "true")
        }
        remove(getByName("server"))
    }
    mixin {
        defaultRefmapName = "mixins.skytils.refmap.json"
    }
    mods {
        create("catlas") {
            sourceSet(sourceSets["main"])
        }
    }
}

val shadowMe: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

val shadowMeMod: Configuration by configurations.creating {
    configurations.modImplementation.get().extendsFrom(this)
}

dependencies {
    modImplementation("net.fabricmc:fabric-language-kotlin:1.12.3+kotlin.2.0.21")

    include(modRuntimeOnly("gg.essential:loader-fabric:1.2.3")!!)
    modImplementation("net.fabricmc.fabric-api:fabric-api") {
        version {
            require(when {
                platform.mcVersion == 12105 -> "0.121.0+1.21.5"
                else -> "0.119.2+1.21.4"
            })
        }
    }

    modCompileOnly("gg.essential:essential-${if (platform.mcVersion >= 12100) "1.20.6-fabric" else platform.toString()}:17141+gd6f4cfd3a8") {
        exclude(module = "asm")
        exclude(module = "asm-commons")
        exclude(module = "asm-tree")
        exclude(module = "gson")
        exclude(module = "kotlinx-coroutines-core-jvm")
        exclude(module = "universalcraft-1.20.6-fabric")
    }
    modCompileOnly("gg.essential:universalcraft-${platform}:396")

    include(implementation("gg.essential:elementa-unstable-layoutdsl:676") {
        excludeKotlin()
        exclude(module = "elementa-1.8.9-forge")
    })

    include(implementation("dev.dediamondpro:minemark-elementa:1.2.3"){
        exclude(module = "elementa-1.8.9-forge")
        excludeKotlin()
    })

    shadowMe(platform(kotlin("bom")))
    shadowMe(platform(ktor("bom", "2.3.13", addSuffix = false)))

    shadowMe(ktor("serialization-kotlinx-json")) { excludeKotlin() }

    shadowMe(ktorClient("core")) { excludeKotlin() }
    shadowMe(ktorClient("cio")) { excludeKotlin() }
    shadowMe(ktorClient("content-negotiation")) { excludeKotlin() }
    shadowMe(ktorClient("encoding")) { excludeKotlin() }

    shadowMe(ktorServer("core")) { excludeKotlin() }
    shadowMe(ktorServer("cio")) { excludeKotlin() }
    shadowMe(ktorServer("content-negotiation")) { excludeKotlin() }
    shadowMe(ktorServer("compression")) { excludeKotlin() }
    shadowMe(ktorServer("cors")) { excludeKotlin() }
    shadowMe(ktorServer("conditional-headers")) { excludeKotlin() }
    shadowMe(ktorServer("auto-head-response")) { excludeKotlin() }
    shadowMe(ktorServer("default-headers")) { excludeKotlin() }
    shadowMe(ktorServer("host-common")) { excludeKotlin() }
    shadowMe(ktorServer("auth")) { excludeKotlin() }

    include(implementation("org.brotli:dec:0.1.2")!!)

    shadowMe(project(":events")) {
        isTransitive = false
    }
    shadowMe(project(":vigilance")) {
        excludeKotlin()
    }
    shadowMe("gg.skytils.hypixel.types:types") {
        excludeKotlin()
    }
    shadowMe("gg.skytils.skytilsws.shared:ws-shared") {
        excludeKotlin()
    }

    include(implementation("net.hypixel:mod-api:1.0.1")!!)

    include(implementation(annotationProcessor("io.github.llamalad7:mixinextras-fabric:0.5.0-rc.1")!!)!!)
    annotationProcessor("net.fabricmc:sponge-mixin:0.15.5+mixin.0.8.7")
    // compileOnly("org.spongepowered:mixin:0.8.5")
}

sourceSets {
    main {
        output.setResourcesDir(kotlin.classesDirectory)
    }
}

val enabledVersions = setOf(
    "1.21.4-fabric",
    "1.21.5-fabric",
)

tasks {
    build {
        if (platform.mcVersionStr !in enabledVersions) {
            enabled = false
        }
    }
    processResources {
        dependsOn(compileJava)
        setOf("mcmod.info", "fabric.mod.json").forEach {
            filesMatching(it) {
                expand(
                    mapOf(
                        "version" to version,
                        "mcversion" to platform.mcVersionStr,
                        "javaversion" to platform.javaVersion.asInt(),
                    )
                )
            }
        }
    }
    named<Jar>("jar") {
        manifest {
            attributes(
                mapOf(
                    "MixinConfigs" to "mixins.skytils.json,mixins.skytils-events.json,mixins.skytils-init.json"
                )
            )
        }
        dependsOn(shadowJar)
        enabled = false
    }
    named<RemapJarTask>("remapJar") {
        archiveBaseName.set("Catlas")
        inputFile.set(shadowJar.get().archiveFile)
        doLast {
            MessageDigest.getInstance("SHA-256").digest(archiveFile.get().asFile.readBytes())
                .let {
                    println("SHA-256: " + it.joinToString(separator = "") { "%02x".format(it) }.uppercase())
                }
        }
    }
    named<ShadowJar>("shadowJar") {
        archiveBaseName.set("Catlas")
        archiveClassifier.set("dev")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        configurations = listOf(shadowMe, shadowMeMod)

        relocate("io.ktor", "gg.skytils.ktor")
        relocate("gg.essential.vigilance", "gg.skytils.vigilance")
        relocate("net.hypixel.modapi.tweaker", "gg.skytils.hypixel-net.modapi.tweaker")

        exclude(
            "**/LICENSE_MixinExtras",
            "**/LICENSE.md",
            "**/LICENSE.txt",
            "**/LICENSE",
            "**/NOTICE",
            "**/NOTICE.txt",
            "dummyThing"
        )
        mergeServiceFiles()
    }
    withType<AbstractArchiveTask> {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = platform.javaVersion.toString()
            freeCompilerArgs =
                listOf(
                    /*"-opt-in=kotlin.RequiresOptIn", */
                    "-Xjvm-default=all",
                    //"-Xjdk-release=1.8",
                    "-Xbackend-threads=0",
                    /*"-Xuse-k2"*/
                )
            languageVersion = "2.0"
            apiVersion = "2.0"
        }
        kotlinDaemonJvmArguments.set(
            listOf(
                "-Xmx2G",
                //"-Xbackend-threads=0"
            )
        )
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(platform.javaVersion.asInt()))
    }
}

signing {
    if (project.hasProperty("signing.gnupg.keyName")) {
        useGpgCmd()
        sign(tasks["remapJar"])
    }
}

/**
 * Builds the dependency notation for the named Ktor [module] at the given [version].
 *
 * @param module simple name of the Ktor module, for example "client-core".
 * @param version optional desired version, unspecified if null.
 */
fun DependencyHandler.ktor(module: String, version: String? = null, addSuffix: Boolean = true) =
    "io.ktor:ktor-$module${if (addSuffix) "-jvm" else ""}${version?.let { ":$version" } ?: ""}"

fun DependencyHandler.ktorClient(module: String, version: String? = null) = ktor("client-${module}", version)

fun DependencyHandler.ktorServer(module: String, version: String? = null) = ktor("server-${module}", version)

fun JavaVersion.asInt() = this.ordinal + 1

fun <T : ModuleDependency> T.excludeKotlin(): T {
    exclude(group = "org.jetbrains.kotlin")
    exclude(module = "kotlinx-coroutines-core")
    exclude(module = "kotlinx-serialization-core")
    exclude(module = "kotlinx-serialization-json")
    exclude(module = "kotlinx-serialization-cbor")
    return this
}