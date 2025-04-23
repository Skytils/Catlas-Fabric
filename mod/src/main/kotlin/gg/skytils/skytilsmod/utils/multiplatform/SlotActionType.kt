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

package gg.skytils.skytilsmod.utils.multiplatform

//#if MC==10809
//$$ object SlotActionType {
//$$     /**
//$$      * Performs a normal slot click. This can pickup or place items in the slot, possibly merging the cursor stack into the slot, or swapping the slot stack with the cursor stack if they can't be merged.
//$$      */
//$$     val PICKUP = 0
//$$     /**
//$$      * Performs a shift-click. This usually quickly moves items between the player's inventory and the open screen handler.
//$$      */
//$$     val QUICK_MOVE = 1
//$$     /**
//$$      * Exchanges items between a slot and a hotbar slot. This is usually triggered by the player pressing a 1-9 number key while hovering over a slot.
//$$      *
//$$      * <p>When the action type is swap, the click data is the hotbar slot to swap with (0-8).
//$$      */
//$$     val SWAP = 2
//$$     /**
//$$      * Clones the item in the slot. Usually triggered by middle clicking an item in creative mode.
//$$      */
//$$     val CLONE = 3
//$$     /**
//$$      * Throws the item out of the inventory. This is usually triggered by the player pressing Q while hovering over a slot, or clicking outside the window.
//$$      *
//$$      * <p>When the action type is throw, the click data determines whether to throw a whole stack (1) or a single item from that stack (0).
//$$      */
//$$     val THROW = 4
//$$     /**
//$$      * Drags items between multiple slots. This is usually triggered by the player clicking and dragging between slots.
//$$      *
//$$      * <p>This action happens in 3 stages. Stage 0 signals that the drag has begun, and stage 2 signals that the drag has ended. In between multiple stage 1s signal which slots were dragged on.
//$$      *
//$$      * <p>The stage is packed into the click data along with the mouse button that was clicked. See {@link net.minecraft.screen.ScreenHandler#packQuickCraftData(int, int) ScreenHandler.packQuickCraftData(int, int)} for details.
//$$      */
//$$     val QUICK_CRAFT = 5
//$$     /**
//$$      * Replenishes the cursor stack with items from the screen handler. This is usually triggered by the player double clicking.
//$$      */
//$$     val PICKUP_ALL = 6
//$$ }
//#else
typealias SlotActionType = net.minecraft.screen.slot.SlotActionType
//#endif