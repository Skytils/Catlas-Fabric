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
import gg.skytils.event.impl.play.EntityInteractEvent
import gg.skytils.event.impl.play.WorldUnloadEvent
import gg.skytils.event.impl.render.WorldDrawEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod._event.MainThreadPacketReceiveEvent
import gg.skytils.skytilsmod.core.tickTimer
import gg.skytils.skytilsmod.utils.*
import net.minecraft.entity.decoration.ItemFrameEntity
import net.minecraft.block.Blocks
import net.minecraft.item.Items
import net.minecraft.item.Item
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import java.awt.Color
import java.awt.Point
import java.util.*
import kotlin.collections.ArrayDeque
import kotlin.random.Random

object AlignmentTaskSolver : EventSubscriber {
    // the blocks are on the west side, frames block pos is 1 block higher
    private val topLeft = BlockPos(-2, 124, 79).up()
    private val bottomRight = BlockPos(-2, 120, 75).up()
    private val box = BlockPos.iterate(topLeft, bottomRight).toList().sortedWith { a, b ->
        if (a.y == b.y) {
            return@sortedWith b.z - a.z
        }
        if (a.y < b.y) return@sortedWith 1
        if (a.y > b.y) return@sortedWith -1
        return@sortedWith 0
    }

    private val grid = LinkedHashSet<MazeSpace>()
    private val directionSet = HashMap<Point, Int>()
    private val clicks = HashMap<BlockPos, Int>()
    private val pendingClicks = HashMap<BlockPos, Int>()

    init {
        tickTimer(20, repeats = true) {
            computeLayout()
            computeTurns()
        }
    }

    fun isSolverActive() =
        Skytils.config.alignmentTerminalSolver && Utils.inDungeons && mc.player != null && TerminalFeatures.isInPhase3()

    fun computeLayout() {
        if (!isSolverActive()) return
        val (x, y, z) = mc.player ?: return
        if (topLeft.getSquaredDistanceFromCenter(x, y, z) <= 25 * 25) {
            if (grid.size < 25) {
                @Suppress("UNCHECKED_CAST")
                val frames = mc.world?.entities?.filter {
                    it is ItemFrameEntity &&
                            box.contains(it.blockPos) &&
                            it.heldItemStack != null &&
                            Utils.equalsOneOf(
                                it.heldItemStack.item,
                                Items.ARROW,
                                //#if MC<11300
                                //$$ Item.fromBlock(Blocks.WOOL)
                                //#else
                                Items.RED_WOOL,
                                Items.LIME_WOOL
                                //#endif
                            )
                } as List<ItemFrameEntity> ?: return
                if (frames.isNotEmpty()) {
                    for ((i, pos) in box.withIndex()) {
                        val coords = Point(i % 5, i / 5)
                        val frame = frames.find { it.blockPos == pos }
                        val type = frame?.heldItemStack?.let {
                            when (it.item) {
                                Items.ARROW -> SpaceType.PATH
                                //#if MC<11300
                                //$$ Item.byRawId(35) -> when (it.damage) {
                                //$$     5 -> SpaceType.STARTER
                                //$$     14 -> SpaceType.END
                                //$$     else -> SpaceType.PATH
                                //$$ }
                                //#else
                                Items.RED_WOOL -> SpaceType.END
                                Items.LIME_WOOL -> SpaceType.STARTER
                                //#endif
                                else -> SpaceType.EMPTY
                            }
                        } ?: SpaceType.EMPTY
                        grid.add(MazeSpace(frame?.decorationBlockPos, type, coords))
                    }
                }
            } else if (directionSet.isEmpty()) {
                val startPositions = grid.filter { it.type == SpaceType.STARTER }
                val endPositions = grid.filter { it.type == SpaceType.END }
                val layout = layout

                for (start in startPositions) {
                    for (end in endPositions) {
                        val pointMap = solve(layout, start.coords, end.coords)
                        for (move in convertPointMapToMoves(pointMap)) {
                            directionSet[move.point] = move.directionNum
                        }
                    }
                }
            }
        }
    }

