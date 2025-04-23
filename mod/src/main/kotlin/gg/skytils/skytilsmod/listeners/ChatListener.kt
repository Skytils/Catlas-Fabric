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
package gg.skytils.skytilsmod.listeners

import gg.essential.universal.UChat
import gg.skytils.event.EventPriority
import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.play.ChatMessageReceivedEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.failPrefix
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod.Skytils.prefix
import gg.skytils.skytilsmod.features.impl.funny.Funny
import gg.skytils.skytilsmod.features.impl.funny.skytilsplus.AdManager
import gg.skytils.skytilsmod.mixins.transformers.accessors.AccessorGuiNewChat
import gg.skytils.skytilsmod.utils.Utils
import gg.skytils.skytilsmod.utils.runClientCommand
import gg.skytils.skytilsmod.utils.stripControlCodes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object ChatListener : EventSubscriber {
    private var lastPartyDisbander = ""
    private val invitePattern = Regex("(?:(?:\\[.+?] )?(?:\\w+) invited )(?:\\[.+?] )?(\\w+)")
    private val playerPattern = Regex("(?:\\[.+?] )?(\\w+)")
    private val party_start_pattern = Regex("^Party Members \\((\\d+)\\)$")
    private val leader_pattern = Regex("^Party Leader: (?:\\[.+?] )?(\\w+) ●$")
    private val members_pattern = Regex(" (?:\\[.+?] )?(\\w+) ●")
    private var awaitingDelimiter = false

    fun onChat(event: ChatMessageReceivedEvent) {
        if (!Utils.isOnHypixel) return
        val formatted = event.message.method_10865()
        val unformatted = formatted.stripControlCodes()
        if (Skytils.config.autoReparty) {
            if (formatted.endsWith("§r§ehas disbanded the party!§r")) {
                playerPattern.find(unformatted)?.let {
                    lastPartyDisbander = it.groupValues[1]
                    println("Party disbanded by $lastPartyDisbander")
                    Skytils.launch {
                        if (Skytils.config.autoRepartyTimeout == 0) return@launch
                        try {
                            println("waiting for timeout")
                            delay(Skytils.config.autoRepartyTimeout * 1000L)
                            lastPartyDisbander = ""
                            println("cleared last party disbander")
                        } catch (_: Exception) {
                        }
                    }
                    return
                }
            }
            if (unformatted.contains("You have 60 seconds to accept") && lastPartyDisbander.isNotEmpty() && event.message.siblings.size >= 7) {
                val acceptMessage = event.message.siblings[6].style
                if (acceptMessage.hoverEvent.value.string.contains(lastPartyDisbander)) {
                    Skytils.sendMessageQueue.add("/p accept $lastPartyDisbander")
                    lastPartyDisbander = ""
                    return
                }
            }
        }

        // Await delimiter
        if (awaitingDelimiter && unformatted.startsWith("---")) {
            event.cancelled = true
            awaitingDelimiter = false
            return
        }

        // Reparty command
        // Getting party
        if (RepartyCommand.gettingParty) {
            if (unformatted.contains("-----")) {
                when (RepartyCommand.Delimiter) {
                    0 -> {
                        println("Get Party Delimiter Cancelled")
                        RepartyCommand.Delimiter++
                        event.cancelled = true
                        return
                    }

                    1 -> {
                        println("Done querying party")
                        RepartyCommand.gettingParty = false
                        RepartyCommand.Delimiter = 0
                        event.cancelled = true
                        return
                    }
                }
            } else if (unformatted.startsWith("Party M") || unformatted.startsWith("Party Leader")) {
                val player = mc.player
                val partyStart = party_start_pattern.find(unformatted)
                val leader = leader_pattern.find(unformatted)
                val members = members_pattern.findAll(unformatted)
                if (partyStart != null && partyStart.groupValues[1].toInt() == 1) {
                    UChat.chat("$failPrefix §cYou cannot reparty yourself.")
                    RepartyCommand.partyThread!!.interrupt()
                } else if (leader != null && leader.groupValues[1] != player.name) {
                    UChat.chat("$failPrefix §cYou are not party leader.")
                    RepartyCommand.partyThread!!.interrupt()
                } else {
                    members.forEach {
                        val partyMember = it.groupValues[1]
                        if (partyMember != player.name) {
                            RepartyCommand.party.add(partyMember)
                            println(partyMember)
                        }
                    }
                }
                event.cancelled = true
                return
            }
        }
        // Disbanding party
        if (RepartyCommand.disbanding) {
            if (unformatted.contains("-----")) {
                when (RepartyCommand.Delimiter) {
                    0 -> {
                        println("Disband Delimiter Cancelled")
                        RepartyCommand.Delimiter++
                        event.cancelled = true
                        return
                    }

                    1 -> {
                        println("Done disbanding")
                        RepartyCommand.disbanding = false
                        RepartyCommand.Delimiter = 0
                        event.cancelled = true
                        return
                    }
                }
            } else if (unformatted.endsWith("has disbanded the party!")) {
                event.cancelled = true
                return
            }
        }
        // Inviting
        if (RepartyCommand.inviting) {
            if (unformatted.endsWith(" to the party! They have 60 seconds to accept.")) {
                val invitee = invitePattern.find(unformatted)
                if (invitee != null) {
                    println("${invitee.groupValues[1]}: ${RepartyCommand.repartyFailList.remove(invitee.groupValues[1])}")
                }
                tryRemoveLineAtIndex(1)
                awaitingDelimiter = true
                event.cancelled = true
                println("Player Invited!")
                RepartyCommand.inviting = false
                return
            } else if (unformatted.contains("Couldn't find a player") || unformatted.contains("You cannot invite that player")) {
                tryRemoveLineAtIndex(1)
                event.cancelled = true
                println("Player Invited!")
                RepartyCommand.inviting = false
                return
            }
        }
        // Fail Inviting
        if (RepartyCommand.failInviting) {
            if (unformatted.endsWith(" to the party! They have 60 seconds to accept.")) {
                val invitee = invitePattern.find(unformatted)
                if (invitee != null) {
                    println("${invitee.groupValues[1]}: ${RepartyCommand.repartyFailList.remove(invitee.groupValues[1])}")
                }
                tryRemoveLineAtIndex(1)
                event.cancelled = true
                RepartyCommand.inviting = false
                return
            } else if (unformatted.contains("Couldn't find a player") || unformatted.contains("You cannot invite that player")) {
                tryRemoveLineAtIndex(1)
                event.cancelled = true
                println("Player Invited!")
                RepartyCommand.inviting = false
                return
            }
        }
        if (unformatted == "Welcome to Hypixel SkyBlock!") {
            if (Skytils.config.firstLaunch) {
                UChat.chat("$prefix §bThank you for downloading Skytils!")
                runClientCommand("/skytils help")
                Skytils.config.firstLaunch = false
                Skytils.config.markDirty()
                Skytils.config.writeData()
            }
            Funny.joinedSkyblock()
            AdManager.joinedSkyblock()
        }
    }

    private fun tryRemoveLineAtIndex(index: Int) {
        val lines =
            (mc.inGameHud.chatHud as AccessorGuiNewChat).drawnChatLines
        if (lines.size > index) {
            lines.removeAt(index)
        }
    }

    override fun setup() {
        register(::onChat, EventPriority.Highest)
    }
}
