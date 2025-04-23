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
package gg.skytils.skytilsmod.commands.impl

import gg.essential.universal.UChat
import gg.essential.universal.utils.MCClickEventAction
import gg.essential.universal.wrappers.message.UTextComponent
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.failPrefix
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod.Skytils.successPrefix
import gg.skytils.skytilsmod.commands.BaseCommand
import gg.skytils.skytilsmod.features.impl.handlers.ItemCycle
import gg.skytils.skytilsmod.features.impl.handlers.ItemCycle.getIdentifier
import gg.skytils.skytilsmod.gui.itemcycle.ItemCycleGui
import gg.skytils.skytilsmod.utils.SkyblockIsland
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.class_0_1374
import java.util.UUID

object ItemCycleCommand : BaseCommand("skytilsitemcycle", aliases = listOf("stic")) {
    override fun getCommandUsage(player: ClientPlayerEntity): String = "/stic"

    override fun processCommand(player: ClientPlayerEntity, args: Array<String>) {
        val subcommand = args.getOrNull(0)?.lowercase()
        when (subcommand) {
            "create" -> {
                val item = mc.player.method_0_7087()?.getIdentifier() ?: throw class_0_1374("You must be holding an item to create a cycle.")
                val id = UUID.randomUUID()
                val name = args.getOrNull(1) ?: id.toString()

                ItemCycle.cycles[id] = ItemCycle.Cycle(UUID.randomUUID(), name, hashSetOf(), item)

                UTextComponent("$successPrefix §fCreated a new no-op cycle with id $id")
                    .setClick(MCClickEventAction.SUGGEST_COMMAND, "/stic condition $id")
                    .chat()
            }
            "condition" -> {
                val id = args.getOrNull(1)?.let { UUID.fromString(it) } ?: throw class_0_1374("You must specify a cycle id.")
                val cycle = ItemCycle.cycles[id] ?: throw class_0_1374("No cycle with id $id found.")

                when (args.getOrNull(2)) {
                    "clear" -> {
                        cycle.conditions.clear()
                        UChat.chat("$successPrefix §fCleared all conditions for cycle with id $id")
                    }
                    "add" -> {
                        val negated = args.getOrNull(4)?.toBoolean() ?: false
                        when (args.getOrNull(3)) {
                            "island" -> {
                                val islands = args.getOrNull(5)?.split(",")?.mapNotNullTo(hashSetOf()) { SkyblockIsland.byMode[it] } ?: throw class_0_1374("You must specify an island mode.")
                                val cond = ItemCycle.Cycle.Condition.IslandCondition(islands, negated)
                                cycle.conditions.add(cond)
                                UTextComponent("$successPrefix §fAdded a new island condition with id ${cond.uuid} to cycle")
                                    .setClick(MCClickEventAction.SUGGEST_COMMAND, "/stic condition $id remove ${cond.uuid}")
                                    .chat()
                            }
                            "click" -> {
                                val button = args.getOrNull(5)?.toIntOrNull() ?: throw class_0_1374("You must specify a button id.")
                                val type = args.getOrNull(6)?.toIntOrNull() ?: throw class_0_1374("You must specify a click type.")
                                val cond = ItemCycle.Cycle.Condition.ClickCondition(button, type, negated)
                                cycle.conditions.add(cond)
                                UTextComponent("$successPrefix §fAdded a new click condition with id ${cond.uuid} to cycle")
                                    .setClick(MCClickEventAction.SUGGEST_COMMAND, "/stic condition $id remove ${cond.uuid}")
                                    .chat()
                            }
                            "item" -> {
                                val item = mc.player.method_0_7087()?.getIdentifier() ?: throw class_0_1374("You must be holding an item to create a cycle.")
                                val cond = ItemCycle.Cycle.Condition.ItemCondition(item, negated)
                                cycle.conditions.add(cond)
                                UTextComponent("$successPrefix §fAdded a new item condition with id ${cond.uuid} to cycle")
                                    .setClick(MCClickEventAction.SUGGEST_COMMAND, "/stic condition $id remove ${cond.uuid}")
                                    .chat()
                            }
                        }
                    }
                    "remove" -> {
                        val condId = args.getOrNull(3)?.let { UUID.fromString(it) } ?: throw class_0_1374("You must specify a condition id.")
                        if (cycle.conditions.removeAll { it.uuid == condId })
                            UChat.chat("$successPrefix §fRemoved condition with id $condId from cycle with id $id")
                        else
                            UChat.chat("$failPrefix §cNo condition with id $condId found in cycle with id $id")
                    }
                }
            }
            "target" -> {
                val id = args.getOrNull(1)?.let { UUID.fromString(it) } ?: throw class_0_1374("You must specify a cycle id.")
                val cycle = ItemCycle.cycles[id] ?: throw class_0_1374("No cycle with id $id found.")

                cycle.swapTo =  mc.player.method_0_7087()?.getIdentifier() ?: throw class_0_1374("You must be holding an item to bind a new target.")

                UTextComponent("$successPrefix §fBound the current held item to the cycle with id $id")
                    .setClick(MCClickEventAction.SUGGEST_COMMAND, "/stic condition $id")
                    .chat()
            }
            "delete" -> {
                val id = args.getOrNull(1)?.let { UUID.fromString(it) } ?: throw class_0_1374("You must specify a cycle id.")
                if (ItemCycle.cycles.remove(id) != null)
                    UChat.chat("$successPrefix §fRemoved cycle with id $id")
                else
                    UChat.chat("$failPrefix §cNo cycle with id $id found")
            }
            else -> {
                Skytils.displayScreen = ItemCycleGui()
            }
        }
    }
}