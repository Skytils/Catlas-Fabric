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

import gg.skytils.event.EventSubscriber
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod._event.PacketSendEvent
import gg.skytils.skytilsmod.core.SoundQueue
import gg.skytils.skytilsmod.utils.ItemUtil.getSkyBlockItemID
import gg.skytils.skytilsmod.utils.Utils
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket

object LockOrb : EventSubscriber {
    private val orbTimeRegex = Regex("§e(?<seconds>\\d+)s§r")

    override fun setup() {
        register(::onPacket)
    }

    fun onPacket(event: PacketSendEvent<*>) {
        if (!Utils.inSkyblock || !Skytils.config.powerOrbLock) return
        if (event.packet !is PlayerInteractBlockC2SPacket) return
        val itemId = getSkyBlockItemID(mc.player.method_0_7087()) ?: return
        if (!itemId.endsWith("_POWER_ORB")) return
        val heldOrb = PowerOrbs.getPowerOrbMatchingItemId(itemId) ?: return
        val orbs = mc.world.entities.filterIsInstance<ArmorStandEntity>().mapNotNull {
            val name = it.displayName.method_10865()
            val orb = PowerOrbs.getPowerOrbMatchingName(name) ?: return@mapNotNull null
            Triple(it, orb, name)
        }
        for ((orbEntity, orb, name) in orbs) {
            if (orb.ordinal >= heldOrb.ordinal) {
                val remainingTime = orbTimeRegex.find(name)?.groupValues?.get(1)?.toInt() ?: continue
                if (remainingTime >= Skytils.config.powerOrbDuration) {
                    if (orbEntity.squaredDistanceTo(mc.player) <= (orb.radius * orb.radius)) {
                        SoundQueue.addToQueue("random.orb", 0.8f, 1f)
                        event.cancelled = true
                    }
                }
            }
        }
    }

    private enum class PowerOrbs(var orbName: String, var radius: Double, var itemId: String) {
        RADIANT("§aRadiant", 18.0, "RADIANT_POWER_ORB"),
        MANAFLUX("§9Mana Flux", 18.0, "MANA_FLUX_POWER_ORB"),
        OVERFLUX("§5Overflux", 18.0, "OVERFLUX_POWER_ORB"),
        PLASMAFLUX("§d§lPlasmaflux", 20.0, "PLASMAFLUX_POWER_ORB");

        companion object {
            fun getPowerOrbMatchingName(name: String): PowerOrbs? = entries.find { name.startsWith(it.orbName) }

            fun getPowerOrbMatchingItemId(itemId: String): PowerOrbs? = entries.find { it.itemId == itemId }
        }
    }
}