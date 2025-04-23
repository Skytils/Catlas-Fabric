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

import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.play.WorldUnloadEvent
import gg.skytils.event.impl.render.WorldDrawEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod._event.DungeonPuzzleDiscoveredEvent
import gg.skytils.skytilsmod.core.tickTimer
import gg.skytils.skytilsmod.listeners.DungeonListener
import gg.skytils.skytilsmod.utils.*
import gg.skytils.skytilsmod.utils.graphics.colors.CommonColors
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.entity.mob.CreeperEntity
import net.minecraft.block.Blocks
import net.minecraft.util.math.Box
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11
import java.awt.Color


object CreeperSolver : EventSubscriber {
    private val colors = CommonColors.set.copySet()
    private val solutionPairs = arrayListOf<Pair<BlockPos, BlockPos>>()
    private var creeper: CreeperEntity? = null
    private val candidateBlocks = setOf(Blocks.PRISMARINE, Blocks.SEA_LANTERN)

    fun onPuzzleDiscovered(event: DungeonPuzzleDiscoveredEvent) {
        if (event.puzzle == "Creeper Beams") {
            updatePuzzleState()
        }
    }

    fun updatePuzzleState() {
        if (this.creeper == null) {
            val creeperScan = mc.player?.boundingBox?.expand(14.0, 8.0, 13.0) ?: return
            this.creeper = mc.world?.getEntitiesByClass(CreeperEntity::class.java, creeperScan) {
                it != null && !it.isInvisible && it.maxHealth == 20f && it.health == 20f && !it.hasCustomName()
            }?.firstOrNull()
        } else if (solutionPairs.isEmpty()) {
            val baseBlock = BlockPos(this.creeper!!.x, 75.0, this.creeper!!.z).toBoundingBox()
            val validBox = Box(baseBlock.minX, baseBlock.minY, baseBlock.minZ, baseBlock.maxX, baseBlock.maxY + 2, baseBlock.maxZ)

            val roomBB = this.creeper!!.boundingBox.expand(14.0, 10.0, 13.0)
            val candidates = BlockPos.iterate(BlockPos(roomBB.minVec), BlockPos(roomBB.maxVec)).filter {
                it.y > 68 && mc.world?.getBlockState(it)?.block in candidateBlocks
            }
            val pairs = candidates.elementPairs()
            val usedPositions = mutableSetOf<BlockPos>()
            solutionPairs.addAll(pairs.filter { (a, b) ->
                if (a in usedPositions || b in usedPositions) return@filter false
                checkLineBox(validBox, a.middleVec(), b.middleVec(), Holder(Vec3d(0.0, 0.0, 0.0))).also {
                    if (it) {
                        usedPositions.add(a)
                        usedPositions.add(b)
                    }
                }
            })
        }
    }

    init {
        tickTimer(20, repeats = true) {
            if (Skytils.config.creeperBeamsSolver && Utils.inDungeons && DungeonListener.incompletePuzzles.contains(
                    "Creeper Beams"
                )
            ) {
                updatePuzzleState()
            }
        }
    }

