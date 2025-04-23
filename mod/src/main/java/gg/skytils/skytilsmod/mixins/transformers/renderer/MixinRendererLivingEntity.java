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
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import gg.skytils.skytilsmod.mixins.hooks.renderer.RendererLivingEntityHookKt;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinRendererLivingEntity<T extends LivingEntity> extends EntityRenderer<T> {
    protected MixinRendererLivingEntity(EntityRenderDispatcher renderManager) {
        super(renderManager);
    }

    @ModifyVariable(method = "renderName*", at = @At(value = "STORE", ordinal = 0))
    private String replaceEntityName(String name, @Local(argsOnly = true) T entity) {
        return RendererLivingEntityHookKt.replaceEntityName(entity, name);
    }

    @Inject(method = "getOverlayColor", at = @At("HEAD"), cancellable = true)
    private void setColorMultiplier(T entity, float lightBrightness, float partialTickTime, CallbackInfoReturnable<Integer> cir) {
        RendererLivingEntityHookKt.setColorMultiplier(entity, lightBrightness, partialTickTime, cir);
    }

    @WrapOperation(method = "applyOverlayColor(Lnet/minecraft/entity/LivingEntity;FZ)Z", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/LivingEntity;hurtTime:I", opcode = Opcodes.GETFIELD))
    private int changeHurtTime(LivingEntity instance, Operation<Integer> original) {
        return RendererLivingEntityHookKt.replaceHurtTime(instance, original);
    }
}
