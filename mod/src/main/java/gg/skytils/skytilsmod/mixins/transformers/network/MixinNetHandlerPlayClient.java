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

package gg.skytils.skytilsmod.mixins.transformers.network;

import gg.skytils.skytilsmod.features.impl.dungeons.MasterMode7Features;
import gg.skytils.skytilsmod.mixins.extensions.ExtensionEntityLivingBase;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.MobSpawnS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientPlayNetworkHandler.class, priority = 1001)
public abstract class MixinNetHandlerPlayClient implements ClientPlayPacketListener {
    @Shadow
    private ClientWorld world;

    @Inject(method = "onMobSpawn", at = @At("TAIL"))
    private void onHandleSpawnMobTail(MobSpawnS2CPacket packetIn, CallbackInfo ci) {
        Entity entity = this.world.getEntityById(packetIn.getId());
        if (entity != null) {
            MasterMode7Features.INSTANCE.onMobSpawned(entity);
            ((ExtensionEntityLivingBase) entity).getSkytilsHook().onNewDisplayName(
                    entity.getDataTracker().getString(2)
            );
        }
    }
}
