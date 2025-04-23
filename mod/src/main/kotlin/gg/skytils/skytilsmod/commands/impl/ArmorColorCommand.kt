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
import gg.skytils.skytilsmod.Skytils.failPrefix
import gg.skytils.skytilsmod.Skytils.prefix
import gg.skytils.skytilsmod.Skytils.successPrefix
import gg.skytils.skytilsmod.commands.BaseCommand
import gg.skytils.skytilsmod.core.PersistentSave
import gg.skytils.skytilsmod.features.impl.handlers.ArmorColor
import gg.skytils.skytilsmod.utils.ItemUtil
import gg.skytils.skytilsmod.utils.Utils
import gg.skytils.skytilsmod.utils.graphics.colors.CustomColor
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.class_0_1371
import net.minecraft.class_0_1374
import net.minecraft.item.ArmorItem

object ArmorColorCommand : BaseCommand("armorcolor", listOf("armourcolour", "armorcolour", "armourcolor")) {
    override fun getCommandUsage(player: ClientPlayerEntity): String = "/armorcolor <clearall/clear/set>"

    override fun processCommand(player: ClientPlayerEntity, args: Array<String>) {
        if (args.isEmpty()) {
            UChat.chat("$prefix §b" + getCommandUsage(player))
            return
        }
        val subcommand = args[0].lowercase()
        if (subcommand == "clearall") {
            ArmorColor.armorColors.clear()
            PersistentSave.markDirty<ArmorColor>()
            UChat.chat("$successPrefix §aCleared all your custom armor colors!")
        } else if (subcommand == "clear" || subcommand == "set") {
            if (!Utils.inSkyblock) throw class_0_1374("You must be in Skyblock to use this command!")
            val item = player.method_0_7087()
                ?: throw class_0_1374("You must hold a leather armor piece to use this command")
            if ((item.item as? ArmorItem)?.material != ArmorItem.class_1740.LEATHER) throw class_0_1374("You must hold a leather armor piece to use this command")
            val extraAttributes = ItemUtil.getExtraAttributes(item)
            if (extraAttributes == null || !extraAttributes.contains("uuid")) throw class_0_1374("This item does not have a UUID!")
            val uuid = extraAttributes.getString("uuid")
            if (subcommand == "set") {
                if (args.size != 2) throw class_0_1374("You must specify a valid hex color!")
                val color: CustomColor = try {
                    Utils.customColorFromString(args[1])
                } catch (e: IllegalArgumentException) {
                    throw class_0_1371("$failPrefix §cUnable to get a color from inputted string.")
                }
                ArmorColor.armorColors[uuid] = color
                PersistentSave.markDirty<ArmorColor>()
                UChat.chat("$successPrefix §aSet the color of your ${item.name}§a to ${args[1]}!")
            } else {
                if (ArmorColor.armorColors.containsKey(uuid)) {
                    ArmorColor.armorColors.remove(uuid)
                    PersistentSave.markDirty<ArmorColor>()
                    UChat.chat("$successPrefix §aCleared the custom color for your ${item.name}§a!")
                } else UChat.chat("§cThat item doesn't have a custom color!")
            }
        } else UChat.chat(getCommandUsage(player))
    }
}