    fun computeTurns() {
        if (mc.world == null || directionSet.isEmpty()) return
        for (space in grid) {
            if (space.type != SpaceType.PATH || space.framePos == null) continue
            val frame =
                (mc.world?.entities?.find { it is ItemFrameEntity && it.decorationBlockPos == space.framePos }
                    ?: continue) as ItemFrameEntity
            val neededClicks = getTurnsNeeded(frame.rotation, directionSet.getOrElse(space.coords) { 0 })
            clicks[space.framePos] = neededClicks
        }
    }

    fun onPacketSend(event: EntityInteractEvent) {
        if (directionSet.isNotEmpty() && Skytils.config.alignmentTerminalSolver && isSolverActive()) {
            if ((Skytils.config.blockIncorrectTerminalClicks || Skytils.config.predictAlignmentClicks)) {
                val entity = event.entity
                if (entity !is ItemFrameEntity) return
                val pos = entity.decorationBlockPos
                val clicks = clicks[pos]
                val pending = pendingClicks[pos] ?: 0

                if (Skytils.config.blockIncorrectTerminalClicks) {
                    if (clicks != null && clicks == pending) {
                        printDevMessage({ "Click packet on $pos was cancelled, rot: ${entity.rotation}, clicks: ${clicks}, pending: $pending" }, "predictalignment")
                        event.cancelled = true
                    } else {
                        val blockBehind = mc.world?.getBlockState(pos.method_10093(entity.horizontalFacing.opposite))
                        if (blockBehind?.block == Blocks.SEA_LANTERN) {
                            printDevMessage({ "Click packet on $pos was cancelled, reason: lantern" }, "predictalignment")
                            event.cancelled = true
                        }
                        printDevMessage({ "Allowed click on ${pos}, rot: ${entity.rotation}, clicks ${clicks}, pending $pending" }, "predictalignment")
                    }
                }

                if (!event.cancelled && Skytils.config.predictAlignmentClicks) {
                    pendingClicks[pos] = pending + 1
                    printDevMessage({ "Pending clicks on $pos: ${pendingClicks[pos]}, rot: ${entity.rotation}" }, "predictalignment")
                }
            }
        }
    }

    fun onPacketReceive(event: MainThreadPacketReceiveEvent<*>) {
        if (directionSet.isNotEmpty() && Skytils.config.alignmentTerminalSolver && isSolverActive()) {
            if (Skytils.config.predictAlignmentClicks && event.packet is EntityTrackerUpdateS2CPacket) {
                val entity = mc.world?.getEntityById(event.packet.id()) as? ItemFrameEntity ?: return
                val pos = entity.decorationBlockPos
                val pending = pendingClicks[pos]
                if (pending != null) {
                    val newRot = (event.packet.trackedValues.find { it.id() == 9 }?.value() as? Byte ?: return).toInt()
                    val currentRot = entity.rotation
                    val delta = getTurnsNeeded(currentRot, newRot)
                    val newPending = pending - delta
                    printDevMessage({ "$pos moved from $currentRot to $newRot, delta: $delta, current pending ${pending}, new pending $newPending" }, "predictalignment")
                    pendingClicks[pos] = newPending

                    val space = grid.find { it.framePos == pos } ?: return
                    val turns = getTurnsNeeded(newRot, directionSet.getOrElse(space.coords) { 0 })
                    val currentClicks = clicks[pos]
                    if (turns != currentClicks) {
                        printDevMessage({ "Synced clicks on $pos from $currentClicks to $turns" }, "predictalignment")
                        clicks[pos] = turns
                    }
                }
            }
        }
    }

    fun onRenderWorld(event: WorldDrawEvent) {
        if (!TerminalFeatures.isInPhase3()) return
        val matrixStack = UMatrixStack()
        for (space in grid) {
            if (space.type != SpaceType.PATH || space.framePos == null) continue
            val neededClicks = clicks[space.framePos] ?: continue
            if (neededClicks == 0) continue
            val pending = pendingClicks[space.framePos] ?: 0

            RenderUtil.drawLabel(
                space.framePos.middleVec(),
                if (pending > 0) "${neededClicks - pending} (${neededClicks})" else "$neededClicks",
                if (pending > 0 && neededClicks - pending == 0) Color.GREEN else Color.RED,
                event.partialTicks,
                matrixStack
            )
        }
    }

