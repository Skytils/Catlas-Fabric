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

package gg.skytils.event.mixins.entity;

import gg.skytils.event.EventsKt;
import gg.skytils.event.impl.entity.EntityAttackEvent;
import gg.skytils.event.impl.play.EntityInteractEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//#if MC>12000
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
//#endif

@Mixin(PlayerEntity.class)
public class MixinEntityPlayer {
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onAttack(Entity targetEntity, CallbackInfo ci) {
        if (EventsKt.postCancellableSync(new EntityAttackEvent((Entity) (Object) this, targetEntity))) {
            ci.cancel();
        }
    }
}
