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

import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//#if MC!=10809
import net.minecraft.client.gui.screen.SplashTextRenderer;
//#endif

@Mixin(TitleScreen.class)
public interface AccessorGuiMainMenu {
    @Accessor
    //#if MC==10809
    //$$ String getSplashText();
    //#else
    SplashTextRenderer getSplashText();
    //#endif

    @Accessor
    //#if MC==10809
    //$$ void setSplashText(String text);
    //#else
    void setSplashText(SplashTextRenderer text);
    //#endif
}
