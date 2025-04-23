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

import gg.skytils.skytilsmod.features.impl.dungeons.MasterMode7Features;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.EnderDragonEntityRenderer;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderDragonEntityRenderer.class)
public abstract class MixinRenderDragon extends MobEntityRenderer<EnderDragonEntity> {
    @Unique
    private EnderDragonEntity lastDragon = null;

    public MixinRenderDragon(EntityRenderDispatcher renderManager, EntityModel modelBase, float f) {
        super(renderManager, modelBase, f);
    }

    @Inject(method = "render(Lnet/minecraft/entity/boss/dragon/EnderDragonEntity;FFFFFF)V", at = @At("HEAD"))
    private void onRenderModel(EnderDragonEntity entitylivingbaseIn, float f, float g, float h, float i, float j, float scaleFactor, CallbackInfo ci) {
        lastDragon = entitylivingbaseIn;
    }

    @ModifyArg(method = "render(Lnet/minecraft/entity/boss/dragon/EnderDragonEntity;FFFFFF)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderColor(FFFF)V"), index = 3)
    private float replaceHurtOpacity(float value) {
        return MasterMode7Features.INSTANCE.getHurtOpacity((EnderDragonEntityRenderer) (Object) this, lastDragon, value);
    }

    @Inject(method = "render(Lnet/minecraft/entity/boss/dragon/EnderDragonEntity;FFFFFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/EntityModel;setAngles(Lnet/minecraft/entity/Entity;FFFFFF)V", ordinal = 2, shift = At.Shift.AFTER))
    private void afterRenderHurtFrame(EnderDragonEntity entitylivingbaseIn, float f, float g, float h, float i, float j, float scaleFactor, CallbackInfo ci) {
        MasterMode7Features.INSTANCE.afterRenderHurtFrame((EnderDragonEntityRenderer) (Object) this, entitylivingbaseIn, f, g, h, i, j, scaleFactor, ci);
    }

    @Inject(method = "getTexture", at = @At("HEAD"), cancellable = true)
    private void replaceEntityTexture(EnderDragonEntity entity, CallbackInfoReturnable<Identifier> cir) {
        MasterMode7Features.INSTANCE.getEntityTexture(entity, cir);
    }
}
