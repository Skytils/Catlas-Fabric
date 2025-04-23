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
package gg.skytils.skytilsmod.features.impl.handlers

import com.aayushatharva.brotli4j.encoder.BrotliOutputStream
import com.aayushatharva.brotli4j.encoder.Encoder
import gg.essential.elementa.utils.withAlpha
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.TickEvent
import gg.skytils.event.impl.play.WorldUnloadEvent
import gg.skytils.event.impl.render.WorldDrawEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod._event.LocationChangeEvent
import gg.skytils.skytilsmod.commands.impl.OrderedWaypointCommand
import gg.skytils.skytilsmod.core.PersistentSave
import gg.skytils.skytilsmod.core.tickTimer
import gg.skytils.skytilsmod.tweaker.DependencyLoader
import gg.skytils.skytilsmod.utils.*
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import net.minecraft.util.math.BlockPos
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.binary.Base64InputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.compressors.gzip.GzipParameters
import org.brotli.dec.BrotliInputStream
import java.awt.Color
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.Reader
import java.io.Writer
import java.nio.file.Path
import java.util.zip.Deflater
import kotlin.io.path.name
import kotlin.io.path.outputStream

object Waypoints : PersistentSave(File(Skytils.modDir, "waypoints.json")), EventSubscriber {
    val categories = HashSet<WaypointCategory>()

    private val sbeWaypointFormat =
        Regex("(?:\\.?\\/?crystalwaypoint parse )?(?<name>[a-zA-Z\\d]+)@-(?<x>[-\\d]+),(?<y>[-\\d]+),(?<z>[-\\d]+)\\\\?n?")
    private var visibleWaypoints = emptyList<Waypoint>()

