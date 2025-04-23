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

import gg.essential.universal.UChat
import gg.essential.universal.UMatrixStack
import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.play.WorldUnloadEvent
import gg.skytils.event.impl.render.WorldDrawEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.failPrefix
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod.Skytils.successPrefix
import gg.skytils.skytilsmod._event.DungeonPuzzleDiscoveredEvent
import gg.skytils.skytilsmod.core.tickTimer
import gg.skytils.skytilsmod.listeners.DungeonListener
import gg.skytils.skytilsmod.utils.RenderUtil
import gg.skytils.skytilsmod.utils.Utils
import gg.skytils.skytilsmod.utils.multiplatform.UDirection
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.minecraft.block.Blocks
import net.minecraft.block.entity.ChestBlockEntity
import net.minecraft.util.math.Box
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import kotlin.math.floor

//#if MC<11300
//$$ import net.minecraft.client.GlStateManager
//#else
import com.mojang.blaze3d.systems.RenderSystem
import gg.skytils.skytilsmod.mixins.transformers.accessors.AccessorWorld
//#endif

object BoulderSolver : EventSubscriber {
    var boulderChest: BlockPos? = null
    var boulderFacing: Direction? = null
    var grid = Array(7) { arrayOfNulls<BoulderState>(6) }
    var roomVariant = -1
    var variantSteps = ArrayList<ArrayList<BoulderPush>>()
    var expectedBoulders = ArrayList<ArrayList<BoulderState>>()
    private var job: Job? = null

    init {
        tickTimer(20, repeats = true, task = ::update)
    }

    fun onPuzzleDiscovered(event: DungeonPuzzleDiscoveredEvent) {
        if (event.puzzle == "Boulder") {
            update()
        }
    }

    fun onRenderWorld(event: WorldDrawEvent) {
        if (!Skytils.config.boulderSolver || !DungeonListener.incompletePuzzles.contains("Boulder")) return
        if (boulderChest == null) return
        val (viewerX, viewerY, viewerZ) = RenderUtil.getViewerPos(event.partialTicks)
        if (roomVariant >= 0) {
            val matrixStack = UMatrixStack()
            val steps = variantSteps[roomVariant]
            for (step in steps) {
                if (grid[step.x][step.y] != BoulderState.EMPTY) {
                    val downRow = boulderFacing!!.opposite
                    val rightColumn = boulderFacing!!.rotateYClockwise()
                    val farLeftPos = boulderChest!!.offset(downRow, 5).offset(rightColumn.opposite, 9)
                    val boulderPos = farLeftPos.offset(rightColumn, 3 * step.x).offset(downRow, 3 * step.y)
                    val actualDirection: Direction? = when (step.direction) {
                        BoulderPushDirection.FORWARD -> boulderFacing
                        BoulderPushDirection.BACKWARD -> boulderFacing!!.opposite
                        BoulderPushDirection.LEFT -> boulderFacing!!.rotateYCounterclockwise()
                        BoulderPushDirection.RIGHT -> boulderFacing!!.rotateYClockwise()
                    }
                    val buttonPos = boulderPos.offset(actualDirection!!.opposite, 2).method_10074()
                    val x = buttonPos.x - viewerX
                    val y = buttonPos.y - viewerY
                    val z = buttonPos.z - viewerZ

                    //#if MC<11300
                    //$$ GlStateManager.disableCull()
                    //#else
                    RenderSystem.disableCull()
                    //#endif

                    RenderUtil.drawFilledBoundingBox(
                        matrixStack,
                        Box(x, y, z, x + 1, y + 1, z + 1),
                        Skytils.config.boulderSolverColor,
                        0.7f
                    )

                    //#if MC<11300
                    //$$ GlStateManager.enableCull()
                    //#else
                    RenderSystem.enableCull()
                    //#endif
                    break
                }
            }
        }
    }

    fun onWorldChange(event: WorldUnloadEvent) {
        reset()
    }

    enum class BoulderPushDirection {
        FORWARD, BACKWARD, LEFT, RIGHT
    }

    enum class BoulderState {
        EMPTY, FILLED, PLACEHOLDER
    }

    class BoulderPush(var x: Int, var y: Int, var direction: BoulderPushDirection)

