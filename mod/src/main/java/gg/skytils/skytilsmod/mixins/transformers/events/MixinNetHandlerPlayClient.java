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

package gg.skytils.skytilsmod.mixins.transformers.events;

import gg.skytils.event.EventsKt;
import gg.skytils.skytilsmod._event.PacketSendEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.client.network.ClientConnectionState;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class MixinNetHandlerPlayClient extends ClientCommonNetworkHandler {
    protected MixinNetHandlerPlayClient(MinecraftClient client, ClientConnection connection, ClientConnectionState connectionState) {
        super(client, connection, connectionState);
    }

    // TODO: change injection site
    @Override
    public void sendPacket(Packet<?> packet) {
        if (EventsKt.postCancellableSync(new PacketSendEvent(packet))) {
            return;
        }
        super.sendPacket(packet);
    }
}
