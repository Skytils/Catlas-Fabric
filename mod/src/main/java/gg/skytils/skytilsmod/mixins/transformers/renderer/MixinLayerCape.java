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

import net.minecraft.client.network.AbstractClientPlayerEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.class_995;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CapeFeatureRenderer.class, priority = 1010)
public abstract class MixinLayerCape implements class_995<AbstractClientPlayerEntity> {
    @Shadow
    @Final
    private PlayerEntityRenderer field_4840;

    @Inject(method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFFFFFF)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;pushMatrix()V", shift = At.Shift.AFTER))
    private void scaleChild(AbstractClientPlayerEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale, CallbackInfo ci) {
        if (this.field_4840.method_4038().child || entity.isBaby()) {
            RenderSystem.method_4453(0.5, 0.5, 0.5);
            RenderSystem.method_4348(0.0F, 24.0F * scale, 0.0F);
        }
    }
}
