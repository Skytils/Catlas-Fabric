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
import gg.skytils.skytilsmod.mixins.hooks.renderer.LayerCustomHeadHookKt;
import net.minecraft.client.render.entity.feature.HeadFeatureRenderer;
import net.minecraft.class_995;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeadFeatureRenderer.class)
public abstract class MixinLayerCustomHead implements class_995<LivingEntity> {

    @WrapOperation(method = "method_4199", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;isSneaking()Z"))
    private boolean disableSneakOffset(LivingEntity instance, Operation<Boolean> original) {
        return (!(instance instanceof PlayerEntity) || !instance.isBaby()) && original.call(instance);
    }

    @Inject(method = "method_4199", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderColor(FFFF)V", shift = At.Shift.AFTER), cancellable = true)
    private void renderCustomHeadLayer(LivingEntity entity, float p_177141_2_, float p_177141_3_, float partialTicks, float p_177141_5_, float p_177141_6_, float p_177141_7_, float scale, CallbackInfo ci) {
        LayerCustomHeadHookKt.renderCustomHeadLayer(entity, p_177141_2_, p_177141_3_, partialTicks, p_177141_5_, p_177141_6_, p_177141_7_, scale, ci);
    }

    @Inject(method = "method_4199", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;popMatrix()V"))
    private void renderCustomHeadLayerPost(LivingEntity entity, float p_177141_2_, float p_177141_3_, float partialTicks, float p_177141_5_, float p_177141_6_, float p_177141_7_, float scale, CallbackInfo ci) {
        LayerCustomHeadHookKt.renderCustomHeadLayerPost(entity, p_177141_2_, p_177141_3_, partialTicks, p_177141_5_, p_177141_6_, p_177141_7_, scale, ci);
    }

    @Inject(method = "method_4199", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/block/entity/SkullBlockEntityRenderer;method_3581(FFFLnet/minecraft/util/math/Direction;FILcom/mojang/authlib/GameProfile;I)V"), cancellable = true)
    private void renderGlintOnSkull(LivingEntity entitylivingbaseIn, float p_177141_2_, float p_177141_3_, float partialTicks, float p_177141_5_, float p_177141_6_, float p_177141_7_, float scale, CallbackInfo ci) {
        LayerCustomHeadHookKt.renderGlintOnSkull(entitylivingbaseIn, p_177141_2_, p_177141_3_, partialTicks, p_177141_5_, p_177141_6_, p_177141_7_, scale, ci);
    }

}
