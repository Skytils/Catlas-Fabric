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

package gg.skytils.skytilsmod.features.impl.mining

import gg.essential.universal.UMatrixStack
import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.play.WorldUnloadEvent
import gg.skytils.event.impl.render.WorldDrawEvent
import gg.skytils.event.impl.world.BlockStateUpdateEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod._event.PacketReceiveEvent
import gg.skytils.skytilsmod._event.PacketSendEvent
import gg.skytils.skytilsmod.utils.*
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.block.Blocks
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket
import net.minecraft.network.packet.s2c.play.PlaySoundIdS2CPacket
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket
import net.minecraft.util.math.Box
import net.minecraft.util.math.BlockPos
import net.minecraft.particle.ParticleType
import net.minecraft.util.math.Vec3d
import java.awt.Color
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

object CHTreasureChestHelper : EventSubscriber {

    var lastFoundChest = -1L
    var found = 0

    var lastOpenedChest: BlockPos? = null

    val chests = ConcurrentHashMap<BlockPos, CHTreasureChest>()

    data class CHTreasureChest(
        val pos: BlockPos,
        var progress: Int = 0,
        val time: Long = System.currentTimeMillis(),
        var particle: Vec3d? = null
    ) {
        val box = Box(
            pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(),
            (pos.x + 1).toDouble(), (pos.y + 1).toDouble(), (pos.z + 1).toDouble()
        )

        var particleBox: Box? = null
    }

    override fun setup() {
        register(::onBlockChange)
    }

    fun onBlockChange(event: BlockStateUpdateEvent) {
        if (!Skytils.config.chTreasureHelper || mc.player == null || SBInfo.mode != SkyblockIsland.CrystalHollows.mode) return
        if (((event.old.block == Blocks.AIR || event.old.block == Blocks.STONE) && event.update.block == Blocks.field_0_680)) {
            printDevMessage({ "Distance ${event.pos} ${mc.player.method_5831(event.pos)}" }, "chtreasure")
            if (mc.player.boundingBox.expand(8.0, 8.0, 8.0).isPosInside(event.pos)) {
                val diff = System.currentTimeMillis() - lastFoundChest
                if (diff < 1000 && found > 0) {
                    found--
                    chests[event.pos] = CHTreasureChest(event.pos)
                    printDevMessage({ "chest found at $diff" }, "chtreasure")
                } else found = 0
            }
        } else if (event.old.block == Blocks.field_0_680 && event.update.block == Blocks.AIR) {
            chests.remove(event.pos)
        }
    }

    fun onSendPacket(event: PacketSendEvent<*>) {
        if (chests.isEmpty() || !Skytils.config.chTreasureHelper || SBInfo.mode != SkyblockIsland.CrystalHollows.mode) return

        when (val packet = event.packet) {
            is PlayerInteractBlockC2SPacket -> {
                if (chests.containsKey(packet.method_12548())) {
                    lastOpenedChest = packet.method_12548()
                }
            }
        }
    }

    fun onReceivePacket(event: PacketReceiveEvent<*>) {
        if (!Skytils.config.chTreasureHelper || SBInfo.mode != SkyblockIsland.CrystalHollows.mode) return

        when (val packet = event.packet) {
            is GameMessageS2CPacket -> {
                val formatted = packet.message.method_10865()
                if (packet.type != 2.toByte()) {
                    if (formatted == "§r§aYou uncovered a treasure chest!§r") {
                        lastFoundChest = System.currentTimeMillis()
                        found++
                    } else if (lastOpenedChest != null && Utils.equalsOneOf(
                            formatted,
                            "§r§6You have successfully picked the lock on this chest!",
                            "§r§aThe remaining contents of this treasure chest were placed in your inventory"
                        )
                    ) {
                        chests.remove(lastOpenedChest)
                        lastOpenedChest = null
                    }
                }
            }
            is ParticleS2CPacket -> {
                packet.apply {
                    if (type == ParticleType.CRIT && isLongDistance && count == 1 && speed == 0f && offsetX == 0f && offsetY == 0f && offsetZ == 0f) {
                        val probable = chests.values.minByOrNull {
                            it.pos.method_10261(x, y, z)
                        } ?: return

                        if (probable.pos.getSquaredDistanceFromCenter(x, y, z) < 2.5) {
                            probable.particle = Vec3d(x, y, z)
                            probable.particleBox = Box(
                                probable.particle!!.x,
                                probable.particle!!.y,
                                probable.particle!!.z,
                                probable.particle!!.x + 0.1,
                                probable.particle!!.y + 0.1,
                                probable.particle!!.z + 0.1
                            )
                            printDevMessage(
                                { "$count ${if (isLongDistance) "long-distance" else ""} ${type.method_0_4941()} particles with $speed speed at $x, $y, $z, offset by $offsetX, $offsetY, $offsetZ" },
                                "chtreasure"
                            )
                        }
                    }
                }
            }
            is PlaySoundIdS2CPacket -> {
                val sound = packet.method_11460()
                val pitch = packet.pitch
                val volume = packet.volume
                val x = packet.x
                val y = packet.y
                val z = packet.z
                if (volume == 1f && pitch == 1f && Utils.equalsOneOf(
                        sound,
                        "random.orb",
                        "mob.villager.no"
                    ) && chests.isNotEmpty()
                ) {
                    val probable = chests.values.minByOrNull {
                        val rot = mc.player.getRotationFor(it.pos)
                        abs(rot.first - mc.player.yaw) + abs(rot.second - mc.player.pitch)
                    } ?: return
                    if (sound == "random.orb") probable.progress++
                    else probable.progress = 0
                    printDevMessage({ "sound $sound, $pitch pitch, $volume volume, at $x, $y, $z" }, "chtreasure")
                }
            }
        }
    }

    fun onRender(event: WorldDrawEvent) {
        if (!Skytils.config.chTreasureHelper || chests.isEmpty() || SBInfo.mode != SkyblockIsland.CrystalHollows.mode) return
        val (viewerX, viewerY, viewerZ) = RenderUtil.getViewerPos(event.partialTicks)
        val matrixStack = UMatrixStack()
        val time = System.currentTimeMillis()
        chests.entries.removeAll { (pos, chest) ->
            RenderSystem.disableCull()
            RenderUtil.drawFilledBoundingBox(
                matrixStack,
                chest.box.offset(-viewerX, -viewerY, -viewerZ).expand(0.01, 0.01, 0.01),
                Color(255, 0, 0, 69),
                1f
            )
            RenderUtil.drawLabel(
                Vec3d(pos).add(0.5, 1.5, 0.5),
                "${chest.progress}/5",
                Color.ORANGE,
                event.partialTicks,
                matrixStack
            )
            if (chest.particleBox != null) {
                RenderUtil.drawFilledBoundingBox(
                    matrixStack,
                    chest.particleBox!!.offset(-viewerX, -viewerY, -viewerZ),
                    Color(255, 0, 255, 69),
                    1f
                )
            }
            RenderSystem.enableCull()
            return@removeAll (time - chest.time) > (5 * 1000 * 60)
        }
    }

    fun onWorldChange(event: WorldUnloadEvent) {
        chests.clear()
        lastFoundChest = -1L
    }

}