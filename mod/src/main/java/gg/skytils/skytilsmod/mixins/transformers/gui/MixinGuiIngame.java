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

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import gg.skytils.skytilsmod.mixins.hooks.gui.GuiIngameForgeHookKt;
import gg.skytils.skytilsmod.mixins.hooks.gui.GuiIngameHookKt;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.player.PlayerEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class MixinGuiIngame extends DrawContext {

    @ModifyExpressionValue(method = "renderHeldItemTooltip", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/hud/InGameHud;heldItemTooltipFade:I", opcode = Opcodes.GETFIELD))
    private int alwaysShowItemHighlight(int original) {
        return GuiIngameForgeHookKt.alwaysShowItemHighlight(original);
    }

    @Inject(method = "renderHotbarItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/ItemRenderer;method_4026(Lnet/minecraft/item/ItemStack;II)V"))
    private void renderRarityOnHotbar(int index, int xPos, int yPos, float partialTicks, PlayerEntity player, CallbackInfo ci) {
        GuiIngameHookKt.renderRarityOnHotbar(index, xPos, yPos, partialTicks, player, ci);
    }

    @ModifyVariable(method = "renderVignetteOverlay", at = @At(value = "STORE", ordinal = 0), ordinal = 1)
    private float disableWorldBorder(float f) {
        return GuiIngameHookKt.onWorldBorder(f);
    }

}
