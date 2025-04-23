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

package gg.skytils.event.mixins.render;

import gg.skytils.event.EventsKt;
import gg.skytils.event.impl.render.LivingEntityPostRenderEvent;
import gg.skytils.event.impl.render.LivingEntityPreRenderEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>12000
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
//#endif

@Mixin(LivingEntityRenderer.class)
public class MixinRendererLivingEntity
        //#if MC<12000
        //$$ <T extends LivingEntity>
        //#else
        <T extends LivingEntity, M extends EntityModel<T>>
        //#endif
{
    //#if MC<12000
    //$$ @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;DDDFF)V", at = @At("HEAD"), cancellable = true)
    //$$ private void onRender(T entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
    //#else
    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"), cancellable = true)
    private void onRender(T entity, float f, float partialTicks, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
    //#endif
        Entity viewEntity = MinecraftClient.getInstance().getCameraEntity();
        if (viewEntity == null) return;
        double renderX = entity.lastRenderX + (entity.getPos().x - entity.lastRenderX - viewEntity.getPos().x + viewEntity.lastRenderX) * partialTicks - viewEntity.lastRenderX;
        double renderY = entity.lastRenderY + (entity.getPos().y - entity.lastRenderY - viewEntity.getPos().y + viewEntity.lastRenderY) * partialTicks - viewEntity.lastRenderY;
        double renderZ = entity.lastRenderZ + (entity.getPos().z - entity.lastRenderZ - viewEntity.getPos().z + viewEntity.lastRenderZ) * partialTicks - viewEntity.lastRenderZ;
        @SuppressWarnings("unchecked")
        //#if MC<12000
        //$$ LivingEntityPreRenderEvent<T>
        //#else
        LivingEntityPreRenderEvent<T, M>
        //#endif
                event =
                new LivingEntityPreRenderEvent<>(entity,
                    //#if MC<12000
                    //$$ (LivingEntityRenderer<T>) (Object) this,
                    //#else
                    (LivingEntityRenderer<T, M>) (Object) this,
                    //#endif
                    renderX, renderY, renderZ, partialTicks);
        if (EventsKt.postCancellableSync(event)) {
            ci.cancel();
        }
    }

    //#if MC<12000
    //$$ @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;DDDFF)V", at = @At("TAIL"))
    //$$ private void onRenderPost(T entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
    //#else
    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("TAIL"))
    private void onRenderPost(T entity, float f, float partialTicks, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
    //#endif
        EventsKt.postSync(new LivingEntityPostRenderEvent(entity));
    }
}
