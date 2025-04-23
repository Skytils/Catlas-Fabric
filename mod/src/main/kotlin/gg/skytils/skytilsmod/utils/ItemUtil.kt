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
package gg.skytils.skytilsmod.utils

import net.minecraft.component.DataComponentTypes
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement

object ItemUtil {

    /**
     * Returns the Skyblock Item ID of a given Skyblock item
     *
     * @author BiscuitDevelopment
     * @param item the Skyblock item to check
     * @return the Skyblock Item ID of this item or `null` if this isn't a valid Skyblock item
     */
    @JvmStatic
    fun getSkyBlockItemID(item: ItemStack?): String? {
        if (item == null) {
            return null
        }
        val extraAttributes = getExtraAttributes(item) ?: return null

        return if (!extraAttributes.getType("id") == NbtElement.STRING_TYPE) {
            null
        } else extraAttributes.getString("id")
    }

    /**
     * Returns the `ExtraAttributes` compound tag from the item's NBT data.
     *
     * @author BiscuitDevelopment
     * @param item the item to get the tag from
     * @return the item's `ExtraAttributes` compound tag or `null` if the item doesn't have one
     */
    @JvmStatic
    fun getExtraAttributes(item: ItemStack?): NbtCompound? {
        return item?.get(DataComponentTypes.CUSTOM_DATA)?.nbt?.getCompound("ExtraAttributes")
    }

    /**
     * Returns the Skyblock Item ID of a given Skyblock Extra Attributes NBT Compound
     *
     * @author BiscuitDevelopment
     * @param extraAttributes the NBT to check
     * @return the Skyblock Item ID of this item or `null` if this isn't a valid Skyblock NBT
     */
    @JvmStatic
    fun getSkyBlockItemID(extraAttributes: NbtCompound?): String? {
        if (extraAttributes != null) {
            val itemId = extraAttributes.getString("id")
            if (itemId.isNotEmpty()) {
                return itemId
            }
        }
        return null
    }

    /**
     * Returns a string list containing the nbt lore of an ItemStack, or
     * an empty list if this item doesn't have a lore. The returned lore
     * list is unmodifiable since it has been converted from an NBTTagList.
     *
     * @author BiscuitDevelopment
     * @param itemStack the ItemStack to get the lore from
     * @return the lore of an ItemStack as a string list
     */
    @JvmStatic
    fun getItemLore(itemStack: ItemStack): List<String> {
        return itemStack.get(DataComponentTypes.LORE)?.styledLines?.map { it.formattedText } ?: emptyList()
    }
}