    fun update() {
        if (!Skytils.config.boulderSolver || !DungeonListener.incompletePuzzles.contains("Boulder")) return
        val player = mc.player
        val world: World? = mc.world
        if ((job == null || job?.isCancelled == true || job?.isCompleted == true) && Utils.inDungeons && world != null && player != null && roomVariant != -2) {
            job = Skytils.launch {
                var foundBirch = false
                var foundBarrier = false
                for (potentialBarrier in Utils.getBlocksWithinRangeAtSameY(player.blockPos, 13, 68)) {
                    if (foundBarrier && foundBirch) break
                    if (!foundBarrier) {
                        if (world.getBlockState(potentialBarrier).block === Blocks.BARRIER) {
                            foundBarrier = true
                        }
                    }
                    if (!foundBirch) {
                        val potentialBirch = potentialBarrier.down(2)
                        //#if MC<12000
                        //$$ if (world.getBlockState(potentialBirch).block === Blocks.PLANKS && Blocks.PLANKS.method_0_726(
                        //$$         world,
                        //$$         potentialBirch
                        //$$     ) == 2
                        //$$ ) {
                        //$$     foundBirch = true
                        //$$ }
                        //#else
                        if (world.getBlockState(potentialBirch).block === Blocks.BIRCH_PLANKS) foundBirch = true
                        //#endif
                    }
                }
                if (!foundBirch || !foundBarrier) return@launch
                if (boulderChest == null || boulderFacing == null) {
                    val playerX = player.x.toInt()
                    val playerZ = player.z.toInt()
                    val xRange = playerX - 25..playerX + 25
                    val zRange = playerZ - 25..playerZ + 25
                    //#if MC<11300
                    //$$ findChest@ for (te in world.field_0_262) {
                    //#else
                    findChest@ for (te in (mc.world!! as AccessorWorld).blockEntityTickers) {
                    //#endif
                        if (te.pos.y == 66 && te.pos.x in xRange && te.pos.z in zRange
                        ) {
                            //#if MC<=11202
                            //$$ if (te is ChestBlockEntity && te.viewerCount == 0) {
                            //#else
                            if (te is ChestBlockEntity && ChestBlockEntity.getPlayersLookingInChestCount(mc.world!!, te.pos) == 0) {
                            //#endif
                                val potentialChestPos = te.pos
                                if (world.getBlockState(potentialChestPos.method_10074()).block ==
                                    //#if MC<11300
                                    //$$ Blocks.STONEBRICK
                                    //#else
                                    Blocks.STONE_BRICKS
                                    //#endif
                                    && world.getBlockState(
                                        potentialChestPos.up(3)
                                    ).block == Blocks.BARRIER
                                ) {
                                    boulderChest = potentialChestPos
                                    println("Boulder chest is at $boulderChest")
                                    for (direction in UDirection.HORIZONTALS) {
                                        if (world.getBlockState(potentialChestPos.method_10093(direction)).block == Blocks.STAINED_HARDENED_CLAY) {
                                            boulderFacing = direction
                                            println("Boulder room is facing $direction")
                                            break@findChest
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val downRow = boulderFacing!!.opposite
                    val rightColumn = boulderFacing!!.rotateYClockwise()
                    val farLeftPos = boulderChest!!.offset(downRow, 5).offset(rightColumn.opposite, 9)
                    var row = 0
                    while (row < 6) {
                        var column = 0
                        while (column < 7) {
                            val current = farLeftPos.offset(rightColumn, 3 * column).offset(downRow, 3 * row)
                            val state = world.getBlockState(current)
                            grid[column][row] =
                                if (state.block === Blocks.AIR) BoulderState.EMPTY else BoulderState.FILLED
                            column++
                        }
                        row++
                    }
                    if (roomVariant == -1) {
                        roomVariant = -2
                        var i = 0
                        while (i < expectedBoulders.size) {
                            val expected = expectedBoulders[i]
                            var isRight = true
                            var j = 0
                            while (j < expected.size) {
                                val column = j % 7
                                val r = floor((j / 7f).toDouble()).toInt()
                                val state = expected[j]
                                if (grid[column][r] != state && state != BoulderState.PLACEHOLDER) {
                                    isRight = false
                                    break
                                }
                                j++
                            }
                            if (isRight) {
                                roomVariant = i
                                UChat.chat("$successPrefix §aSkytils detected boulder variant ${roomVariant + 1}.")
                                break
                            }
                            i++
                        }
                        if (roomVariant == -2) {
                            UChat.chat("$failPrefix §cSkytils couldn't detect the boulder variant.")
                        }
                    }
                }
            }
        }
    }

    fun reset() {
        boulderChest = null
        boulderFacing = null
        grid = Array(7) { arrayOfNulls(6) }
        roomVariant = -1
    }

    init {
        expectedBoulders.add(
            arrayListOf(
                BoulderState.EMPTY,
                BoulderState.FILLED,
                BoulderState.EMPTY,
                BoulderState.EMPTY,
                BoulderState.EMPTY,
                BoulderState.EMPTY,
                BoulderState.EMPTY
            )
        )
        variantSteps.add(
            arrayListOf(
                BoulderPush(2, 4, BoulderPushDirection.RIGHT),
                BoulderPush(2, 3, BoulderPushDirection.FORWARD),
                BoulderPush(3, 3, BoulderPushDirection.RIGHT),
                BoulderPush(4, 3, BoulderPushDirection.RIGHT),
                BoulderPush(4, 1, BoulderPushDirection.FORWARD),
                BoulderPush(5, 1, BoulderPushDirection.RIGHT)
            )
        )
        expectedBoulders.add(
            arrayListOf(
                BoulderState.FILLED,
                BoulderState.EMPTY,
                BoulderState.EMPTY,
                BoulderState.EMPTY,
                BoulderState.EMPTY,
                BoulderState.EMPTY,
                BoulderState.FILLED
            )
        )
        variantSteps.add(
            arrayListOf(
                BoulderPush(3, 4, BoulderPushDirection.FORWARD),
                BoulderPush(2, 4, BoulderPushDirection.LEFT),
                BoulderPush(3, 3, BoulderPushDirection.RIGHT),
                BoulderPush(3, 2, BoulderPushDirection.FORWARD),
                BoulderPush(2, 2, BoulderPushDirection.LEFT),
                BoulderPush(4, 2, BoulderPushDirection.RIGHT),
                BoulderPush(2, 1, BoulderPushDirection.FORWARD),
                BoulderPush(4, 1, BoulderPushDirection.FORWARD),
                BoulderPush(3, 1, BoulderPushDirection.RIGHT)
            )
        )
        expectedBoulders.add(
            arrayListOf(
                BoulderState.FILLED,
                BoulderState.EMPTY,
                BoulderState.EMPTY,
                BoulderState.FILLED,
                BoulderState.EMPTY,
                BoulderState.EMPTY,
                BoulderState.FILLED
            )
        )
        variantSteps.add(arrayListOf(BoulderPush(1, 1, BoulderPushDirection.RIGHT)))
        expectedBoulders.add(
            arrayListOf(
                BoulderState.FILLED,
                BoulderState.FILLED,
                BoulderState.EMPTY,
                BoulderState.EMPTY,
                BoulderState.EMPTY,
                BoulderState.FILLED,
                BoulderState.FILLED
            )
        )
        variantSteps.add(
            arrayListOf(
                BoulderPush(4, 3, BoulderPushDirection.FORWARD),
                BoulderPush(3, 3, BoulderPushDirection.LEFT),
                BoulderPush(3, 1, BoulderPushDirection.FORWARD),
                BoulderPush(2, 1, BoulderPushDirection.LEFT)
            )
        )
        expectedBoulders.add(
            arrayListOf(
                BoulderState.EMPTY,
                BoulderState.EMPTY,
                BoulderState.EMPTY,
                BoulderState.EMPTY,
                BoulderState.EMPTY,
                BoulderState.EMPTY,
                BoulderState.EMPTY,
                BoulderState.EMPTY,
                BoulderState.FILLED
            )
        )
        variantSteps.add(
            arrayListOf(
                BoulderPush(3, 4, BoulderPushDirection.FORWARD),
                BoulderPush(3, 3, BoulderPushDirection.FORWARD),
                BoulderPush(2, 1, BoulderPushDirection.FORWARD),
                BoulderPush(1, 1, BoulderPushDirection.LEFT)
            )
        )
        expectedBoulders.add(
            arrayListOf(
                BoulderState.FILLED,
                BoulderState.EMPTY,
                BoulderState.EMPTY,
                BoulderState.FILLED,
                BoulderState.EMPTY,
                BoulderState.FILLED,
                BoulderState.EMPTY
            )
        )
        variantSteps.add(arrayListOf(BoulderPush(1, 4, BoulderPushDirection.FORWARD), BoulderPush(1, 1, BoulderPushDirection.RIGHT)))
        expectedBoulders.add(
            arrayListOf(
                BoulderState.EMPTY,
                BoulderState.EMPTY,
                BoulderState.EMPTY,
                BoulderState.EMPTY,
                BoulderState.EMPTY,
                BoulderState.EMPTY,
                BoulderState.EMPTY,
                BoulderState.FILLED,
                BoulderState.FILLED,
                BoulderState.FILLED,
                BoulderState.FILLED,
                BoulderState.FILLED,
                BoulderState.FILLED,
                BoulderState.EMPTY,
                BoulderState.FILLED
            )
        )
        variantSteps.add(
            arrayListOf(
                BoulderPush(6, 4, BoulderPushDirection.FORWARD),
                BoulderPush(6, 3, BoulderPushDirection.FORWARD),
                BoulderPush(4, 1, BoulderPushDirection.FORWARD),
                BoulderPush(5, 1, BoulderPushDirection.RIGHT)
            )
        )
        expectedBoulders.add(
            arrayListOf(
                BoulderState.EMPTY,
                BoulderState.EMPTY,
                BoulderState.EMPTY,
                BoulderState.EMPTY,
                BoulderState.EMPTY,
                BoulderState.EMPTY,
                BoulderState.EMPTY,
                BoulderState.FILLED
            )
        )
        variantSteps.add(arrayListOf(BoulderPush(0, 1, BoulderPushDirection.FORWARD)))
    }

    override fun setup() {
        register(::onPuzzleDiscovered)
        register(::onRenderWorld)
        register(::onWorldChange)
    }
}
