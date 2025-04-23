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

import gg.essential.universal.UMatrixStack
import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.play.WorldUnloadEvent
import gg.skytils.event.impl.render.WorldDrawEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod.core.tickTimer
import gg.skytils.skytilsmod.listeners.DungeonListener
import gg.skytils.skytilsmod.utils.RenderUtil
import gg.skytils.skytilsmod.utils.Utils
import gg.skytils.skytilsmod.utils.multiplatform.UDirection
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.minecraft.block.Block
import net.minecraft.block.LeverBlock
import net.minecraft.block.Blocks
import net.minecraft.util.DyeColor
import net.minecraft.block.entity.ChestBlockEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import java.awt.Color
import java.util.*

/**
 * Original code was taken from Danker's Skyblock Mod under GPL 3.0 license and modified by the Skytils team
 *
 * @author bowser0000
 * @link https://github.com/bowser0000/SkyblockMod/blob/master/LICENSE
 */
object WaterBoardSolver : EventSubscriber {
    private val solutions = hashMapOf<WoolColor, Set<LeverBlock>>()
    private var chestPos: BlockPos? = null
    private var roomFacing: Direction? = null
    private var prevInWaterRoom = false
    private var inWaterRoom = false
    private var variant = -1
    private var job: Job? = null

    override fun setup() {
        register(::onWorldChange)
        register(::onRenderWorld)
    }

