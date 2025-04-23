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
import gg.skytils.skytilsmod.features.impl.dungeons.MasterMode7Features;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.class_1509;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.BossEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.particle.ParticleType;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EnderDragonEntity.class)
public abstract class MixinEntityDragon extends MobEntity implements BossEntity, class_1509, Monster {
    public MixinEntityDragon(World worldIn) {
        super(worldIn);
    }

    @WrapWithCondition(method = "updatePostDeath", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addParticle(Lnet/minecraft/particle/ParticleType;DDDDDD[I)V"))
    private boolean removeDeathParticle(World instance, ParticleType particleType, double xCoord, double yCoord, double zCoord, double xOffset, double yOffset, double zOffset, int[] p_175688_14_) {
        return !MasterMode7Features.INSTANCE.shouldHideDragonDeath();
    }
}
