/*
 * Skytils - Hypixel Skyblock Quality of Life Mod
 * Copyright (C) 2020-2025 Skytils
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

package gg.skytils.skytilsmod.gui.features

import gg.essential.api.EssentialAPI
import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.ChildBasedMaxSizeConstraint
import gg.essential.elementa.constraints.CramSiblingConstraint
import gg.essential.elementa.constraints.SubtractiveConstraint
import gg.essential.elementa.dsl.basicColorConstraint
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.pixels
import gg.essential.elementa.events.UIClickEvent
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.universal.UChat
import gg.essential.vigilance.utils.onLeftClick
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.core.PersistentSave
import gg.skytils.skytilsmod.features.impl.protectitems.strategy.ItemProtectStrategy
import gg.skytils.skytilsmod.features.impl.protectitems.strategy.impl.FavoriteStrategy
import gg.skytils.skytilsmod.gui.constraints.FixedChildBasedRangeConstraint
import gg.skytils.skytilsmod.gui.profile.components.InventoryComponent
import gg.skytils.skytilsmod.gui.profile.components.SlotComponent
import gg.skytils.skytilsmod.gui.profile.components.WardrobeComponent
import gg.skytils.skytilsmod.utils.ItemUtil
import net.minecraft.item.ItemStack
import java.awt.Color

class ProtectItemGui : WindowScreen(ElementaVersion.V2, newGuiScale = EssentialAPI.Companion.getGuiUtil().getGuiScale()) {

    val protectedColor = Color(75, 227, 62, 190)
    val unprotectedColor = Color(255, 0, 0, 190)

    val inventoryState: State<List<ItemStack?>?> by lazy {
        val inv = client.player.inventory.field_7547.toList()
        BasicState(inv.subList(9, inv.size) + inv.subList(0, 9))
    }

    val inventoryComponent = InventoryComponent(inventoryState).constrain {
        x = CenterConstraint()
        y = CenterConstraint()

        width = (9 * (16 + 2)).pixels
        height = (4 * (16 + 2)).pixels
    }.apply {
        parseInv(inventoryState.get())
        Window.Companion.enqueueRenderOperation {
            children.filterIsInstance<SlotComponent>().forEach(::setup)
        }
    } childOf window

    val armorState: State<List<ItemStack?>> = BasicState(client.player.inventory.field_7548.reversed())
    val armorComponent = WardrobeComponent.ArmorComponent(armorState, true).constrain {
        x = SubtractiveConstraint(inventoryComponent.constraints.x, 50.pixels)
        y = CramSiblingConstraint()
        width = ChildBasedMaxSizeConstraint()
        height = FixedChildBasedRangeConstraint()
    }.apply {
        parseSlots(armorState.get())
        Window.Companion.enqueueRenderOperation {
            children.filterIsInstance<SlotComponent>().forEach(::setup)
        }
    } childOf window

    private fun setup(slot: SlotComponent) {
        if (slot.item != null) {
            slot.constrain {
                color = basicColorConstraint {
                    if (FavoriteStrategy.worthProtecting(
                            slot.item,
                            ItemUtil.getExtraAttributes(slot.item),
                            ItemProtectStrategy.ProtectType.DROPKEYININVENTORY
                        )
                    )
                        protectedColor
                    else
                        unprotectedColor
                }
            }
        }
        slot.onLeftClick { e ->
            onLeftClickSlot(e, slot)
        }
    }

    fun onLeftClickSlot(event: UIClickEvent, slot: SlotComponent): Boolean {
        val item = slot.item ?: return false
        val extraAttributes = ItemUtil.getExtraAttributes(item) ?: return false
        if (extraAttributes.contains("uuid")) {
            val uuid = extraAttributes.getString("uuid")
            if (FavoriteStrategy.favoriteUUIDs.remove(uuid)) {
                PersistentSave.Companion.markDirty<FavoriteStrategy.FavoriteStrategySave>()
                UChat.chat("${Skytils.successPrefix} §cI will no longer protect your ${item.name}§a!")
                return true
            } else {
                FavoriteStrategy.favoriteUUIDs.add(uuid)
                PersistentSave.Companion.markDirty<FavoriteStrategy.FavoriteStrategySave>()
                UChat.chat("${Skytils.successPrefix} §aI will now protect your ${item.name}!")
                return true
            }
        } else {
            val itemId = ItemUtil.getSkyBlockItemID(item) ?: return false
            if (FavoriteStrategy.favoriteItemIds.remove(itemId)) {
                PersistentSave.Companion.markDirty<FavoriteStrategy.FavoriteStrategySave>()
                UChat.chat("${Skytils.successPrefix} §cI will no longer protect all of your ${itemId}s!")
                return true
            } else {
                FavoriteStrategy.favoriteItemIds.add(itemId)
                PersistentSave.Companion.markDirty<FavoriteStrategy.FavoriteStrategySave>()
                UChat.chat("${Skytils.successPrefix} §aI will now protect all of your ${itemId}s!")
                return true
            }
        }
    }
}