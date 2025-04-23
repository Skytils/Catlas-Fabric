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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gg.skytils.skytilsmod.Skytils;
import gg.skytils.skytilsmod.mixins.hooks.renderer.ItemRendererHookKt;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class MixinItemRenderer {
    @Shadow
    private ItemStack mainHand;

    @WrapOperation(method = "renderFirstPersonItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;method_0_7990()I"))
    private int getItemInUseCountForFirstPerson(AbstractClientPlayerEntity abstractClientPlayer, Operation<Integer> original) {
        return ItemRendererHookKt.getItemInUseCountForFirstPerson(abstractClientPlayer, this.mainHand, original);
    }

    @Inject(method = "applySwingOffset", at = @At(value = "TAIL"))
    private void modifySize(float equipProgress, float swingProgress, CallbackInfo ci) {
        ItemRendererHookKt.modifySize();
    }

    @WrapOperation(method = "renderOverlays", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;method_3236(F)V"))
    private void cancelFirstPersonFire(HeldItemRenderer instance, float f1, Operation<Void> original) {
        if (!Skytils.getConfig().getNoFire()) {
            original.call(instance, f1);
        }
    }
}
