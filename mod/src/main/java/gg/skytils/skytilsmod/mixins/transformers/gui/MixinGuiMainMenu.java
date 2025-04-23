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

package gg.skytils.skytilsmod.mixins.transformers.gui;

import gg.skytils.skytilsmod.mixins.hooks.gui.GuiMainMenuHookKt;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.class_411;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Calendar;

@Mixin(TitleScreen.class)
public class MixinGuiMainMenu extends Screen implements class_411 {
    @Inject(method = "init", at = @At(value = "INVOKE", target = "Ljava/util/Calendar;setTime(Ljava/util/Date;)V", shift = At.Shift.AFTER, remap = false), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void setSplashText(CallbackInfo ci, Calendar calendar) {
        GuiMainMenuHookKt.setSplashText((TitleScreen) (Object) this, calendar);
    }
}