    fun onWorldRender(event: WorldDrawEvent) {
        if (Skytils.config.creeperBeamsSolver && solutionPairs.isNotEmpty() && !creeper!!.isRemoved && DungeonListener.incompletePuzzles.contains(
                "Creeper Beams"
            )
        ) {
            val (viewerX, viewerY, viewerZ) = RenderUtil.getViewerPos(event.partialTicks)

            RenderSystem.disableCull()
            val blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND)
            UGraphics.enableBlend()
            UGraphics.tryBlendFuncSeparate(770, 771, 1, 0)
            val matrixStack = UMatrixStack()
            for (i in solutionPairs.indices) {
                val (one, two) = solutionPairs[i]
                if (mc.world?.getBlockState(one)?.block == Blocks.PRISMARINE && mc.world?.getBlockState(two)?.block == Blocks.PRISMARINE) {
                    continue
                }
                val color = Color(colors[i % colors.size].toInt())
                val first = Vec3d(one).add(-viewerX, -viewerY, -viewerZ)
                val second = Vec3d(two).add(-viewerX, -viewerY, -viewerZ)
                val aabb1 = Box(
                    first.x,
                    first.y,
                    first.z,
                    first.x + 1,
                    first.y + 1,
                    first.z + 1
                )
                val aabb2 = Box(
                    second.x,
                    second.y,
                    second.z,
                    second.x + 1,
                    second.y + 1,
                    second.z + 1
                )
                RenderUtil.drawFilledBoundingBox(
                    matrixStack, aabb1.expand(0.01, 0.01, 0.01), color, 0.8f
                )
                RenderUtil.drawFilledBoundingBox(
                    matrixStack, aabb2.expand(0.01, 0.01, 0.01), color, 0.8f
                )
            }
            if (!blendEnabled) UGraphics.disableBlend()
            RenderSystem.enableCull()
        }
    }

    fun onWorldChange(event: WorldUnloadEvent) {
        creeper = null
        solutionPairs.clear()
    }

    /**
     * @author qJake
     * @link https://stackoverflow.com/a/3235902
     * https://creativecommons.org/licenses/by-sa/2.5/
     * Modified
     */
    private fun checkLineBox(bb: Box, point1: Vec3d, point2: Vec3d, hitVec: Holder<Vec3d>): Boolean {
        val minVec = bb.minVec
        val maxVec = bb.maxVec
        if (point2.x < minVec.x && point1.x < minVec.x) return false
        if (point2.x > maxVec.x && point1.x > maxVec.x) return false
        if (point2.y < minVec.y && point1.y < minVec.y) return false
        if (point2.y > maxVec.y && point1.y > maxVec.y) return false
        if (point2.z < minVec.z && point1.z < minVec.z) return false
        if (point2.z > maxVec.z && point1.z > maxVec.z) return false
        if (bb.contains(point1)) {
            hitVec.value = point1
            return true
        }
        if ((getIntersection(
                point1.x - minVec.x,
                point2.x - minVec.x,
                point1,
                point2,
                hitVec
            ) && bb.isVecInYZ(hitVec.value))
            || (getIntersection(
                point1.y - minVec.y,
                point2.y - minVec.y,
                point1,
                point2,
                hitVec
            ) && bb.isVecInXZ(hitVec.value))
            || (getIntersection(
                point1.z - minVec.z,
                point2.z - minVec.z,
                point1,
                point2,
                hitVec
            ) && bb.isVecInXY(hitVec.value))
            || (getIntersection(
                point1.x - maxVec.x,
                point2.x - maxVec.x,
                point1,
                point2,
                hitVec
            ) && bb.isVecInYZ(hitVec.value))
            || (getIntersection(
                point1.y - maxVec.y,
                point2.y - maxVec.y,
                point1,
                point2,
                hitVec
            ) && bb.isVecInXZ(hitVec.value))
            || (getIntersection(
                point1.z - maxVec.z,
                point2.z - maxVec.z,
                point1,
                point2,
                hitVec
            ) && bb.isVecInXY(hitVec.value))
        ) return true

        return false
    }

    /**
     * @author qJake
     * @link https://stackoverflow.com/a/3235902
     * https://creativecommons.org/licenses/by-sa/2.5/
     * Modified
     */
    private fun getIntersection(
        dist1: Double,
        dist2: Double,
        point1: Vec3d,
        point2: Vec3d,
        hitVec: Holder<Vec3d>
    ): Boolean {
        if ((dist1 * dist2) >= 0.0f) return false
        if (dist1 == dist2) return false
        hitVec.value = point1 + ((point2 - point1) * (-dist1 / (dist2 - dist1)))
        return true
    }

    /**
     * Checks if the specified vector is within the YZ dimensions of the bounding box.
     */
    private fun Box.isVecInYZ(vec: Vec3d): Boolean {
        return vec.y >= this.minY && vec.y <= this.maxY && vec.z >= this.minZ && vec.z <= this.maxZ
    }

    /**
     * Checks if the specified vector is within the XZ dimensions of the bounding box.
     */
    private fun Box.isVecInXZ(vec: Vec3d): Boolean {
        return vec.x >= this.minX && vec.x <= this.maxX && vec.z >= this.minZ && vec.z <= this.maxZ

    }

    /**
     * Checks if the specified vector is within the XY dimensions of the bounding box.
     */
    private fun Box.isVecInXY(vec: Vec3d): Boolean {
        return vec.x >= this.minX && vec.x <= this.maxX && vec.y >= this.minY && vec.y <= this.maxY
    }

    private class Holder<T>(var value: T)

    override fun setup() {
        register(::onPuzzleDiscovered)
        register(::onWorldRender)
        register(::onWorldChange)
    }
}