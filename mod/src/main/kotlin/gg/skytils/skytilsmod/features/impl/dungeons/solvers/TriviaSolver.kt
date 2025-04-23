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
import gg.skytils.event.EventPriority
import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.play.ChatMessageReceivedEvent
import gg.skytils.event.impl.play.WorldUnloadEvent
import gg.skytils.event.impl.render.LivingEntityPreRenderEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.failPrefix
import gg.skytils.skytilsmod._event.DungeonPuzzleResetEvent
import gg.skytils.skytilsmod.core.DataFetcher
import gg.skytils.skytilsmod.features.impl.dungeons.DungeonTimer
import gg.skytils.skytilsmod.listeners.DungeonListener
import gg.skytils.skytilsmod.utils.*
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.text.LiteralTextContent
import kotlin.math.floor

object TriviaSolver : EventSubscriber {
    private val questionStartRegex = Regex("§r§f {32}§r§6§lQuestion #\\d§r")
    private val answerRegex = Regex("§r§6 (?<type>[ⓐⓑⓒ]) §a(?<answer>[\\w ]+)§r")

    val triviaSolutions = hashMapOf<String, List<String>>()

    private var lines = mutableListOf<String>()
    private var trackLines = false
    private var fullQuestion: String? = null
    private var correctAnswers = mutableListOf<String>()
    private var correctAnswer: String? = null

    override fun setup() {
        register(::onChat, EventPriority.Highest)
        register(::onRenderArmorStandPre)
        register(::onWorldChange)
        register(::onPuzzleReset)
    }

    fun onChat(event: ChatMessageReceivedEvent) {
        if (!Skytils.config.triviaSolver || !Utils.inDungeons || !DungeonListener.incompletePuzzles.contains("Quiz")) return
        val formatted = event.message.method_10865()

        if (formatted == "§r§4[STATUE] Oruo the Omniscient§r§f: §r§fI am §r§4Oruo the Omniscient§r§f. I have lived many lives. I have learned all there is to know.§r" && triviaSolutions.size == 0) {
            UChat.chat("$failPrefix §cSkytils failed to load solutions for Quiz.")
            DataFetcher.reloadData()
        }

        if (questionStartRegex.matches(formatted)) {
            reset(trackLines = true)
        }

        if (trackLines) {
            lines.add(formatted)

            answerRegex.find(formatted)?.destructured?.let { (type, answer) ->
                if (type == "ⓐ") {
                    fullQuestion = lines.subList(1, lines.size - 2).joinToString(" ") { it.stripControlCodes().trim() }

                    if (fullQuestion == "What SkyBlock year is it?") {
                        val currentTime =
                            (if (DungeonTimer.dungeonStartTime > 0L) DungeonTimer.dungeonStartTime else System.currentTimeMillis()) / 1000.0
                        val diff = floor(currentTime - 1560276000)
                        val year = (diff / 446400 + 1).toInt()
                        correctAnswers.add("Year $year")
                    } else {
                        triviaSolutions.entries.find {
                            fullQuestion == it.key
                        }?.let {
                            correctAnswers.addAll(it.value)
                        }
                    }
                }

                if (!correctAnswers.any { it == answer }) {
                    event.message = LiteralTextContent(formatted.replace("§a", "§c"))
                } else correctAnswer = answer

                if (type == "ⓒ") {
                    trackLines = false
                }
            }
        }
    }

    fun onRenderArmorStandPre(event: LivingEntityPreRenderEvent<*>) {
        val answer = correctAnswer ?: return
        if (!Skytils.config.triviaSolver || !DungeonListener.incompletePuzzles.contains("Quiz") || event.entity !is ArmorStandEntity) return

        val name = event.entity.customName

        if (name.isNotEmpty() && name.containsAny("ⓐ", "ⓑ", "ⓒ") && !name.contains(answer)) {
            event.cancelled = true
        }
    }

    fun onWorldChange(event: WorldUnloadEvent) {
        reset()
    }

    fun onPuzzleReset(event: DungeonPuzzleResetEvent) {
        if (event.puzzle == "Quiz") {
            reset()
        }
    }

    private fun reset(trackLines: Boolean = false) {
        lines.clear()
        this.trackLines = trackLines
        fullQuestion = null
        correctAnswers.clear()
    }
}