    init {
        tickTimer(20, repeats = true) {
            if (!Skytils.config.waterBoardSolver || !Utils.inDungeons) return@tickTimer
            val player = mc.player ?: return@tickTimer
            val world = mc.world ?: return@tickTimer
            if (DungeonListener.incompletePuzzles.contains("Water Board") && variant == -1 && (job == null || job?.isCancelled == true || job?.isCompleted == true)) {
                job = Skytils.launch {
                    prevInWaterRoom = inWaterRoom
                    inWaterRoom = false
                    if (Utils.getBlocksWithinRangeAtSameY(player.blockPos, 13, 54).any {
                            world.getBlockState(it).block == Blocks.field_0_628
                        }) {
                        if (chestPos == null || roomFacing == null) {
                            val playerX = mc.player.x.toInt()
                            val playerZ = mc.player.z.toInt()
                            val xRange = playerX - 25..playerX + 25
                            val zRange = playerZ - 25..playerZ + 25
                            findChest@
                            for (te in world.field_0_262) {
                                if (te.pos.y == 56 && te is ChestBlockEntity && te.viewerCount == 0 && te.pos.x in xRange && te.pos.z in zRange
                                ) {
                                    val potentialChestPos = te.pos
                                    if (world.getBlockState(potentialChestPos.method_10074()).block === Blocks.STONE && world.getBlockState(
                                            potentialChestPos.up(2)
                                        ).block === Blocks.field_0_761
                                    ) {
                                        for (direction in UDirection.HORIZONTALS) {
                                            if (world.getBlockState(
                                                    potentialChestPos.offset(direction.opposite, 3).down(2)
                                                ).block === Blocks.field_0_628 && world.getBlockState(
                                                    potentialChestPos.offset(direction, 2)
                                                ).block === Blocks.STONE
                                            ) {
                                                chestPos = potentialChestPos
                                                roomFacing = direction
                                                println("Water board chest is at $chestPos")
                                                println("Water board room is facing $direction")
                                                break@findChest
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (chestPos == null) return@launch
                        for (blockPos in Utils.getBlocksWithinRangeAtSameY(player.blockPos, 25, 82)) {
                            if (world.getBlockState(blockPos).block === Blocks.field_0_633) {
                                inWaterRoom = true
                                if (!prevInWaterRoom) {
                                    var foundGold = false
                                    var foundClay = false
                                    var foundEmerald = false
                                    var foundQuartz = false
                                    var foundDiamond = false
                                    val x = blockPos.x
                                    val z = blockPos.z

                                    // Detect first blocks near water stream
                                    for (puzzleBlockPos in BlockPos.iterate(
                                        BlockPos(x + 1, 78, z + 1),
                                        BlockPos(x - 1, 77, z - 1)
                                    )) {
                                        when (world.getBlockState(puzzleBlockPos).block) {
                                            Blocks.GOLD_BLOCK -> foundGold = true
                                            Blocks.HARDENED_CLAY -> foundClay = true
                                            Blocks.EMERALD_BLOCK -> foundEmerald = true
                                            Blocks.QUARTZ_BLOCK -> foundQuartz = true
                                            Blocks.DIAMOND_BLOCK -> foundDiamond = true
                                        }
                                    }
                                    variant = when {
                                        foundGold && foundClay -> 0
                                        foundEmerald && foundQuartz -> 1
                                        foundQuartz && foundDiamond -> 2
                                        foundGold && foundQuartz -> 3
                                        else -> -1
                                    }

                                    when (variant) {
                                        0 -> {
                                            solutions[WoolColor.PURPLE] = setOf(
                                                LeverBlock.QUARTZ,
                                                LeverBlock.GOLD,
                                                LeverBlock.DIAMOND,
                                                LeverBlock.CLAY
                                            )
                                            solutions[WoolColor.ORANGE] =
                                                setOf(LeverBlock.GOLD, LeverBlock.COAL, LeverBlock.EMERALD)
                                            solutions[WoolColor.BLUE] = setOf(
                                                LeverBlock.QUARTZ,
                                                LeverBlock.GOLD,
                                                LeverBlock.EMERALD,
                                                LeverBlock.CLAY
                                            )
                                            solutions[WoolColor.GREEN] = setOf(LeverBlock.EMERALD)
                                            solutions[WoolColor.RED] = setOf()
                                        }

                                        1 -> {
                                            solutions[WoolColor.PURPLE] = setOf(LeverBlock.COAL)
                                            solutions[WoolColor.ORANGE] = setOf(
                                                LeverBlock.QUARTZ,
                                                LeverBlock.GOLD,
                                                LeverBlock.EMERALD,
                                                LeverBlock.CLAY
                                            )
                                            solutions[WoolColor.BLUE] = setOf(
                                                LeverBlock.QUARTZ,
                                                LeverBlock.DIAMOND,
                                                LeverBlock.EMERALD
                                            )
                                            solutions[WoolColor.GREEN] =
                                                setOf(LeverBlock.QUARTZ, LeverBlock.EMERALD)
                                            solutions[WoolColor.RED] =
                                                setOf(LeverBlock.QUARTZ, LeverBlock.COAL, LeverBlock.EMERALD)
                                        }

                                        2 -> {
                                            solutions[WoolColor.PURPLE] =
                                                setOf(LeverBlock.QUARTZ, LeverBlock.GOLD, LeverBlock.DIAMOND)
                                            solutions[WoolColor.ORANGE] = setOf(LeverBlock.EMERALD)
                                            solutions[WoolColor.BLUE] =
                                                setOf(LeverBlock.QUARTZ, LeverBlock.DIAMOND)
                                            solutions[WoolColor.GREEN] = setOf()
                                            solutions[WoolColor.RED] =
                                                setOf(LeverBlock.GOLD, LeverBlock.EMERALD)
                                        }

                                        3 -> {
                                            solutions[WoolColor.PURPLE] = setOf(
                                                LeverBlock.QUARTZ,
                                                LeverBlock.GOLD,
                                                LeverBlock.EMERALD,
                                                LeverBlock.CLAY
                                            )
                                            solutions[WoolColor.ORANGE] =
                                                setOf(LeverBlock.GOLD, LeverBlock.COAL)
                                            solutions[WoolColor.BLUE] = setOf(
                                                LeverBlock.QUARTZ,
                                                LeverBlock.GOLD,
                                                LeverBlock.COAL,
                                                LeverBlock.EMERALD,
                                                LeverBlock.CLAY
                                            )
                                            solutions[WoolColor.GREEN] =
                                                setOf(LeverBlock.GOLD, LeverBlock.EMERALD)
                                            solutions[WoolColor.RED] = setOf(
                                                LeverBlock.GOLD,
                                                LeverBlock.DIAMOND,
                                                LeverBlock.EMERALD,
                                                LeverBlock.CLAY
                                            )
                                        }

                                        else -> {
                                        }
                                    }
                                    break
                                }
                            }
                        }
                    } else {
                        variant = -1
                        solutions.clear()
                    }
                }
            }
        }
    }

    fun onRenderWorld(event: WorldDrawEvent) {
        if (!Skytils.config.waterBoardSolver || !DungeonListener.incompletePuzzles.contains("Water Board")) return
        if (chestPos == null || roomFacing == null || variant == -1) return
        val leverStates = LeverBlock.entries.associateWithTo(EnumMap(LeverBlock::class.java)) {
            getLeverToggleState(it.leverPos)
        }
        val renderTimes = HashMap<LeverBlock, Int>()
        var matching = 0
        val matrixStack = UMatrixStack()
        for (color in WoolColor.entries) {
            val renderColor = Color(color.dyeColor.method_7788().color).brighter()
            if (color.isExtended) {
                val solution = solutions[color] ?: continue
                for ((lever, switched) in leverStates) {
                    if (switched && !solution.contains(lever) || !switched && solution.contains(lever)) {
                        val pos = lever.leverPos
                        val displayed =
                            renderTimes.compute(lever) { _: LeverBlock?, v: Int? -> v?.inc() ?: 0 }
                        RenderUtil.drawLabel(
                            Vec3d(pos!!.up()).add(0.5, 0.5 + 0.5 * displayed!!, 0.5),
                            "§l" + color.name,
                            renderColor,
                            event.partialTicks,
                            matrixStack
                        )
                    }
                }
                if (leverStates.entries.all { (key, value) ->
                        value && solution.contains(key) || !value && !solution.contains(
                            key
                        )
                    }) {
                    RenderUtil.drawLabel(
                        Vec3d(chestPos!!.offset(roomFacing!!.opposite, 17).up(5)).add(
                            0.5,
                            0.5 + 0.5 * matching,
                            0.5
                        ), "§l" + color.name, renderColor, event.partialTicks, matrixStack
                    )
                    matching++
                }
            }
        }
    }

    fun onWorldChange(event: WorldUnloadEvent) {
        variant = -1
        solutions.clear()
        chestPos = null
        roomFacing = null
        prevInWaterRoom = false
        inWaterRoom = false
    }

    private fun getLeverToggleState(pos: BlockPos?): Boolean {
        val block = mc.world.getBlockState(pos)
        return if (block.block !== Blocks.LEVER) false else block.testProperty(LeverBlock.POWERED)
    }

    enum class WoolColor(var dyeColor: DyeColor) {
        PURPLE(DyeColor.PURPLE),
        ORANGE(DyeColor.ORANGE),
        BLUE(DyeColor.BLUE),
        GREEN(DyeColor.GREEN),
        RED(DyeColor.RED);

        val isExtended: Boolean
            get() = if (chestPos == null || roomFacing == null) false else mc.world.getBlockState(
                chestPos!!.offset(
                    roomFacing!!.opposite, 3 + ordinal
                )
            ).block == Blocks.WOOL
    }

    enum class LeverBlock(var block: Block) {
        QUARTZ(Blocks.QUARTZ_BLOCK),
        GOLD(Blocks.GOLD_BLOCK),
        COAL(Blocks.COAL_BLOCK),
        DIAMOND(Blocks.DIAMOND_BLOCK),
        EMERALD(Blocks.EMERALD_BLOCK),
        CLAY(Blocks.HARDENED_CLAY);

        val leverPos: BlockPos?
            get() {
                if (chestPos == null || roomFacing == null) return null
                val shiftBy = ordinal % 3 * 5
                val leverSide = if (ordinal < 3) roomFacing!!.rotateYClockwise() else roomFacing!!.rotateYCounterclockwise()
                return chestPos!!.up(5).offset(leverSide.opposite, 6).offset(
                    roomFacing!!.opposite, 2 + shiftBy
                ).method_10093(leverSide)
            }
    }
}