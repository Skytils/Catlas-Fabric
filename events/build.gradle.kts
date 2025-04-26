/*
 * Skytils - Hypixel Skyblock Quality of Life Mod
 * Copyright (C) 2020-2023 Skytils
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
import net.fabricmc.loom.task.AbstractRemapJarTask
import net.fabricmc.loom.task.RemapJarTask
import org.apache.tools.ant.filters.FixCrLfFilter

plugins {
    kotlin("jvm")
    id("gg.essential.defaults")
    id("gg.essential.multi-version")
}

repositories {
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.6.4")
    // compileOnly(annotationProcessor("io.github.llamalad7:mixinextras-common:0.5.0-beta.1")!!)
    // annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
    // compileOnly("org.spongepowered:mixin:0.8.5")
}

group = "gg.skytils.events"

loom {
    mixin {
        useLegacyMixinAp.set(true)
        defaultRefmapName = "mixins.skytils-events.refmap.json"
    }
}

tasks.processResources {
    filesMatching("**/*.json") {
        filter(FixCrLfFilter::class, "eol" to FixCrLfFilter.CrLf.newInstance("lf"))
    }
}

tasks.withType<AbstractArchiveTask> {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.named<Jar>("jar") {
    //exclude("**/mixins.skytils-events.refmap.json")
}

//tasks.named<RemapJarTask>("remapJar") {
//    from(layout.buildDirectory.dir("classes/java/main")) {
//        include("**/mixins.skytils-events.refmap.json")
//        into("")
//    }
//}