/*
 * Skytils - Hypixel Skyblock Quality of Life Mod
 * Copyright (C) 2020-2024 Skytils
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

package gg.skytils.event.impl.screen

import gg.skytils.event.CancellableEvent
import gg.skytils.event.Event
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.GenericContainerScreenHandler
import net.minecraft.screen.slot.Slot


/**
 * [gg.skytils.event.mixins.gui.MixinGuiContainer.backgroundDrawn]
 */
class GuiContainerBackgroundDrawnEvent(
    val gui: ContainerGui,
    val container: ScreenHandler,
    val mouseX: Int,
    val mouseY: Int,
    val partialTicks: Float
) : Event() {
    val chestName by lazy {
        getChestName(gui)
    }
}

/**
 * [gg.skytils.event.mixins.gui.MixinGuiContainer.closeWindowPressed]
 */
class GuiContainerCloseWindowEvent(val gui: ContainerGui, val container: ScreenHandler) : CancellableEvent() {
    val chestName by lazy {
        getChestName(gui)
    }
}

/**
 * [gg.skytils.event.mixins.gui.MixinGuiContainer.onDrawSlot]
 */
class GuiContainerPreDrawSlotEvent(val gui: ContainerGui, val container: ScreenHandler, val slot: Slot) : CancellableEvent() {
    val chestName by lazy {
        getChestName(gui)
    }
}

/**
 * [gg.skytils.event.mixins.gui.MixinGuiContainer.onDrawSlotPost]
 */
class GuiContainerPostDrawSlotEvent(val gui: ContainerGui, val container: ScreenHandler, val slot: Slot) : Event() {
    val chestName by lazy {
        getChestName(gui)
    }
}

/**
 * [gg.skytils.event.mixins.gui.MixinGuiContainer.onForegroundDraw]
 */
class GuiContainerForegroundDrawnEvent(
    val gui: ContainerGui,
    val container: ScreenHandler,
    val mouseX: Int,
    val mouseY: Int,
    val partialTicks: Float
) : Event() {
    val chestName by lazy {
        getChestName(gui)
    }
}

/**
 * [gg.skytils.event.mixins.gui.MixinGuiContainer.onMouseClick]
 */
class GuiContainerSlotClickEvent(
    val gui: ContainerGui,
    val container: ScreenHandler,
    val slot: Slot?,
    val slotId: Int,
    val clickedButton: Int,
    val clickType: Int
) : CancellableEvent() {
    val chestName by lazy {
        getChestName(gui)
    }
}

private fun getChestName(containerGui: ContainerGui): String {
    //#if MC<12000
    //$$ return (containerGui.handler as? GenericContainerScreenHandler)?.inventory?.displayName?.method_0_5147()?.trim() ?: error("Container is not a chest")
    //#else
    return containerGui.title.string
    //#endif
}

typealias ContainerGui =
        //#if MC<12000
        //$$ HandledScreen
        //#else
        HandledScreen<*>
        //#endif