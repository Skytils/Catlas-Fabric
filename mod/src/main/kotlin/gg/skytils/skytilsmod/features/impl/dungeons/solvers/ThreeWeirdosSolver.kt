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
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.failPrefix
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod.Skytils.prefix
import gg.skytils.skytilsmod.core.DataFetcher
import gg.skytils.skytilsmod.listeners.DungeonListener
import gg.skytils.skytilsmod.utils.Utils
import gg.skytils.skytilsmod.utils.multiplatform.UDirection
import gg.skytils.skytilsmod.utils.stripControlCodes
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.block.Blocks
import net.minecraft.util.math.BlockPos

object ThreeWeirdosSolver : EventSubscriber {
    val solutions = hashSetOf<String>()

    var riddleNPC: String? = null

    @JvmField
    var riddleChest: BlockPos? = null

    override fun setup() {
        register(::onChat, EventPriority.Highest)
        register(::onWorldChange)
    }
    fun onChat(event: ChatMessageReceivedEvent) {
        if (!Skytils.config.threeWeirdosSolver || !Utils.inDungeons || !DungeonListener.incompletePuzzles.contains("Three Weirdos")) return
        val formatted = event.message.method_10865()
        if (formatted.startsWith("§a§lPUZZLE SOLVED!") && "wasn't fooled by " in formatted) {
            riddleNPC = null
            riddleChest = null
        }
        if (formatted.startsWith("§e[NPC] ")) {
            if (solutions.size == 0) {
                UChat.chat("$failPrefix §cSkytils failed to load solutions for Three Weirdos.")
                DataFetcher.reloadData()
            }

            if (solutions.any {
                    formatted.contains(it)
                }) {
                val npcName = formatted.substringAfter("§c").substringBefore("§f")
                riddleNPC = npcName
                UChat.chat("$prefix §a§l${npcName.stripControlCodes()} §2has the blessing.")

                mc.world?.entities?.find {
                    it is ArmorStandEntity && riddleNPC!! in it.customName
                }?.let {
                    riddleChest = UDirection.HORIZONTALS.map { dir -> it.blockPos.method_10093(dir)  }.find {
                        mc.world?.getBlockState(it)?.block == Blocks.field_0_680
                    }
                    println("Riddle NPC ${it.customName} @ ${it.blockPos} w/ chest @ $riddleChest")
                }
            }
        }
    }

    fun onWorldChange(event: WorldUnloadEvent) {
        riddleNPC = null
        riddleChest = null
    }
}