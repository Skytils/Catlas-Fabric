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

package gg.skytils.skytilsmod.features.impl.dungeons.catlas.utils

import gg.skytils.skytilsmod.Skytils.json
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod.features.impl.dungeons.catlas.core.map.Room
import gg.skytils.skytilsmod.features.impl.dungeons.catlas.core.map.RoomData
import gg.skytils.skytilsmod.features.impl.dungeons.catlas.handlers.DungeonInfo
import gg.skytils.skytilsmod.features.impl.dungeons.catlas.handlers.DungeonScanner
import gg.skytils.skytilsmod.utils.Utils
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import kotlin.math.roundToInt

object ScanUtils {
    @OptIn(ExperimentalSerializationApi::class)
    val roomList: Set<RoomData> by lazy {
        mc.resourceManager.getResourceOrThrow(
            Identifier.of("catlas:rooms.json")
        ).inputStream.use(json::decodeFromStream)
    }

    @OptIn(ExperimentalSerializationApi::class)
    val legacyItems: Map<Identifier, Pair<Int, Int>> by lazy {
        val legacyObj: JsonObject = mc.resourceManager.getResourceOrThrow(
            Identifier.of("catlas:legacy.json")
        ).inputStream.use(json::decodeFromStream)

        val blocksObj = legacyObj["blocks"]!!.jsonObject
        return@lazy blocksObj.entries.associate {
            Identifier.of(it.value.jsonPrimitive.content.substringBefore("[")) to it.key.split(":", limit = 2).let { it.first().toInt() to it.last().toInt() }
        }
    }

    fun getRoomData(x: Int, z: Int): RoomData? {
        return getRoomData(getCore(x, z))
    }

    fun getRoomData(hash: Int): RoomData? {
        return roomList.find { hash in it.cores }
    }

    fun getRoomCenter(posX: Int, posZ: Int): Pair<Int, Int> {
        val roomX = ((posX - DungeonScanner.startX) / 32f).roundToInt()
        val roomZ = ((posZ - DungeonScanner.startZ) / 32f).roundToInt()
        return (roomX * 32 + DungeonScanner.startX) to (roomZ * 32 + DungeonScanner.startZ)
    }

    fun getRoomFromPos(pos: BlockPos): Room? {
        val x = ((pos.x - DungeonScanner.startX + 15) shr 5)
        val z = ((pos.z - DungeonScanner.startZ + 15) shr 5)
        val room = DungeonInfo.dungeonList.getOrNull(x * 2 + z * 22)
        return room as? Room
    }

    fun getCore(x: Int, z: Int): Int {
        val sb = StringBuilder(150)
        val chunk = mc.world!!.getChunk(x shr 4, z shr 4)
        var bedrock = 0
        for (y in 140 downTo 12) {
            val blockState = chunk.getBlockState(BlockPos(x, y, z))
            val id = if (blockState.isAir) 0 else {
                val identifier = Registries.BLOCK.getId(blockState.block)
                println(identifier)
                legacyItems[identifier]?.first ?: continue
            }
            if (id != 0) println("Mapped ${blockState} to ${id}")
            if (id == 0 && bedrock >= 2 && y < 69) {
                sb.append(CharArray(y - 11) { '0' })
                break
            }

            if (id == 7) {
                bedrock++
            } else {
                bedrock = 0
                if (Utils.equalsOneOf(id, 5, 54, 146)) continue
            }

            sb.append(id)
        }
        return sb.toString().hashCode()
    }
}
