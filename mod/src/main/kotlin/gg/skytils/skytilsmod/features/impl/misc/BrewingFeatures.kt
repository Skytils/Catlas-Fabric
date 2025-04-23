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

package gg.skytils.skytilsmod.features.impl.misc

import gg.essential.universal.UMatrixStack
import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.render.WorldDrawEvent
import gg.skytils.event.impl.screen.GuiContainerBackgroundDrawnEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod._event.PacketSendEvent
import gg.skytils.skytilsmod.core.tickTimer
import gg.skytils.skytilsmod.utils.*
import net.minecraft.screen.GenericContainerScreenHandler
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.block.entity.BrewingStandBlockEntity
import net.minecraft.util.math.BlockPos
import java.awt.Color

object BrewingFeatures : EventSubscriber {
    var lastBrewingStand: BrewingStandBlockEntity? = null
    val brewingStandToTimeMap = hashMapOf<BlockPos, Long>()
    val timeRegex = Regex("§a(?<sec>\\d+(?:.\\d)?)s")
    private val green = Color(0, 255, 0, 128)
    private val red = Color(255, 0, 0, 128)

    init {
        tickTimer(100, repeats = true) {
            if (!Skytils.config.colorBrewingStands || !Utils.inSkyblock || SBInfo.mode != SkyblockIsland.PrivateIsland.mode) return@tickTimer
            brewingStandToTimeMap.entries.removeIf {
                mc.world?.getBlockEntity(it.key) !is BrewingStandBlockEntity
            }
        }
    }

    override fun setup() {
        register(::onPacketSend)
        register(::onContainerUpdate)
        register(::onWorldDraw)
    }

    fun onPacketSend(event: PacketSendEvent<*>) {
        if (!Skytils.config.colorBrewingStands || !Utils.inSkyblock || SBInfo.mode != SkyblockIsland.PrivateIsland.mode) return
        if (event.packet is PlayerInteractBlockC2SPacket && event.packet.method_12548().y != -1) {
            lastBrewingStand = mc.world.getBlockEntity(event.packet.method_12548()) as? BrewingStandBlockEntity ?: return
        }
    }

    fun onContainerUpdate(event: GuiContainerBackgroundDrawnEvent) {
        if (!Skytils.config.colorBrewingStands || !Utils.inSkyblock || SBInfo.mode != SkyblockIsland.PrivateIsland.mode) return
        if (lastBrewingStand == null || event.container !is GenericContainerScreenHandler || event.chestName != "Brewing Stand") return
        val timeSlot = event.container.getSlot(22).stack ?: return
        val time = timeRegex.find(timeSlot.name)?.groups?.get("sec")?.value?.toDoubleOrNull() ?: 0.0
        brewingStandToTimeMap[lastBrewingStand!!.pos] = System.currentTimeMillis() + (time * 1000L).toLong()
    }

    fun onWorldDraw(event: WorldDrawEvent) {
        if (!Skytils.config.colorBrewingStands || !Utils.inSkyblock || SBInfo.mode != SkyblockIsland.PrivateIsland.mode) return
        val (viewerX, viewerY, viewerZ) = RenderUtil.getViewerPos(event.partialTicks)
        val matrixStack = UMatrixStack()
        val currTime = System.currentTimeMillis()
        brewingStandToTimeMap.forEach { (pos, time) ->
            RenderUtil.drawFilledBoundingBox(
                matrixStack,
                pos.toBoundingBox().expandBlock().offset(-viewerX, -viewerY, -viewerZ),
                if (time > currTime) red else green,
                1f
            )
        }
    }
}