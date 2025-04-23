/*
 * Skytils - Hypixel Skyblock Quality of Life Mod
 * Copyright (C) 2020-2025 Skytils
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

package gg.skytils.skytilsmod.mixins.transformers.skyhanni;

import gg.skytils.skytilsmod.features.impl.funny.skytilsplus.SheepifyRebellion;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "at.hannibal2.skyhanni.features.misc.HideArmor", remap = false)
public abstract class MixinHideArmor {
    @Inject(method = "shouldHideArmor", at = @At("RETURN"), cancellable = true, require = 0)
    private void skytils$shouldHideArmor(PlayerEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        if (SheepifyRebellion.INSTANCE.getDummyModelMap().containsKey(entity)) {
            cir.setReturnValue(false);
        }
    }
}
