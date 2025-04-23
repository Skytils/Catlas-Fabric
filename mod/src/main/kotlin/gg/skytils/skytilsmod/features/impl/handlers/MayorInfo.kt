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
package gg.skytils.skytilsmod.features.impl.handlers

import gg.skytils.event.EventPriority
import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.play.ChatMessageReceivedEvent
import gg.skytils.event.impl.screen.GuiContainerPostDrawSlotEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.client
import gg.skytils.skytilsmod.Skytils.json
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod.core.tickTimer
import gg.skytils.skytilsmod.utils.ItemUtil
import gg.skytils.skytilsmod.utils.TabListUtils
import gg.skytils.skytilsmod.utils.Utils
import gg.skytils.skytilsmod.utils.stripControlCodes
import gg.skytils.skytilsws.client.WSClient
import gg.skytils.skytilsws.shared.packet.C2SPacketJerryVote
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import net.minecraft.screen.GenericContainerScreenHandler
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

object MayorInfo : EventSubscriber {

    val mayorData = HashSet<Mayor>()

    var currentMayor: String? = null
    var mayorPerks = HashSet<String>()
    var currentMinister: String? = null
    var ministerPerk: String? = null
    var allPerks = HashSet<String>()

    var jerryMayor: Mayor? = null
    var newJerryPerks = 0L
    private var lastCheckedElectionOver = 0L
    private var lastFetchedMayorData = 0L

    private val jerryNextPerkRegex = Regex("§7Next set of perks in §e(?<h>\\d+?)h (?<m>\\d+?)m")

    init {
        tickTimer(60 * 20, repeats = true) {
            if (!Utils.inSkyblock || mc.currentServerEntry?.address?.lowercase()
                    ?.contains("alpha") == true
            ) return@tickTimer
            if (currentMayor == "Jerry" && System.currentTimeMillis() > newJerryPerks) {
                jerryMayor = null
                fetchJerryData()
            }
            if (System.currentTimeMillis() - lastFetchedMayorData > 24 * 60 * 60 * 1000) {
                fetchMayorData()
            }
            if (System.currentTimeMillis() - lastCheckedElectionOver > 60 * 60 * 1000) {
                val elected = TabListUtils.tabEntries.find {
                    it.second.startsWith("§r §r§fWinner: §r§a")
                }.run { this?.second?.substring(19, this.second.length - 2) } ?: currentMayor
                if (currentMayor != elected) {
                    fetchMayorData()
                }
                lastCheckedElectionOver = System.currentTimeMillis()
            }
        }
    }

    override fun setup() {
        register(::onDrawSlot)
        register(::onChat, EventPriority.Highest)
    }

    fun onChat(event: ChatMessageReceivedEvent) {
        if (!Utils.inSkyblock) return
        if (event.message.string == "§eEverybody unlocks §6exclusive §eperks! §a§l[HOVER TO VIEW]") {
            fetchMayorData()
        }
    }

    fun onDrawSlot(event: GuiContainerPostDrawSlotEvent) {
        if (!Utils.inSkyblock) return
        if (mc.currentServerEntry?.address?.lowercase()
                ?.contains("alpha") == true
        ) return
        if (event.container is GenericContainerScreenHandler) {
            val chestName = event.chestName
            if (event.slot.hasStack() && ((chestName == "Mayor Jerry" && (event.slot.id == 13 || event.slot.stack?.name == "§dJERRY IS MAYOR!!!")) || (chestName == "Calendar and Events" && event.slot.id == 37))) {
                val lore = ItemUtil.getItemLore(event.slot.stack)
                if (!lore.contains("§9Perkpocalypse Perks:")) return
                val endingIn = lore.asReversed().find { it.startsWith("§7Next set of perks in") } ?: return
                val perks =
                    lore.subList(lore.indexOf("§9Perkpocalypse Perks:"), lore.size - 1).filter { it.startsWith("§b") }
                        .map { it.stripControlCodes() }.ifEmpty { return }
                val mayor = mayorData.find {
                    it.perks.any { p ->
                        perks.contains(p.name)
                    }
                } ?: return
                val matcher = jerryNextPerkRegex.find(endingIn) ?: return
                val timeLeft =
                    matcher.groups["h"]!!.value.toInt().hours + matcher.groups["m"]!!.value.toInt().minutes
                val nextPerksNoRound = System.currentTimeMillis() + timeLeft.inWholeMilliseconds
                val nextPerks = (nextPerksNoRound / 300000.0).roundToLong() * 300000L
                if (jerryMayor != mayor || abs(nextPerks - newJerryPerks) > 60000) {
                    println("Jerry has ${mayor.name}'s perks ($perks) and is ending in $nextPerks ($${endingIn.stripControlCodes()})")
                    sendJerryData(mayor, nextPerks)
                }
                newJerryPerks = nextPerks
                jerryMayor = mayor
            }
        }
    }

    fun fetchMayorData() = Skytils.IO.launch {
        val res = client.get("https://api.hypixel.net/resources/skyblock/election").body<JsonObject>()
        val mayorObj = res["mayor"] as JsonObject
        val newMayor = json.decodeFromJsonElement<Mayor>(mayorObj)
        val newMinister = mayorObj["minister"]?.let { json.decodeFromJsonElement<Minister>(it) }
        tickTimer(1) {
            currentMayor = newMayor.name
            currentMinister = newMinister?.name
            lastFetchedMayorData = System.currentTimeMillis()
            if (currentMayor != "Jerry") jerryMayor = null
            mayorPerks.clear()
            mayorPerks.addAll(newMayor.perks.map { it.name })
            allPerks.clear()
            allPerks.addAll(mayorPerks)
            newMinister?.perk?.name?.let {
                ministerPerk = it
                allPerks.add(it)
            }
        }
    }

    fun fetchJerryData() = {} // no-op

    fun sendJerryData(mayor: Mayor?, nextSwitch: Long) = Skytils.IO.launch {
        if (mayor == null || nextSwitch <= System.currentTimeMillis()) return@launch
        if (!Skytils.trustClientTime) {
            println("Client's time isn't trusted, skip sending jerry data.")
            return@launch
        }
        WSClient.sendPacket(C2SPacketJerryVote(mayor.name, nextSwitch, System.currentTimeMillis()))
    }
}

@Serializable
class Mayor(val name: String, val perks: List<MayorPerk>)

@Serializable
class MayorPerk(val name: String, val description: String)

@Serializable
class Minister(val name: String, val perk: MayorPerk)

@Serializable
data class JerrySession(
    val nextSwitch: Long,
    val mayor: Mayor,
    val perks: List<MayorPerk>
)
