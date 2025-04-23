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

package gg.skytils.skytilsmod.utils.toast

import gg.skytils.skytilsmod.gui.profile.components.ItemComponent
import net.minecraft.block.Blocks
import net.minecraft.item.ItemStack

//#if MC==10809
//$$ import net.minecraft.enchantment.Enchantment
//$$ import net.minecraft.item.Item
//#else
import net.minecraft.enchantment.Enchantments
//#endif

class SuperboomToast :
        Toast(
            "§9Superboom TNT",
            ItemComponent(superboom)
        )
{
    companion object {
        //#if MC==10809
        //$$ private val superboom = ItemStack(Item.fromBlock(Blocks.TNT))
        //$$     .apply { addEnchantment(Enchantment.field_0_137, 1) }
        //#else
         private val superboom = ItemStack(Blocks.TNT.asItem())
           .apply { addEnchantment(Enchantments.UNBREAKING, 1) }
        //#endif
    }
}