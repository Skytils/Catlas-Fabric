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

import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.play.WorldUnloadEvent
import gg.skytils.event.impl.render.WorldDrawEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod._event.DungeonPuzzleDiscoveredEvent
import gg.skytils.skytilsmod._event.DungeonPuzzleResetEvent
import gg.skytils.skytilsmod.core.tickTimer
import gg.skytils.skytilsmod.listeners.DungeonListener
import gg.skytils.skytilsmod.utils.RenderUtil
import gg.skytils.skytilsmod.utils.SuperSecretSettings
import gg.skytils.skytilsmod.utils.Utils
import gg.skytils.skytilsmod.utils.tictactoe.AlphaBetaAdvanced
import gg.skytils.skytilsmod.utils.tictactoe.Board
import net.minecraft.entity.decoration.ItemFrameEntity
import net.minecraft.block.Blocks
import net.minecraft.item.Items
import net.minecraft.util.math.Box
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.item.map.MapState
import kotlin.experimental.and

object TicTacToeSolver : EventSubscriber {

    private const val MAP_COLOR_INDEX = 8256
    private const val COLOR_INT_X = 114
    private const val COLOR_INT_O = 33
    private var topLeft: BlockPos? = null
    private var roomFacing: Direction? = null
    private var board: Board? = null
    private var mappedPositions = HashMap<Int, ItemFrameEntity>()
    private var bestMove: BlockPos? = null

    init {
        tickTimer(20, repeats = true) {
            if (!Utils.inDungeons || !Skytils.config.ticTacToeSolver || mc.player == null) return@tickTimer
            if (SuperSecretSettings.azooPuzzoo || DungeonListener.incompletePuzzles.contains("Tic Tac Toe")) {
                updatePuzzleState()
            } else {
                bestMove = null
            }
        }
    }

    override fun setup() {
        register(::onPuzzleDiscovered)
        register(::onWorldLoad)
        register(::onPuzzleReset)
        register(::onRenderWorld)
    }

    fun onPuzzleDiscovered(event: DungeonPuzzleDiscoveredEvent) {
        if (event.puzzle == "Tic Tac Toe") {
            updatePuzzleState()
        }
    }

    @Throws(IllegalStateException::class)
    fun updatePuzzleState() {
        val frames = getBoardFrames()
        if (topLeft == null || roomFacing == null || board == null) {
            parseInitialState(frames)
        } else if (!board!!.isGameOver) {
            board!!.turn = if (frames.size % 2 == 0) Board.State.X else Board.State.O
            if (board!!.turn == Board.State.O) {
                for (frame in frames) {
                    if (frame !in mappedPositions.values) {
                        val (row, column) = getBoardPosition(frame)
                        board!!.place(column, row, getSpotOwner(frame))
                        mappedPositions[row * Board.BOARD_WIDTH + column] = frame
                    }
                }
                AlphaBetaAdvanced.run(board!!)
                val move = board!!.algorithmBestMove
                if (move != -1) {
                    val column = move % Board.BOARD_WIDTH
                    val row = move / Board.BOARD_WIDTH
                    bestMove = topLeft!!.down(row).offset(roomFacing!!.rotateYClockwise(), column)
                }
            } else {
                bestMove = null
            }
        } else {
            bestMove = null
        }
    }

    fun onWorldLoad(event: WorldUnloadEvent) {
        topLeft = null
        roomFacing = null
        board = null
        bestMove = null
        mappedPositions.clear()
    }

    fun onPuzzleReset(event: DungeonPuzzleResetEvent) {
        if (event.puzzle == "Tic Tac Toe") {
            board = null
            bestMove = null
            mappedPositions.clear()
        }
    }

    fun onRenderWorld(event: WorldDrawEvent) {
        if (!Utils.inDungeons || !Skytils.config.ticTacToeSolver) return
        if (bestMove != null) {
            RenderUtil.drawOutlinedBoundingBox(
                Box(bestMove, bestMove!!.method_10069(1, 1, 1)),
                Skytils.config.ticTacToeSolverColor,
                3f,
                event.partialTicks
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getBoardFrames(): List<ItemFrameEntity> = mc.world.entities.filter {
        it is ItemFrameEntity &&
                it.rotation == 0 &&
                it.blockPos.method_10074().let { realPos -> realPos.y in 70..72 } &&
                it.heldItemStack?.let { item ->
                    item.item == Items.FILLED_MAP &&
                            Items.FILLED_MAP.getMapState(item, mc.world)?.let { mapData ->
                                mapData[MAP_COLOR_INDEX].let { colorInt ->
                                    colorInt == COLOR_INT_X || colorInt == COLOR_INT_O
                                }
                            } ?: false
                } ?: false &&
                mc.world.getBlockState(it.blockPos.method_10074().offset(it.facing.opposite, 1)).block == Blocks.IRON_BLOCK
    } as List<ItemFrameEntity>

    private fun parseInitialState(frames: List<ItemFrameEntity>) {
        for (frame in frames) {
            val (row, column) = getBoardPosition(frame)
            if (board == null) {
                topLeft = frame.blockPos.up(row-1).offset(frame.facing.rotateYClockwise(), column)
                roomFacing = frame.facing.opposite
                board = Board()
            }
            board!!.place(column, row, getSpotOwner(frame))
            mappedPositions[row * Board.BOARD_WIDTH + column] = frame
        }
        if (board != null) {
            board!!.turn = if (frames.size % 2 == 0) Board.State.X else Board.State.O
        }
    }

    private operator fun MapState.get(index: Int): Int {
        return (this.colors[index] and 255.toByte()).toInt()
    }

    private fun getMapData(entity: ItemFrameEntity) = Items.FILLED_MAP.getMapState(entity.heldItemStack, mc.world)

    private fun getBoardPosition(frame: ItemFrameEntity): Pair<Int, Int> {
        val realPos = frame.blockPos.method_10074()
        val blockBehind = realPos.method_10093(frame.facing.opposite)

        val row = 72 - realPos.y
        val column = when {
            mc.world.getBlockState(blockBehind.method_10093(frame.facing.rotateYCounterclockwise())).block != Blocks.IRON_BLOCK -> 2
            mc.world.getBlockState(blockBehind.method_10093(frame.facing.rotateYClockwise())).block != Blocks.IRON_BLOCK -> 0
            else -> 1
        }

        return row to column
    }

    private fun getSpotOwner(frame: ItemFrameEntity): Board.State {
        val mapData = getMapData(frame) ?: error("Non map checked")
        return if (mapData[MAP_COLOR_INDEX] == COLOR_INT_X) Board.State.X else Board.State.O
    }
}