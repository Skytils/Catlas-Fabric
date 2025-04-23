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

package gg.skytils.skytilsmod.mixins.transformers.accessors;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

//#if MC>=12000
import net.minecraft.screen.slot.SlotActionType;
//#endif

@Mixin(HandledScreen.class)
public interface AccessorGuiContainer {
    @Accessor("backgroundWidth")
    int getXSize();

    @Accessor("backgroundHeight")
    int getYSize();

    @Accessor("x")
    int getGuiLeft();

    @Accessor("y")
    int getGuiTop();

    @Invoker("onMouseClick")
    //#if MC<12000
    //$$ void invokeHandleMouseClick(Slot slotIn, int slotId, int clickedButton, int clickType);
    //#else
    void invokeHandleMouseClick(Slot slotIn, int slotId, int clickedButton, SlotActionType clickType);
    //#endif
}
