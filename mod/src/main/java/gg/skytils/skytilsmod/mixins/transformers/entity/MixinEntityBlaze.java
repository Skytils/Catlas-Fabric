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

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import gg.skytils.skytilsmod.mixins.hooks.entity.EntityBlazeHookKt;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.particle.ParticleType;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlazeEntity.class)
public abstract class MixinEntityBlaze extends HostileEntity {

    public MixinEntityBlaze(World worldIn) {
        super(worldIn);
    }

    @WrapWithCondition(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addParticle(Lnet/minecraft/particle/ParticleType;DDDDDD[I)V"))
    private boolean spawnParticle(World world, ParticleType particleType, double xCoord, double yCoord, double zCoord, double xOffset, double yOffset, double zOffset, int[] p_175688_14_) {
        return EntityBlazeHookKt.removeBlazeSmokeParticle(world, particleType, xCoord, yCoord, zCoord, xOffset, yOffset, zOffset, p_175688_14_);
    }
}