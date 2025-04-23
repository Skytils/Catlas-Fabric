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
package gg.skytils.skytilsmod.features.impl.dungeons.solvers

import gg.essential.elementa.utils.withAlpha
import gg.essential.universal.UChat
import gg.essential.universal.UMatrixStack
import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.play.ChatMessageSentEvent
import gg.skytils.event.impl.play.WorldUnloadEvent
import gg.skytils.event.impl.render.WorldDrawEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod._event.MainThreadPacketReceiveEvent
import gg.skytils.skytilsmod.listeners.DungeonListener
import gg.skytils.skytilsmod.utils.DevTools
import gg.skytils.skytilsmod.utils.RenderUtil
import gg.skytils.skytilsmod.utils.Utils
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.block.Blocks
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.util.math.Box
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import java.awt.Color
import kotlin.math.PI
import kotlin.math.abs

object TeleportMazeSolver : EventSubscriber {

    private val steppedPads = HashSet<BlockPos>()
    val poss = HashSet<BlockPos>()
    val valid = HashSet<BlockPos>()

    override fun setup() {
        register(::onSendMsg)
        register(::onPacket)
        register(::onWorldRender)
        register(::onWorldChange)
    }

    fun onSendMsg(event: ChatMessageSentEvent) {
        if (DevTools.getToggle("tpmaze") && event.message == "/resettp") {
            steppedPads.clear()
            poss.clear()
            event.cancelled = true
        }
    }

    fun onPacket(event: MainThreadPacketReceiveEvent<*>) {
        if (!Skytils.config.teleportMazeSolver || !Utils.inDungeons || !DungeonListener.incompletePuzzles.contains("Teleport Maze")) return
        if (mc.player == null || mc.world == null) return
        event.packet.apply {
            when (this) {
                is PlayerPositionLookS2CPacket -> {
                    if (y == 69.5 && Utils.equalsOneOf(
                            mc.player.y,
                            69.5,
                            69.8125
                        ) && abs(x % 1) == 0.5 && abs(z % 1) == 0.5
                    ) {
                        val currPos = mc.player.blockPos
                        val pos = BlockPos(x, y, z)
                        val oldTpPad = findEndPortalFrame(currPos) ?: return
                        val tpPad = findEndPortalFrame(pos) ?: return
                        steppedPads.add(oldTpPad)
                        if (tpPad !in steppedPads) {
                            steppedPads.add(tpPad)
                            val deg2Rad = PI/180
                            val magicYaw = (-yaw * deg2Rad - PI).toFloat()
                            val yawX = MathHelper.sin(magicYaw)
                            val yawZ = MathHelper.cos(magicYaw)
                            val pitchVal = -MathHelper.cos(-pitch * deg2Rad.toFloat())
                            val vec = Vec3d((yawX * pitchVal).toDouble(), 69.0, (yawZ * pitchVal).toDouble())
                            valid.clear()
                            for (i in 4..23) {
                                val bp = BlockPos(
                                    x + vec.x * i,
                                    vec.y,
                                    z + vec.z * i
                                )
                                val allDir = Utils.getBlocksWithinRangeAtSameY(bp, 2, 69)

                                valid.addAll(allDir.filter {
                                    it !in steppedPads && mc.world.getBlockState(
                                        it
                                    ).block === Blocks.END_PORTAL_FRAME
                                })
                            }
                            if (DevTools.getToggle("tpmaze")) UChat.chat(valid.joinToString { it.toString() })
                            if (poss.isEmpty()) poss.addAll(valid)
                            else poss.removeAll {
                                it !in valid
                            }
                        }
                        if (DevTools.getToggle("tpmaze")) UChat.chat(
                            "current: ${mc.player.pos}, ${mc.player.pitch} ${mc.player.yaw} new: ${this.x} ${this.y} ${this.z} - ${this.pitch} ${this.yaw} - ${
                                this.flags.joinToString { it.name }
                            }"
                        )
                    }
                }
            }
        }
    }

    private fun findEndPortalFrame(pos: BlockPos): BlockPos? {
        return Utils.getBlocksWithinRangeAtSameY(pos, 1, 69).find {
            mc.world.getBlockState(it).block === Blocks.END_PORTAL_FRAME
        }
    }

    fun onWorldRender(event: WorldDrawEvent) {
        if (!Skytils.config.teleportMazeSolver || steppedPads.isEmpty() || !DungeonListener.incompletePuzzles.contains("Teleport Maze")) return
        val (viewerX, viewerY, viewerZ) = RenderUtil.getViewerPos(event.partialTicks)
        val matrixStack = UMatrixStack()

        for (pos in steppedPads) {
            val x = pos.x - viewerX
            val y = pos.y - viewerY
            val z = pos.z - viewerZ
            RenderSystem.disableCull()
            RenderUtil.drawFilledBoundingBox(
                matrixStack,
                Box(x, y, z, x + 1, y + 1, z + 1).expand(0.01, 0.01, 0.01),
                Skytils.config.teleportMazeSolverColor
            )
            RenderSystem.enableCull()
        }

        for (pos in poss) {
            val x = pos.x - viewerX
            val y = pos.y - viewerY
            val z = pos.z - viewerZ
            RenderSystem.disableCull()
            RenderUtil.drawFilledBoundingBox(
                matrixStack,
                Box(x, y, z, x + 1, y + 1, z + 1).expand(0.01, 0.01, 0.01),
                Color.GREEN.withAlpha(69),
                1f
            )
            RenderSystem.enableCull()
        }
    }

    fun onWorldChange(event: WorldUnloadEvent) {
        steppedPads.clear()
        poss.clear()
    }
}