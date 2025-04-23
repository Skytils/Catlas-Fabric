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
import gg.skytils.skytilsmod.features.impl.dungeons.DungeonTimer
import gg.skytils.skytilsmod.features.impl.funny.Funny
import gg.skytils.skytilsmod.utils.RenderUtil
import gg.skytils.skytilsmod.utils.Utils
import net.minecraft.block.WeightedPressurePlateBlock
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.block.Blocks
import net.minecraft.util.math.Box
import net.minecraft.util.math.BlockPos
import java.awt.Color

object ShootTheTargetSolver : EventSubscriber {
    private val positions = listOf(
        BlockPos(68, 130, 50), BlockPos(66, 130, 50), BlockPos(64, 130, 50),
        BlockPos(68, 128, 50), BlockPos(66, 128, 50), BlockPos(64, 128, 50),
        BlockPos(68, 126, 50), BlockPos(66, 126, 50), BlockPos(64, 126, 50)
    )
    private val plate = BlockPos(63, 127, 35)

    private val shot = arrayListOf<BlockPos>()

    override fun setup() {
        register(::onBlockChange)
        register(::onRenderWorld)
        register(::onLoad)
    }

    fun onBlockChange(event: BlockStateUpdateEvent) {
        if (!Utils.inDungeons || DungeonTimer.phase2ClearTime == -1L || !Skytils.config.shootTheTargetSolver) return
        val pos = event.pos
        val old = event.old
        val state = event.update
        if (positions.contains(pos)) {
            if (old.block == Blocks.EMERALD_BLOCK && state.block == Blocks.STAINED_HARDENED_CLAY) {
                shot.add(pos)
            }
        } else if (pos == plate && state.block is WeightedPressurePlateBlock) {
            if (state.testProperty(WeightedPressurePlateBlock.POWER) == 0 || old !is WeightedPressurePlateBlock || old.testProperty(
                    WeightedPressurePlateBlock.POWER
                ) == 0
            ) {
                shot.clear()
            }
        }
    }

    fun onRenderWorld(event: WorldDrawEvent) {
        if (!Skytils.config.shootTheTargetSolver || shot.isEmpty()) return
        val (viewerX, viewerY, viewerZ) = RenderUtil.getViewerPos(event.partialTicks)
        val matrixStack = UMatrixStack()

        for (pos in shot) {
            val x = pos.x - viewerX
            val y = pos.y - viewerY
            val z = pos.z - viewerZ
            RenderSystem.disableCull()
            RenderUtil.drawFilledBoundingBox(
                matrixStack,
                Box(x, y, z, x + 1, y + 1, z + 1).expand(0.01, 0.01, 0.01),
                Color.RED,
                0.5f
            )
            RenderSystem.enableCull()
        }
    }

    fun onLoad(event: WorldUnloadEvent) {
        shot.clear()
    }
}