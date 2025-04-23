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

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import gg.skytils.event.EventsKt;
import gg.skytils.event.impl.render.SelectionBoxDrawEvent;
import gg.skytils.event.impl.render.WorldDrawEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=12000
import net.minecraft.client.util.math.MatrixStack;
//#endif

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
    @Shadow private MinecraftClient client;

    @Inject(method =
            //#if MC<12000
            //$$ "renderWorldInternal",
            //#else
            "renderWorld",
            //#endif
            at = @At(value = "CONSTANT", args = "stringValue=hand", shift = At.Shift.BEFORE))
    //#if MC<12000
    //$$ public void renderWorld(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
    //$$     EventsKt.postSync(new WorldDrawEvent(partialTicks));
    //$$ }
    //#else
    public void renderWorld(float tickDelta, long limitTime, MatrixStack matrices, CallbackInfo ci) {
        EventsKt.postSync(new WorldDrawEvent(tickDelta));
    }
    //#endif

    // Moved to WorldRenderer
    //#if MC<12000
    //$$ @WrapWithCondition(method = "renderWorldInternal", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;method_3294(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/hit/HitResult;IF)V"))
    //$$ public boolean renderSelectionBox(WorldRenderer instance, PlayerEntity d1, HitResult d2, int f, float blockpos, @Local(argsOnly = true) float partialTicks) {
    //$$     SelectionBoxDrawEvent event = new SelectionBoxDrawEvent(this.client.crosshairTarget, partialTicks);
    //$$     return !EventsKt.postCancellableSync(event);
    //$$ }
    //#endif
}
