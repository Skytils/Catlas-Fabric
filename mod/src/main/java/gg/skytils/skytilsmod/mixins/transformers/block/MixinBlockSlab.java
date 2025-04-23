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

package gg.skytils.skytilsmod.mixins.transformers.block;

import gg.skytils.skytilsmod.core.Config;
import net.minecraft.block.Block;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SlabBlock.class)
public abstract class MixinBlockSlab extends Block {
    public MixinBlockSlab(Material materialIn) {
        super(materialIn);
    }

    @Inject(method = "doesSideBlockRendering", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;testProperty(Lnet/minecraft/state/property/Property;)Ljava/lang/Comparable;", ordinal = 0), cancellable = true)
    private void checkRendering(WorldView world, BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> cir) {
        if (Config.INSTANCE.getFixFallingSandRendering() && !(world.getBlockState(pos).getBlock() instanceof SlabBlock))
            cir.setReturnValue(true);
    }
}
