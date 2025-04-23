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
package gg.skytils.skytilsmod.features.impl.protectitems

import gg.essential.universal.UChat
import gg.skytils.event.CancellableEvent
import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.screen.GuiContainerCloseWindowEvent
import gg.skytils.event.impl.screen.GuiContainerSlotClickEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils.failPrefix
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod._event.ItemTossEvent
import gg.skytils.skytilsmod.core.SoundQueue
import gg.skytils.skytilsmod.features.impl.protectitems.strategy.ItemProtectStrategy
import gg.skytils.skytilsmod.utils.ItemUtil
import gg.skytils.skytilsmod.utils.Utils
import gg.skytils.skytilsmod.utils.displayNameStr
import gg.skytils.skytilsmod.utils.multiplatform.SlotActionType
import gg.skytils.skytilsmod.utils.toStringIfTrue
import net.minecraft.block.Blocks
import net.minecraft.screen.GenericContainerScreenHandler
import net.minecraft.item.Item
import net.minecraft.item.ItemStack

object ProtectItems : EventSubscriber {

    init {
        ItemProtectStrategy.isAnyToggled()
    }

    override fun setup() {
        register(::onCloseWindow)
        register(::onDropItem)
        register(::onSlotClick)
    }

    fun onCloseWindow(event: GuiContainerCloseWindowEvent) {
        if (!Utils.inSkyblock) return
        val item =
            //#if MC==10809
            //$$ mc.player?.inventory?.cursorStack ?: return
            //#else
            event.container.cursorStack
            //#endif
        val extraAttr = ItemUtil.getExtraAttributes(item)
        val strategy = ItemProtectStrategy.findValidStrategy(item, extraAttr, ItemProtectStrategy.ProtectType.USERCLOSEWINDOW) ?: return
        for (slot in event.container.slots) {
            if (slot.inventory !== mc.player?.inventory || slot.hasStack() || !slot.canInsert(item)) continue
            mc.interactionManager?.clickSlot(event.container.syncId, slot.id, 0, SlotActionType.PICKUP, mc.player)
            notifyStopped(null, "dropping", strategy)
            return
        }
        notifyStopped(null, "closing the window on", strategy)
        event.cancelled = true
    }

    fun onDropItem(event: ItemTossEvent) {
        if (!Utils.inSkyblock) return
        val strategy = ItemProtectStrategy.findValidStrategy(event.stack, ItemUtil.getExtraAttributes(event.stack), ItemProtectStrategy.ProtectType.HOTBARDROPKEY) ?: return
        notifyStopped(event, "dropping", strategy)
    }

    fun onSlotClick(event: GuiContainerSlotClickEvent) {
        if (!Utils.inSkyblock) return
        if (event.container is GenericContainerScreenHandler && ItemProtectStrategy.isAnyToggled()) {
            val inv = (event.container as GenericContainerScreenHandler).inventory
            val chestName = event.chestName
            if (event.slot != null && event.slot!!.hasStack()) {
                var item: ItemStack = event.slot!!.stack ?: return
                var extraAttr = ItemUtil.getExtraAttributes(item)
                if (chestName.startsWith("Salvage")) {
                    var inSalvageGui = false
                    if (item.displayNameStr.contains("Salvage") || item.displayNameStr.contains("Essence")) {
                        val salvageItem = inv.getStack(13) ?: return
                        item = salvageItem
                        extraAttr = ItemUtil.getExtraAttributes(item) ?: return
                        inSalvageGui = true
                    }
                    if (inSalvageGui || event.slot!!.inventory === mc.player?.inventory) {
                        val strategy = ItemProtectStrategy.findValidStrategy(
                            item,
                            extraAttr,
                            ItemProtectStrategy.ProtectType.SALVAGE
                        )
                        if (strategy != null) {
                            notifyStopped(event, "salvaging", strategy)
                            return
                        }
                    }
                }
                if (chestName != "Large Chest" && inv.size() == 54) {
                    if (!chestName.contains("Auction")) {
                        val sellItem = inv.getStack(49)
                        if (sellItem != null) {
                            if (sellItem.item === hopperItem && sellItem.displayNameStr.contains(
                                    "Sell Item"
                                ) || ItemUtil.getItemLore(sellItem).any { s: String -> s.contains("buyback") }
                            ) {
                                if (event.slotId != 49 && event.slot!!.inventory === mc.player?.inventory) {
                                    val strategy = ItemProtectStrategy.findValidStrategy(
                                        item,
                                        extraAttr,
                                        ItemProtectStrategy.ProtectType.SELLTONPC
                                    )
                                    if (strategy != null) {
                                        notifyStopped(event, "selling", strategy)
                                        return
                                    }
                                }
                            }
                        }
                    } else if (event.slotId == 29 && chestName.startsWith("Create ") && chestName.endsWith(" Auction")) {
                        val itemForSale = inv.getStack(13)
                        if (itemForSale != null) {
                            val strategy = ItemProtectStrategy.findValidStrategy(
                                itemForSale,
                                ItemUtil.getExtraAttributes(itemForSale),
                                ItemProtectStrategy.ProtectType.SELLTOAUCTION
                            )
                            if (strategy != null) {
                                notifyStopped(event, "auctioning", strategy)
                                return
                            }
                        }
                    }
                }
            }
        }
        val cursorStack =
            //#if MC==10809
            //$$ mc.player?.inventory?.cursorStack
            //#else
            event.container.cursorStack
            //#endif
        if (event.slotId == -999 && cursorStack != null && event.clickType != 5) {
            val extraAttr = ItemUtil.getExtraAttributes(cursorStack)
            val strategy = ItemProtectStrategy.findValidStrategy(cursorStack, extraAttr, ItemProtectStrategy.ProtectType.CLICKOUTOFWINDOW)
            if (strategy != null) {
                notifyStopped(event, "dropping", strategy)
                return
            }
        }
        if (event.clickType == 4 && event.slotId != -999 && event.slot != null && event.slot!!.hasStack()) {
            val item = event.slot!!.stack
            val extraAttr = ItemUtil.getExtraAttributes(item)
            val strategy = ItemProtectStrategy.findValidStrategy(item, extraAttr, ItemProtectStrategy.ProtectType.DROPKEYININVENTORY)
            if (strategy != null) {
                notifyStopped(event, "dropping", strategy)
                return
            }
        }
    }

    private fun notifyStopped(event: CancellableEvent?, action: String, strategy: ItemProtectStrategy? = null) {
        SoundQueue.addToQueue("note.bass", 0.5f, 1f)
        UChat.chat("$failPrefix §cStopped you from $action that item!${"§7 (§e${strategy?.name}§7)".toStringIfTrue(strategy != null)}")
        event?.cancelled = true
    }

    private val hopperItem =
        //#if MC==10809
        //$$ Item.fromBlock(Blocks.field_0_787)
        //#else
        Blocks.HOPPER.asItem()
        //#endif
}