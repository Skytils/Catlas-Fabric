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

import com.mojang.authlib.GameProfile;
import gg.skytils.skytilsmod.mixins.hooks.renderer.TileEntitySkullRendererHookKt;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.block.entity.SkullBlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;


@Mixin(SkullBlockEntityRenderer.class)
public abstract class MixinTileEntitySkullRenderer extends BlockEntityRenderer<SkullBlockEntity> {
    @Inject(method = "method_3581", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/EntityModel;setAngles(Lnet/minecraft/entity/Entity;FFFFFF)V", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void addGlintToSkull(float x, float y, float z, Direction face, float rotation, int type, GameProfile profile, int p_180543_8_, CallbackInfo ci, EntityModel model) {
        TileEntitySkullRendererHookKt.addGlintToSkull(x, y, z, face, rotation, type, profile, p_180543_8_, ci, model);
    }
}
