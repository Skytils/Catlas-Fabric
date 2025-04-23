/*
 * Skytils - Hypixel Skyblock Quality of Life Mod
 * Copyright (C) 2020-2024 Skytils
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

package gg.skytils.skytilsmod.features.impl.crimson

import gg.essential.universal.ChatColor
import gg.essential.universal.UKeyboard
import gg.skytils.event.EventPriority
import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.play.WorldUnloadEvent
import gg.skytils.event.impl.render.CheckRenderEntityEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod._event.PacketReceiveEvent
import gg.skytils.skytilsmod.core.tickTimer
import gg.skytils.skytilsmod.utils.*
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket

object KuudraFeatures : EventSubscriber {
    var kuudraOver = false
    var myFaction: CrimsonFaction? = null
    private val factionRegex = Regex("§r§.§l(?<faction>\\w+) Reputation:§r")

    init {
        tickTimer(20, repeats = true) {
            if (Utils.inSkyblock && SBInfo.mode == SkyblockIsland.CrimsonIsle.mode) {
                TabListUtils.tabEntries.filter { it.second.endsWith(" Reputation:§r") }.forEach {
                    val faction = factionRegex.find(it.second)?.groupValues?.get(1) ?: return@forEach
                    myFaction = CrimsonFaction.entries.find { it.displayName == faction }
                }
            }
        }
    }

    override fun setup() {
        register(::onWorldLoad)
        register(::onCheckRender)
        register(::onPacket, EventPriority.Highest)
    }

    fun onWorldLoad(event: WorldUnloadEvent) {
        kuudraOver = false
    }

    fun onCheckRender(event: CheckRenderEntityEvent<*>) {
        if (event.entity !is ArmorStandEntity || SBInfo.mode != SkyblockIsland.KuudraHollow.mode) return
        if (Skytils.config.kuudraHideNonNametags && !kuudraOver && !UKeyboard.isKeyDown(UKeyboard.KEY_LMENU)) {
            if (event.entity.isInvisible && !event.entity.isCustomNameVisible) {
                event.cancelled = true
            }
        }
    }

    fun onPacket(event: PacketReceiveEvent<*>) {
        if (SBInfo.mode != SkyblockIsland.KuudraHollow.mode) return
        if (event.packet is GameMessageS2CPacket) {
            if (event.packet.message.string.stripControlCodes().trim() == "KUUDRA DOWN!") {
                kuudraOver = true
            }
        }
    }
 }

enum class CrimsonFaction(val color: ChatColor, val keyMaterial: String) {
    BARBARIAN(ChatColor.RED, "ENCHANTED_RED_SAND"),
    MAGE(ChatColor.DARK_PURPLE, "ENCHANTED_MYCELIUM");

    val displayName = name.toTitleCase()
    val identifier = name.first().uppercase()
}