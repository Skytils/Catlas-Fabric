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

package gg.skytils.skytilsmod.mixins.transformers.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gg.skytils.skytilsmod.Skytils;
import gg.skytils.skytilsmod.utils.Utils;
import net.minecraft.block.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FishingBobberEntity.class)
public abstract class MixinEntityFishHook extends Entity {

    public MixinEntityFishHook(World worldIn) {
        super(worldIn);
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;method_0_363(Lnet/minecraft/util/math/Box;Lnet/minecraft/block/Material;)Z"))
    private boolean allowLavaBobber(World world, Box aabb, Material materialIn, Operation<Boolean> original) {
        boolean orig = original.call(world, aabb, materialIn);
        if (!Utils.INSTANCE.getInSkyblock() || !Skytils.getConfig().getLavaBobber()) return orig;
        return this.world.method_0_363(aabb, Material.LAVA) || orig;
    }
}
