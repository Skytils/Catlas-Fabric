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

package gg.skytils.skytilsmod.features.impl.misc

import gg.essential.universal.UChat
import gg.essential.universal.wrappers.message.UMessage
import gg.skytils.event.EventSubscriber
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.client
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod._event.MainThreadPacketReceiveEvent
import gg.skytils.skytilsmod.core.tickTimer
import gg.skytils.skytilsmod.utils.ItemUtil
import gg.skytils.skytilsmod.utils.MojangUtil
import gg.skytils.skytilsmod.utils.Utils
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket
import java.util.*

object ScamCheck : EventSubscriber {
    // it caps out at 10 characters for otherParty
    private val tradingRegex = Regex("You {18}(?<otherParty>\\w{1,16})")
    private val tradingWithRegex = Regex("§7Trading with.*? (?<otherParty>\\w{1,16})§f§7\\.")
    private var tradingWindowId = -1
    private var scamChecked = false

    override fun setup() {
        register(::onPacket)
    }

    fun onPacket(event: MainThreadPacketReceiveEvent<*>) {
        if (!Utils.inSkyblock || !Skytils.config.scamCheck) return
        when (val packet = event.packet) {
            is OpenScreenS2CPacket -> {
                val windowTitle =
                    //#if MC<12000
                    //$$ packet.method_11435().method_10865()
                    //#else
                    packet.name.string
                    //#endif
                if (tradingRegex.matches(windowTitle)) {
                    tradingWindowId = packet.syncId
                    scamChecked = false
                }
            }

            is ScreenHandlerSlotUpdateS2CPacket -> {
                if (!scamChecked && packet.syncId == tradingWindowId && packet.slot == 41 && packet.stack != null) {
                    checkScam(packet.stack)
                    scamChecked = true
                }
            }

            is InventoryS2CPacket -> {
                if (!scamChecked && packet.syncId == tradingWindowId && packet.contents.size == 45) {
                    val tradingWith = packet.contents[41] ?: return
                    checkScam(tradingWith)
                    scamChecked = true
                }
            }
        }
    }

    private fun checkScam(tradingWith: ItemStack) {
        val firstLore = ItemUtil.getItemLore(tradingWith).find { it.matches(tradingWithRegex) } ?: return
        val otherParty = firstLore.replace(tradingWithRegex, "$1")
        val worldUUID = mc.world?.players?.find {
            it.uuid.version() == 4 && otherParty == it.name
            //#if MC>=11600
            .string
            //#endif
        }?.uuid
        Skytils.IO.launch {
            val uuid = worldUUID ?: runCatching { MojangUtil.getUUIDFromUsername(otherParty) }.getOrNull()
            ?: return@launch UChat.chat("${Skytils.failPrefix} §cUnable to get the UUID for ${otherParty}! Could they be nicked?")
            val result = checkScammer(uuid, "tradewindow")
            if (result.isScammer) {
                tickTimer(1) {
                    mc.player?.closeHandledScreen()
                }
            }
            result.printResult(otherParty)
        }.invokeOnCompletion {
            if (it != null) UChat.chat("${Skytils.failPrefix} §cSomething went wrong while checking the scammer status for ${otherParty}!")
        }
    }


    suspend fun checkScammer(uuid: UUID, source: String = "unknown") = withContext(Skytils.IO.coroutineContext) {
        client.get("https://${Skytils.domain}/api/scams/check?uuid=$uuid&utm_source=${source}")
            .body<ScamCheckResponse>()
    }
}

@Serializable
data class ScamCheckResponse(
    val isScammer: Boolean,
    val reasons: Map<String, String>,
    val isAlt: Boolean
) {
    fun printResult(username: String) {
        if (!isScammer) {
            UChat.chat("${Skytils.prefix} §a$username is not a known scammer!")
        } else {
            UMessage("${Skytils.prefix} §c$username is a known scammer!\n",
                "§bDatabases:\n",
                reasons.entries.joinToString("\n") { (db, reason) ->
                    "§b${db}: $reason"
                }
            ).chat()
        }
    }
}