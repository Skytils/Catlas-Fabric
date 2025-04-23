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
import gg.skytils.skytilsmod._event.DungeonPuzzleResetEvent
import gg.skytils.skytilsmod.core.tickTimer
import gg.skytils.skytilsmod.listeners.DungeonListener
import gg.skytils.skytilsmod.utils.RenderUtil
import gg.skytils.skytilsmod.utils.Utils
import gg.skytils.skytilsmod.utils.multiplatform.UDirection
import kotlinx.coroutines.launch
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.entity.mob.SilverfishEntity
import net.minecraft.block.Blocks
import net.minecraft.block.entity.ChestBlockEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import java.awt.Color
import java.awt.Point
import kotlin.math.abs

object IcePathSolver : EventSubscriber {
    private val steps: MutableList<Point> = ArrayList()
    private var silverfishChestPos: BlockPos? = null
    private var roomFacing: Direction? = null
    private var grid: Array<IntArray>? = null
    private var silverfish: SilverfishEntity? = null
    private var silverfishPos: Point? = null

    init {
        tickTimer(20, repeats = true) {
            if (!Utils.inDungeons || !Skytils.config.icePathSolver || mc.player == null || "Ice Path" !in DungeonListener.incompletePuzzles) return@tickTimer
            if (silverfishChestPos != null && roomFacing != null && silverfish != null) {
                if (grid == null) {
                    grid = getGridLayout()
                    silverfishPos = getGridPointFromPos(silverfish!!.blockPos)
                    steps.clear()
                    if (silverfishPos != null) {
                        steps.addAll(solve(grid!!, silverfishPos!!.x, silverfishPos!!.y, 9, 0))
                    }
                } else if (silverfish != null) {
                    val silverfishGridPos = getGridPointFromPos(silverfish!!.blockPos)
                    if (silverfish!!.isAlive && silverfishGridPos != silverfishPos) {
                        silverfishPos = silverfishGridPos
                        if (silverfishPos != null) {
                            steps.clear()
                            steps.addAll(solve(grid!!, silverfishPos!!.x, silverfishPos!!.y, 9, 0))
                        }
                    }
                }
            } else {
                mc.world.entities.find {
                    it is SilverfishEntity && !it.isInvisible && mc.player.squaredDistanceTo(it) < 20 * 20
                }?.let {
                    silverfish = it as SilverfishEntity
                    if (silverfishChestPos == null || roomFacing == null) {
                        Skytils.launch {
                            val playerX = mc.player.x.toInt()
                            val playerZ = mc.player.z.toInt()
                            val xRange = playerX - 25..playerX + 25
                            val zRange = playerZ - 25..playerZ + 25
                            findChest@ for (te in mc.world.field_0_262) {
                                if (te.pos.y == 67 && te is ChestBlockEntity && te.viewerCount == 0 && te.pos.x in xRange && te.pos.z in zRange
                                ) {
                                    val pos = te.pos
                                    if (mc.world.getBlockState(pos.method_10074()).block == Blocks.PACKED_ICE && mc.world.getBlockState(
                                            pos.up(2)
                                        ).block == Blocks.field_0_787
                                    ) {
                                        for (direction in UDirection.HORIZONTALS) {
                                            if (mc.world.getBlockState(pos.method_10093(direction)).block == Blocks.STONEBRICK) {
                                                silverfishChestPos = pos
                                                roomFacing = direction
                                                println(
                                                    "Silverfish chest is at $silverfishChestPos and is facing $roomFacing",
                                                )
                                                break@findChest
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun setup() {
        register(::onPuzzleReset)
        register(::onWorldRender)
        register(::onWorldChange)
    }

    fun onPuzzleReset(event: DungeonPuzzleResetEvent) {
        if (event.puzzle == "Ice Path") {
            steps.clear()
            silverfish = null
            silverfishPos = null
        }
    }

    fun onWorldRender(event: WorldDrawEvent) {
        if (!Skytils.config.icePathSolver) return
        if (silverfishChestPos != null && roomFacing != null && grid != null && silverfish?.isAlive == true) {
            RenderSystem.disableCull()

            val points = steps.map { getVec3RelativeToGrid(it.x, it.y)!!.add(0.5, 0.5, 0.5) }
            RenderUtil.draw3DLineStrip(points, 5, Color.RED, event.partialTicks, UMatrixStack.Compat.get())

            RenderSystem.enableCull()
        }
    }

    fun onWorldChange(event: WorldUnloadEvent) {
        silverfishChestPos = null
        roomFacing = null
        grid = null
        steps.clear()
        silverfish = null
        silverfishPos = null
    }

    private fun getVec3RelativeToGrid(column: Int, row: Int): Vec3d? {
        return silverfishChestPos?.let { chestPos ->
            roomFacing?.let { facing ->
                Vec3d(chestPos.offset(facing.opposite, 4)
                    .offset(facing.rotateYCounterclockwise(), 8)
                    .offset(facing.rotateYClockwise(), column)
                    .offset(facing.opposite, row))
            }
        }
    }

    private fun getGridPointFromPos(pos: BlockPos): Point? {
        if (silverfishChestPos == null || roomFacing == null) return null
        val topLeft = silverfishChestPos!!.offset(roomFacing!!.opposite, 4).offset(roomFacing!!.rotateYCounterclockwise(), 8)
        val diff = pos.method_10059(topLeft)

        return Point(abs(diff.getValueOnAxis(roomFacing!!.rotateYClockwise().axis)), abs(diff.getValueOnAxis(roomFacing!!.opposite.axis)))
    }

    private fun BlockPos.getValueOnAxis(axis: Direction.Axis): Int {
        return when (axis) {
            Direction.Axis.X -> this.x
            Direction.Axis.Y -> this.y
            Direction.Axis.Z -> this.z
        }
    }

    fun getGridLayout(): Array<IntArray>? {
        if (silverfishChestPos == null || roomFacing == null) return null
        val grid = Array(17) { IntArray(17) }
        for (row in 0..16) {
            for (column in 0..16) {
                grid[row][column] = if (mc.world.getBlockState(
                        BlockPos(
                            getVec3RelativeToGrid(
                                column,
                                row
                            )
                        )
                    ).block !== Blocks.AIR
                ) 1 else 0
            }
            if (row == 16) return grid
        }
        return null
    }

    /**
     * This code was modified into returning an ArrayList and was taken under CC BY-SA 4.0
     *
     * @link https://stackoverflow.com/a/55271133
     * @author ofekp
     */
    private fun solve(iceCave: Array<IntArray>, startX: Int, startY: Int, endX: Int, endY: Int): ArrayList<Point> {
        val startPoint = Point(startX, startY)
        val queue = ArrayDeque<Point>()
        val iceCaveColors = Array(
            iceCave.size
        ) { arrayOfNulls<Point>(iceCave[0].size) }
        queue.addLast(Point(startX, startY))
        iceCaveColors[startY][startX] = startPoint
        while (queue.isNotEmpty()) {
            val currPos = queue.removeFirst()
            // traverse adjacent nodes while sliding on the ice
            for (dir in UDirection.HORIZONTALS) {
                val nextPos = move(iceCave, iceCaveColors, currPos, dir)
                if (nextPos != null) {
                    queue.addLast(nextPos)
                    iceCaveColors[nextPos.y][nextPos.x] = Point(
                        currPos.x, currPos.y
                    )
                    if (nextPos.getY() == endY.toDouble() && nextPos.getX() == endX.toDouble()) {
                        val steps = ArrayList<Point>()
                        // we found the end point
                        var tmp = currPos // if we start from nextPos we will count one too many edges
                        var count = 0
                        steps.add(nextPos)
                        steps.add(currPos)
                        while (tmp !== startPoint) {
                            count++
                            tmp = iceCaveColors[tmp.y][tmp.x]!!
                            steps.add(tmp)
                        }
                        //System.out.println("Silverfish solved in " + count + " moves.");
                        return steps
                    }
                }
            }
        }
        return arrayListOf()
    }

    /**
     * This code was modified to fit Minecraft and was taken under CC BY-SA 4.0
     *
     * @link https://stackoverflow.com/a/55271133
     * @author ofekp
     */
    private fun move(
        iceCave: Array<IntArray>?,
        iceCaveColors: Array<Array<Point?>>,
        currPos: Point,
        dir: Direction
    ): Point? {
        val x = currPos.x
        val y = currPos.y
        val diffX = dir.vector.x
        val diffY = dir.vector.z
        var i = 1
        while (x + i * diffX >= 0 && x + i * diffX < iceCave!![0].size && y + i * diffY >= 0 && y + i * diffY < iceCave.size && iceCave[y + i * diffY][x + i * diffX] != 1) {
            i++
        }
        i-- // reverse the last step
        return if (iceCaveColors[y + i * diffY][x + i * diffX] != null) {
            // we've already seen this point
            null
        } else Point(x + i * diffX, y + i * diffY)
    }
}