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

import gg.skytils.skytilsmod.utils.ItemRarity.Companion.RARITY_PATTERN
import net.minecraft.item.Items
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList
import net.minecraft.nbt.NbtString
import net.minecraftforge.common.util.Constants
import java.util.*
import kotlin.math.max

object ItemUtil {
    private val PET_PATTERN = "(?:§e⭐ )?§7\\[Lvl \\d+](?: §8\\[.*])? (?<color>§[0-9a-fk-or]).+".toRegex()
    const val NBT_INTEGER = 3
    private const val NBT_STRING = 8
    private const val NBT_LIST = 9
    private const val NBT_COMPOUND = 10

    /**
     * Returns the display name of a given item
     * @author Mojang
     * @param item the Item to get the display name of
     * @return the display name of the item
     */
    @JvmStatic
    fun getDisplayName(item: ItemStack): String {
        var s = item.item.getName(item)
        if (item.nbt != null && item.nbt.contains("display", 10)) {
            val nbtTagCompound = item.nbt.getCompound("display")
            if (nbtTagCompound.contains("Name", 8)) {
                s = nbtTagCompound.getString("Name")
            }
        }
        return s
    }

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
        return if (!extraAttributes.contains("id", NBT_STRING)) {
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
        return if (item == null || !item.hasNbt()) {
            null
        } else item.getOrCreateSubNbt("ExtraAttributes", false)
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
        if (itemStack.hasNbt() && itemStack.nbt.contains("display", NBT_COMPOUND)) {
            val display = itemStack.nbt.getCompound("display")
            if (display.contains("Lore", NBT_LIST)) {
                val lore = display.getList("Lore", NBT_STRING)
                val loreAsList = ArrayList<String>(lore.size())
                for (lineNumber in 0..<lore.size()) {
                    loreAsList.add(lore.getString(lineNumber))
                }
                return Collections.unmodifiableList(loreAsList)
            }
        }
        return emptyList()
    }

    @JvmStatic
    fun hasRightClickAbility(itemStack: ItemStack): Boolean {
        for (line in getItemLore(itemStack)) {
            val stripped = line.stripControlCodes()
            if (stripped.startsWith("Item Ability:") && stripped.endsWith("RIGHT CLICK")) return true
        }
        return false
    }

    /**
     * Returns the rarity of a given Skyblock item
     * Modified
     * @author BiscuitDevelopment
     * @param item the Skyblock item to check
     * @return the rarity of the item if a valid rarity is found, `null` if no rarity is found, `null` if item is `null`
     */
    fun getRarity(item: ItemStack?): ItemRarity {
        if (item == null || !item.hasNbt()) {
            return ItemRarity.NONE
        }
        val display = item.getOrCreateSubNbt("display", false)
        if (display == null || !display.contains("Lore")) {
            return ItemRarity.NONE
        }
        val lore = display.getList("Lore", Constants.NBT.TAG_STRING)
        val name = display.getString("Name")

        // Determine the item's rarity
        for (i in (lore.size() - 1) downTo 0) {
            val currentLine = lore.getString(i)
            val rarityMatcher = RARITY_PATTERN.find(currentLine)
            if (rarityMatcher != null) {
                val rarity = rarityMatcher.groups["rarity"]?.value ?: continue
                ItemRarity.entries.find {
                    it.rarityName == rarity.stripControlCodes().substringAfter("SHINY ")
                }?.let {
                    return it
                }
            }
        }
        val petRarityMatcher = PET_PATTERN.find(name)
        if (petRarityMatcher != null) {
            val color = petRarityMatcher.groupValues.getOrNull(1) ?: return ItemRarity.NONE
            return ItemRarity.byBaseColor(color) ?: ItemRarity.NONE
        }

        // If the item doesn't have a valid rarity, return null
        return ItemRarity.NONE
    }

    fun isPet(item: ItemStack?): Boolean {
        if (item == null || !item.hasNbt()) {
            return false
        }
        val display = item.getOrCreateSubNbt("display", false)
        if (display == null || !display.contains("Lore")) {
            return false
        }
        val name = display.getString("Name")

        return PET_PATTERN.matches(name)
    }

    fun setSkullTexture(item: ItemStack, texture: String, SkullOwner: String): ItemStack {
        val textureTagCompound = NbtCompound()
        textureTagCompound.putString("Value", texture)

        val textures = NbtList()
        textures.add(textureTagCompound)

        val properties = NbtCompound()
        properties.put("textures", textures)

        val skullOwner = NbtCompound()
        skullOwner.putString("Id", SkullOwner)
        skullOwner.put("Properties", properties)

        val nbtTag = NbtCompound()
        nbtTag.put("SkullOwner", skullOwner)

        item.nbt = nbtTag
        return item
    }

    fun getSkullTexture(item: ItemStack?): String? {
        if (item?.item != Items.PLAYER_HEAD) return null
        val nbt = item?.nbt ?: return null
        if (!nbt.contains("SkullOwner")) return null
        return nbt.getCompound("SkullOwner").getCompound("Properties")
            .getList("textures", Constants.NBT.TAG_COMPOUND).getCompound(0).getString("Value")
    }

    fun ItemStack.setLore(lines: List<String>): ItemStack {
        setSubNbt("display", getOrCreateSubNbt("display", true).apply {
            put("Lore", NbtList().apply {
                for (line in lines) add(NbtString(line))
            })
        })
        return this
    }

    fun getStarCount(extraAttributes: NbtCompound) =
        max(extraAttributes.getInt("upgrade_level"), extraAttributes.getInt("dungeon_item_level"))

    fun isSalvageable(stack: ItemStack): Boolean {
        val extraAttr = getExtraAttributes(stack)
        val sbId = getSkyBlockItemID(extraAttr)
        return extraAttr != null && extraAttr.contains("baseStatBoostPercentage") && getStarCount(
            extraAttr
        ) == 0 && sbId != "ICE_SPRAY_WAND"
    }
}