    fun getTurnsNeeded(current: Int, needed: Int): Int {
        return (needed - current + 8) % 8
    }

    fun onWorldLoad(event: WorldUnloadEvent) {
        grid.clear()
        directionSet.clear()
        pendingClicks.clear()
    }

    data class MazeSpace(val framePos: BlockPos? = null, val type: SpaceType, val coords: Point)

    enum class SpaceType {
        EMPTY,
        PATH,
        STARTER,
        END,
    }

    data class GridMove(val point: Point, val directionNum: Int)

    private fun convertPointMapToMoves(solution: List<Point>): List<GridMove> {
        if (solution.isEmpty()) return emptyList()
        return solution.asReversed().zipWithNext { current, next ->
            val diffX = current.x - next.x
            val diffY = current.y - next.y
            val dir = directions.find {
                it.vector.x == diffX && it.vector.z == diffY
            } ?: return@zipWithNext null

            val rotation = when (dir.opposite) {
                Direction.EAST -> 1
                Direction.WEST -> 5
                Direction.SOUTH -> 3
                Direction.NORTH -> 7
                else -> 0
            }
            return@zipWithNext GridMove(current, rotation)
        }.filterNotNull()
    }

    private val layout: Array<IntArray>
        get() {
            val grid = Array(5) { IntArray(5) }
            for (row in 0..4) {
                for (column in 0..4) {
                    val space = this.grid.find { it.coords == Point(row, column) }
                    grid[column][row] = if (space?.framePos != null) 0 else 1
                }
            }
            return grid
        }

    private val directions = Direction.entries.filter { it.vector.y == 0 }.reversed()

    /**
     * This code was modified into returning an ArrayList and was taken under CC BY-SA 4.0
     *
     * @link https://stackoverflow.com/a/55271133
     * @author ofekp
     */
    private fun solve(
        grid: Array<IntArray>,
        start: Point,
        end: Point
    ): ArrayList<Point> {
        val queue = ArrayDeque<Point>()
        val gridCopy = Array(
            grid.size
        ) { arrayOfNulls<Point>(grid[0].size) }
        queue.addLast(start)
        gridCopy[start.y][start.x] = start
        while (queue.isNotEmpty()) {
            val currPos = queue.removeFirst()
            // traverse adjacent nodes while sliding on the ice
            for (dir in directions) {
                val nextPos = move(grid, gridCopy, currPos, dir)
                if (nextPos != null) {
                    queue.addLast(nextPos)
                    gridCopy[nextPos.y][nextPos.x] = Point(
                        currPos.x, currPos.y
                    )
                    if (end == Point(nextPos.x, nextPos.y)) {
                        val steps = ArrayList<Point>()
                        // we found the end point
                        var tmp = currPos // if we start from nextPos we will count one too many edges
                        var count = 0
                        steps.add(nextPos)
                        steps.add(currPos)
                        while (tmp !== start) {
                            count++
                            tmp = gridCopy[tmp.y][tmp.x]!!
                            steps.add(tmp)
                        }
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
        grid: Array<IntArray>,
        gridCopy: Array<Array<Point?>>,
        currPos: Point,
        dir: Direction
    ): Point? {
        val x = currPos.x
        val y = currPos.y
        val diffX = dir.vector.x
        val diffY = dir.vector.z
        val i =
            if (x + diffX >= 0 && x + diffX < grid[0].size && y + diffY >= 0 && y + diffY < grid.size && grid[y + diffY][x + diffX] != 1) {
                1
            } else 0
        return if (gridCopy[y + i * diffY][x + i * diffX] != null) {
            // we've already seen this point
            null
        } else Point(x + i * diffX, y + i * diffY)
    }

    override fun setup() {
        register(::onPacketSend)
        register(::onPacketReceive)
        register(::onRenderWorld)
        register(::onWorldLoad)
    }
}
