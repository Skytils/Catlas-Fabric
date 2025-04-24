/*
 * Skytils - Hypixel Skyblock Quality of Life Mod
 * Copyright (C) 2020-2025 Skytils
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

import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.play.WorldUnloadEvent
import gg.skytils.event.impl.render.WorldDrawEvent
import gg.skytils.event.impl.world.BlockStateUpdateEvent
import gg.skytils.event.impl.world.ChunkLoadEvent
import gg.skytils.event.register
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexRendering
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.ChunkSectionPos
import net.minecraft.util.shape.VoxelShapes
import java.awt.Color
import kotlin.math.max

object HeightProvider : EventSubscriber {
    val heightMap = Long2IntOpenHashMap()

    override fun setup() {
        register(::onWorldUnload)
        register(::onBlockChange)
        register(::onChunkLoad)
        register(::onWorldDraw)
    }

    fun onWorldDraw(event: WorldDrawEvent) {
        val vertexConsumer: VertexConsumer = event.entityVertexConsumers.getBuffer(RenderLayer.getLines())
        for ((k, v) in heightMap) {
            val pos = BlockPos.fromLong(k).withY(v)
            if (!pos.isWithinDistance(event.camera.blockPos, 160.0)) continue
            val matrices = MatrixStack()
            matrices.peek().positionMatrix.mul(event.positionMatrix)
            matrices.push()
            matrices.translate(pos.x - event.camera.pos.x, pos.y - event.camera.pos.y, pos.z - event.camera.pos.z)
            VertexRendering.drawOutline(
                matrices,
                vertexConsumer,
                VoxelShapes.cuboid(Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)),
                0.0,
                0.0,
                0.0,
                Color.RED.rgb
            )
            matrices.pop()
        }
        event.entityVertexConsumers.drawCurrentLayer()
    }

    fun onWorldUnload(event: WorldUnloadEvent) {
        heightMap.clear()
    }

    fun onBlockChange(event: BlockStateUpdateEvent) {
        if (event.update.isAir) return
        heightMap.compute(event.pos.mapKey()) { _, v ->
            if (v != null) max(v, event.pos.y) else event.pos.y
        }
    }

    fun onChunkLoad(event: ChunkLoadEvent) {
        val highestSection = event.chunk.getHighestNonEmptySection()
        if (highestSection == -1) {
            for (x in 0..15) {
                for (z in 0..15) {
                    heightMap[event.chunk.pos.getBlockPos(x, 0, z).mapKey()] = event.chunk.bottomY
                }
            }
        } else {
            val mut = BlockPos.Mutable()
            val highestY = ChunkSectionPos.getBlockCoord(event.chunk.sectionIndexToCoord(highestSection) + 1) + 15
            val lowestY = event.chunk.bottomY

            for (x in event.chunk.pos.startX..event.chunk.pos.endX) {
                mut.x = x
                for (z in event.chunk.pos.startZ..event.chunk.pos.endZ) {
                    mut.z = z
                    for (y in highestY downTo lowestY) {
                        mut.y = y
                        val state = event.chunk.getBlockState(mut)
                        if (!state.isAir) {
                            mut.y = 0
                            heightMap[mut.mapKey()] = y
                            break
                        }
                    }
                }
            }
        }
    }

    fun getHeight(pos: BlockPos): Int? = heightMap[pos.mapKey()]

    fun getHeight(x: Int, z: Int): Int? = heightMap[BlockPos.asLong(x, 0, z)]

    private fun BlockPos.mapKey() = BlockPos.asLong(x, 0, z)
}