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

package gg.skytils.skytilsmod.commands

import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.class_0_1581
import net.minecraft.server.command.CommandOutput

abstract class BaseCommand(private val name: String, private val aliases: List<String> = emptyList()) : class_0_1581() {
    final override fun method_0_5966(): String = name
    final override fun method_0_5964(): List<String> = aliases
    final override fun method_0_5757() = 0

    open fun getCommandUsage(player: ClientPlayerEntity): String = "/$commandName"

    abstract fun processCommand(player: ClientPlayerEntity, args: Array<String>)

    final override fun method_0_5962(sender: CommandOutput, args: Array<String>) =
        processCommand(sender as ClientPlayerEntity, args)

    final override fun method_0_5967(sender: CommandOutput) =
        getCommandUsage(sender as ClientPlayerEntity)
}