    override fun setup() {
        register(::onWorldChange)
        register(::onPlayerMove)
        register(::onLocationChange)
        register(::onWorldRender)
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun getWaypointsFromString(str: String): Set<WaypointCategory> {
        val categories = hashSetOf<WaypointCategory>()
        if (str.startsWith("<Skytils-Waypoint-Data>(V")) {
            val version = str.substringBefore(')').substringAfter('V').toIntOrNull() ?: 0
            val content = str.substringAfter(':')

            val bombChecker = DecompressionBombChecker(100)
            val data = when (version) {
                1 -> {
                    bombChecker.wrapOutput(GzipCompressorInputStream(bombChecker.wrapInput(Base64InputStream(content.byteInputStream())))).use {
                        it.readBytes().decodeToString()
                    }
                }
                2 -> {
                    val wrapped = bombChecker.wrapInput(Base64InputStream(content.byteInputStream()))

                    val inputStream = BrotliInputStream(wrapped)

                    bombChecker.wrapOutput(inputStream).use {
                        it.readBytes().decodeToString()
                    }
                }

                else -> throw IllegalArgumentException("Unknown version $version")
            }

            categories.addAll(json.decodeFromString<CategoryList>(data).categories)

        } else if (Base64.isBase64(str)) {
            Base64InputStream(str.byteInputStream()).use<_, JsonElement>(json::decodeFromStream).let { element ->
                when (element) {
                    is JsonObject -> {
                        categories.addAll(
                            json.decodeFromJsonElement<CategoryList>(element).categories
                        )
                    }

                    is JsonArray -> {
                        val waypoints = json.decodeFromJsonElement<List<Waypoint>>(element)
                        waypoints.groupBy {
                            @Suppress("DEPRECATION")
                            it.island!!
                        }.mapTo(categories) { (island, waypoints) ->
                            WaypointCategory(
                                name = null,
                                waypoints = waypoints.toHashSet(),
                                isExpanded = true,
                                island = island
                            )
                        }
                    }

                    else -> throw IllegalArgumentException("Unknown JSON element type ${element::class}")
                }
            }
        } else if (sbeWaypointFormat.containsMatchIn(str)) {
            val island = SkyblockIsland.byMode[SBInfo.mode] ?: SkyblockIsland.CrystalHollows
            val waypoints = sbeWaypointFormat.findAll(str.trim().replace("\n", "")).map {
                Waypoint(
                    it.groups["name"]!!.value,
                    it.groups["x"]!!.value.toInt(),
                    it.groups["y"]!!.value.toInt(),
                    it.groups["z"]!!.value.toInt(),
                    true,
                    Color.RED,
                    System.currentTimeMillis(),
                    island
                )
            }.toSet()
            categories.add(
                WaypointCategory(
                    name = null,
                    waypoints = waypoints,
                    isExpanded = true,
                    island = island
                )
            )
        } else {
            try {
                val genericArray = json.decodeFromString<JsonArray>(str)
                val foundWaypoints = hashMapOf<SkyblockIsland, HashSet<Waypoint>>()

                for (element in genericArray) {
                    val obj = element.jsonObject

                    val options = obj["options"]?.jsonObject

                    val pos = obj["pos"].let {
                        if (it == null) BlockPos(obj["x"]!!.jsonPrimitive.int, obj["y"]!!.jsonPrimitive.int, obj["z"]!!.jsonPrimitive.int)
                        else if (it is JsonPrimitive) BlockPos.method_0_4851(it.long)
                        else BlockPos(it.jsonObject["x"]!!.jsonPrimitive.int, it.jsonObject["y"]!!.jsonPrimitive.int, it.jsonObject["z"]!!.jsonPrimitive.int)
                    }

                    var r = (obj["r"] ?: options?.get("r"))?.jsonPrimitive?.float
                    var g = (obj["g"] ?: options?.get("g"))?.jsonPrimitive?.float
                    var b = (obj["b"] ?: options?.get("b"))?.jsonPrimitive?.float

                    if ((r != null && g != null && b != null) && (r > 1 || g > 1 || b > 1)) {
                        r /= 255
                        g /= 255
                        b /= 255
                    }

                    val name = (obj["name"] ?: options?.get("name"))?.jsonPrimitive?.content ?: "Unnamed"
                    val color = obj["color"]?.jsonPrimitive?.content?.let(Utils::colorFromString) ?: Color(r ?: 1f, g ?: 0f, b ?: 0f)
                    val island = SkyblockIsland.byMode[((obj["island"] ?: obj["mode"])?.jsonPrimitive?.content)] ?: if (Utils.isOnHypixel) SkyblockIsland.current else SkyblockIsland.CrystalHollows

                    foundWaypoints.getOrPut(island) { hashSetOf() }.add(Waypoint(name, pos.x, pos.y, pos.z, true, color, System.currentTimeMillis()))
                }

                foundWaypoints.forEach { (island, waypoints) ->
                    categories.add(
                        WaypointCategory(
                            name = null,
                            waypoints = waypoints,
                            isExpanded = true,
                            island = island
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                throw IllegalArgumentException("Unknown waypoint format")
            }
        }

        return categories
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun getWaypointsFromFile(file: File): CategoryList {
        val version = file.nameWithoutExtension.substringAfterLast(".V").toIntOrNull() ?: 0

        val bombChecker = DecompressionBombChecker(100)
        val inputStream = when (version) {
            2 -> {
                val wrapped = bombChecker.wrapInput(file.inputStream())

                val inputStream = BrotliInputStream(wrapped)

                bombChecker.wrapOutput(inputStream)
            }

            else -> throw IllegalArgumentException("Unknown version $version")
        }

        return inputStream.use(json::decodeFromStream)
    }

    fun getStringFromWaypoints(categories: Set<WaypointCategory>, version: Int): String {
        val str = json.encodeToString(CategoryList(categories))
            .lines().joinToString("", transform = String::trim)

        val data = when (version) {
            2 -> {
                if (!DependencyLoader.hasNativeBrotli) error("Brotli encoder is not available")
                Base64.encodeBase64String(ByteArrayOutputStream().use { bs ->
                    BrotliOutputStream(bs, Encoder.Parameters().apply {
                        // setMode(Encoder.Mode.TEXT) for smaller values this actually makes the compressed data larger, larger values have no effect
                        setQuality(11)
                    }).use {
                        it.write(str.encodeToByteArray())
                    }
                    bs.toByteArray()
                })
            }

            1 -> {
                Base64.encodeBase64String(ByteArrayOutputStream().use { bs ->
                    GzipCompressorOutputStream(bs, GzipParameters().apply {
                        compressionLevel = Deflater.BEST_COMPRESSION
                    }).use { gs ->
                        gs.write(str.encodeToByteArray())
                    }
                    bs.toByteArray()
                })
            }

            else -> throw IllegalArgumentException("Unknown version $version")
        }


        return "<Skytils-Waypoint-Data>(V${version}):${data}"
    }

    fun writeWaypointsToFile(categoryList: CategoryList, path: Path, version: Int) {
        val realPath = if (!path.name.endsWith(".V$version.SkytilsWaypoints")) path.resolveSibling("${path.name}.V$version.SkytilsWaypoints") else path
        when (version) {
            2 -> {
                if (!DependencyLoader.hasNativeBrotli) error ("Brotli encoder is not available")
                BrotliOutputStream(realPath.outputStream(), Encoder.Parameters().apply {
                    setQuality(11)
                }).use {
                    it.write(json.encodeToString(categoryList).encodeToByteArray())
                }
            }
            else -> throw IllegalArgumentException("Unknown version $version")
        }
    }

    fun computeVisibleWaypoints() {
        if (!Utils.inSkyblock) {
            visibleWaypoints = emptyList()
            printDevMessage("Waypoints unloaded from compute, reason: SB check", "waypoints")
            return
        }
        val currIsland = SkyblockIsland.current
        visibleWaypoints = categories.filter {
            it.island == currIsland
        }.flatMap { category ->
            category.waypoints.filter { it.enabled }
        }
        printDevMessage({ "Waypoints computed for ${SBInfo.mode} (${currIsland}), num: ${visibleWaypoints.size}" }, "waypoints")
    }

    fun onWorldChange(event: WorldUnloadEvent) {
        printDevMessage("Waypoints unloaded from world unload", "waypoints")
        visibleWaypoints = emptyList()
    }

    fun onLocationChange(event: LocationChangeEvent) {
        tickTimer(20, task = ::computeVisibleWaypoints)
    }

    fun onPlayerMove(event: TickEvent) {
        if (mc.player?.hasMoved == true && SBInfo.mode != null && OrderedWaypointCommand.trackedIsland?.mode == SBInfo.mode) {
            val tracked = OrderedWaypointCommand.trackedSet?.firstOrNull()
            if (tracked == null) {
                OrderedWaypointCommand.doneTracking()
            } else if (tracked.pos.getSquaredDistance(mc.player.blockPos) < 2 * 2) {
                OrderedWaypointCommand.trackedSet?.removeFirstOrNull()
            }
        }
    }

    fun onWorldRender(event: WorldDrawEvent) {
        val matrixStack = UMatrixStack()
        if (OrderedWaypointCommand.trackedIsland?.mode == SBInfo.mode) {
            OrderedWaypointCommand.trackedSet?.firstOrNull()?.draw(event.partialTicks, matrixStack)
        }
        visibleWaypoints.forEach {
            it.draw(event.partialTicks, matrixStack)
        }
    }

    override fun read(reader: Reader) {
        val str = reader.readText()
        runCatching {
            categories.addAll(json.decodeFromString<CategoryList>(str).categories)
        }.onFailure { e ->
            println("Error loading waypoints from PersistentSave:")
            e.printStackTrace()
            // Error loading the new Waypoint format. Try loading the old format.
            val waypointsList = json.decodeFromString<HashSet<Waypoint>>(str)
            waypointsList.groupBy {
                @Suppress("DEPRECATION")
                it.island!!
            }.forEach { (island, waypoints) ->
                categories.add(
                    WaypointCategory(
                        name = null,
                        waypoints = waypoints.toHashSet(),
                        isExpanded = true,
                        island = island
                    )
                )
            }
            // If the old format is used, it should be instantly converted to prevent future errors
            markDirty<Waypoints>()
        }
    }

    override fun write(writer: Writer) {
        writer.write(json.encodeToString(CategoryList(categories)))
    }

    override fun setDefault(writer: Writer) {
        writer.write(json.encodeToString(CategoryList(emptySet())))
    }
}

/**
 * Represents the top-level structure of the JSON file. At the moment, this only contains a list of [WaypointCategory] objects.
 */
@Serializable
data class CategoryList(
    val categories: Set<WaypointCategory>
)

/**
 * Represents a collection of waypoints, including a name, island, and a list of [Waypoint] objects.
 */
@Serializable
data class WaypointCategory(
    var name: String?,
    val waypoints: Set<Waypoint>,
    var isExpanded: Boolean = true,
    @Serializable(with = SkyblockIsland.ModeSerializer::class)
    var island: SkyblockIsland
)
@Serializable
data class Waypoint @OptIn(ExperimentalSerializationApi::class) constructor(
    var name: String,
    // `pos` is separated into three different fields because we want "x", "y", and "z" fields
    // instead of a parent object "pos" containing three child fields. This helps keep compatibility with older versions.
    private var x: Int,
    private var y: Int,
    private var z: Int,
    @EncodeDefault
    var enabled: Boolean = true,
    @Serializable(with = IntColorSerializer::class)
    val color: Color = Color.RED,
    @EncodeDefault
    val addedAt: Long = System.currentTimeMillis(),
    @Deprecated("Should only exist in older data formats which do not have waypoint categories.")
    @Serializable(with = SkyblockIsland.ModeSerializer::class)
    var island: SkyblockIsland? = null
) {

    var pos
        get() = BlockPos(x, y, z)
        set(value) {
            x = value.x
            y = value.y
            z = value.z
        }

    fun draw(partialTicks: Float, matrixStack: UMatrixStack) {
        val (viewerX, viewerY, viewerZ) = RenderUtil.getViewerPos(partialTicks)
        RenderUtil.drawFilledBoundingBox(
            matrixStack,
            pos.toBoundingBox().expandBlock().offset(-viewerX, -viewerY, -viewerZ),
            color.withAlpha(color.alpha.coerceAtMost(128)),
            1f
        )
        UGraphics.disableDepth()
        RenderUtil.renderWaypointText(
            name,
            pos.x + 0.5,
            pos.y + 1.0,
            pos.z + 0.5,
            partialTicks,
            matrixStack
        )
        UGraphics.enableDepth()
    }
}
