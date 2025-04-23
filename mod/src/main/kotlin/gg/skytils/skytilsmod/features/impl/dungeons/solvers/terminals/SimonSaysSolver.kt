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
package gg.skytils.skytilsmod.features.impl.dungeons.solvers.terminals

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
import gg.skytils.skytilsmod.utils.RenderUtil
import gg.skytils.skytilsmod.utils.SuperSecretSettings
import gg.skytils.skytilsmod.utils.Utils
import gg.skytils.skytilsmod.utils.middleVec
import gg.skytils.skytilsmod.utils.printDevMessage
import net.minecraft.block.StoneButtonBlock
import net.minecraft.client.network.OtherClientPlayerEntity
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.block.Blocks
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket
import net.minecraft.util.math.Box
import net.minecraft.util.math.BlockPos
import net.minecraft.util.hit.HitResult
import java.awt.Color

object SimonSaysSolver : EventSubscriber {
    val startBtn = BlockPos(110, 121, 91)
    private val clickInOrder = ArrayList<BlockPos>()
    private var clickNeeded = 0

    override fun setup() {
        register(::onReceivePacket)
        register(::onSendPacket)
        register(::onBlockChange)
        register(::onRenderWorld)
        register(::onWorldChange)
    }

    fun onReceivePacket(event: PacketReceiveEvent<*>) {
        if (Skytils.config.simonSaysSolver && Utils.inDungeons && clickInOrder.isNotEmpty() && clickNeeded < clickInOrder.size) {
            if (Skytils.config.predictSimonClicks && event.packet is EntityAnimationS2CPacket && event.packet.animationId == 0) {
                val entity = mc.world.getEntityById(event.packet.id) as? OtherClientPlayerEntity ?: return
                if (entity.x in 105.0..115.0 && entity.y in 115.0..128.0 && entity.z in 87.0..100.0) {
                    val rayCast = entity.raycast(5.0, RenderUtil.getPartialTicks())
                    if (rayCast.type == HitResult.Type.BLOCK) {
                        val hitPos = rayCast.blockPos ?: return
                        if (hitPos.x in 110..111 && hitPos.y in 120..123 && hitPos.z in 92..95) {
                            clickNeeded++
                            printDevMessage("Registered teammate click on Simon Says.", "simon")
                        }
                    }
                }
            }
        }
    }

    fun onSendPacket(event: PacketSendEvent<*>) {
        if (Skytils.config.simonSaysSolver && Utils.inDungeons && clickInOrder.isNotEmpty() && clickNeeded < clickInOrder.size) {
            if (event.packet is PlayerInteractBlockC2SPacket || event.packet is PlayerActionC2SPacket) {
                val pos = when (event.packet) {
                    is PlayerActionC2SPacket -> event.packet.pos
                    is PlayerInteractBlockC2SPacket -> event.packet.method_12548()
                    else -> error("can't reach")
                }.east()
                if (pos.x == 111 && pos.y in 120..123 && pos.z in 92..95) {
                    if (SuperSecretSettings.azooPuzzoo && clickInOrder.size == 3 && clickNeeded == 0 && pos == clickInOrder[1]) {
                        clickNeeded += 2
                    } else if (clickInOrder[clickNeeded] != pos) {
                        if (Skytils.config.blockIncorrectTerminalClicks) {
                            printDevMessage({ "Prevented click on $pos" }, "simon")
                            event.cancelled = true
                        }
                    } else {
                        clickNeeded++
                    }
                }
            }
        }
    }

    fun onBlockChange(event: BlockStateUpdateEvent) {
        val pos = event.pos
        val old = event.old
        val state = event.update
        if (Utils.inDungeons && Skytils.config.simonSaysSolver && TerminalFeatures.isInPhase3()) {
            if ((pos.y in 120..123) && pos.z in 92..95) {
                if (pos.x == 111) {
                    printDevMessage({ "Block at $pos changed to ${state.block.name} from ${old.block.name}" }, "simon")
                    if (state.block === Blocks.OBSIDIAN && old.block === Blocks.SEA_LANTERN) {
                        if (!clickInOrder.contains(pos)) {
                            clickInOrder.add(pos)
                        } else printDevMessage({ "Duplicate block at $pos" }, "simon")
                    }
                } else if (pos.x == 110) {
                    if (state.block === Blocks.AIR) {
                        printDevMessage("Buttons on simon says were removed!", "simon")
                        clickNeeded = 0
                        clickInOrder.clear()
                    } /*else if (state.block === Blocks.stone_button) {
                        if (old.block === Blocks.stone_button) {
                            if (state.getValue(BlockButtonStone.POWERED)) {
                                //println("Button on simon says was pressed")
                                clickNeeded++
                            }
                        }
                    }*/
                }
            } else if (pos == startBtn && state.block === Blocks.STONE_BUTTON && state.testProperty(StoneButtonBlock.POWERED)) {
                printDevMessage("Simon says was started", "simon")
                clickInOrder.clear()
                clickNeeded = 0
            }
        }
    }

    fun onRenderWorld(event: WorldDrawEvent) {
        val (viewerX, viewerY, viewerZ) = RenderUtil.getViewerPos(event.partialTicks)

        if (Skytils.config.simonSaysSolver && clickNeeded < clickInOrder.size) {
            val matrixStack = UMatrixStack()
            RenderUtil.drawLabel(
                startBtn.middleVec(),
                "${clickNeeded}/${clickInOrder.size}",
                Color.WHITE,
                event.partialTicks,
                matrixStack
            )

            for (i in clickNeeded..<clickInOrder.size) {
                val pos = clickInOrder[i]
                val x = pos.x - viewerX
                val y = pos.y - viewerY + .372
                val z = pos.z - viewerZ + .308
                val color = when (i) {
                    clickNeeded -> Color.GREEN
                    clickNeeded + 1 -> Color.YELLOW
                    else -> Color.RED
                }

                RenderUtil.drawFilledBoundingBox(
                    matrixStack,
                    Box(x, y, z, x - .13, y + .26, z + .382),
                    color,
                    0.5f
                )
            }
            RenderSystem.enableCull()
        }
    }

    fun onWorldChange(event: WorldUnloadEvent) {
        clickInOrder.clear()
        clickNeeded = 0
    }
}