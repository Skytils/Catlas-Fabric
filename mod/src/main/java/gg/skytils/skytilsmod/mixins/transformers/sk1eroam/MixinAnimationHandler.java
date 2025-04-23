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

package gg.skytils.skytilsmod.mixins.transformers.sk1eroam;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import gg.skytils.skytilsmod.mixins.hooks.renderer.ItemRendererHookKt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.UseAction;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(targets = "club.sk1er.oldanimations.AnimationHandler", remap = false)
public abstract class MixinAnimationHandler {

    @Shadow
    @Final
    private MinecraftClient mc;

    @Dynamic
    @ModifyVariable(method = "renderItemInFirstPerson", at = @At("STORE"))
    private UseAction changeEnumAction(UseAction action) {
        return mc.player.method_0_7989() != null ? ItemRendererHookKt.getItemInUseCountForFirstPerson(mc.player, mc.player.method_0_7989(), null) == 0 ? UseAction.NONE : action : action;
    }
}
