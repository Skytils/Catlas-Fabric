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

package gg.skytils.skytilsmod.mixins.transformers.renderer;

import gg.skytils.skytilsmod.mixins.hooks.renderer.RenderItemHookKt;
import net.minecraft.client.TextRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class MixinRenderItem {
    @Inject(method = "method_4021", at = @At("HEAD"))
    private void renderRarity(ItemStack stack, int x, int y, CallbackInfo ci) {
        RenderItemHookKt.renderRarity(stack, x, y, ci);
    }

    @Inject(method = "renderItemAndGlow(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/BakedModel;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;method_4384(FFF)V", shift = At.Shift.AFTER))
    private void renderItemPre(ItemStack stack, BakedModel model, CallbackInfo ci) {
        RenderItemHookKt.renderItemPre(stack, model, ci);
    }

    @Inject(method = "renderItemAndGlow(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/BakedModel;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/ItemRenderer;method_4011(Lnet/minecraft/client/render/model/BakedModel;)V", shift = At.Shift.BEFORE), cancellable = true)
    private void modifyGlintRendering(ItemStack stack, BakedModel model, CallbackInfo ci) {
        RenderItemHookKt.modifyGlintRendering(stack, model, ci);
    }

    @Inject(method = "method_4011", at = @At("HEAD"), cancellable = true)
    public void onRenderEffect(CallbackInfo ci) {
        if (RenderItemHookKt.getSkipGlint()) {
            ci.cancel();
        }
    }

}
