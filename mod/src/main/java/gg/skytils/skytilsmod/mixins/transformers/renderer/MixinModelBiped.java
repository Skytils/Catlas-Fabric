/*
 * Skytils - Hypixel Skyblock Quality of Life Mod
 * Copyright (C) 2020-2024 Skytils
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

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.model.ModelPart;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BipedEntityModel.class)
public abstract class MixinModelBiped extends EntityModel {
    @Shadow public ModelPart hat;

    @Inject(method = "setAngles", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;popMatrix()V", ordinal = 0))
    private void renderChildHeadPost(Entity entityIn, float f, float g, float h, float i, float j, float scale, CallbackInfo ci) {
        if (this.child && entityIn instanceof PlayerEntity) {
            this.hat.render(scale);
        }
    }

    @WrapWithCondition(method = "setAngles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelPart;render(F)V", ordinal = 6))
    private boolean renderChildHeadwear(ModelPart instance, float j, @Local(argsOnly = true) Entity entityIn) {
        return !this.child || !(entityIn instanceof PlayerEntity);
    }
}
