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

import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SignEditScreen.class)
public interface AccessorGuiEditSign {
    /**
     * Reference to the sign object.
     */
    @Accessor("sign")
    SignBlockEntity getTileSign();

    /**
     * Counts the number of screen updates.
     */
    @Accessor("ticksSinceOpened")
    int getUpdateCounter();

    /**
     * The index of the line that is being edited.
     */
    @Accessor("currentRow")
    int getEditLine();

    /**
     * "Done" button for the GUI.
     */
    @Accessor("field_3032")
    ClickableWidget getDoneBtn();
}
