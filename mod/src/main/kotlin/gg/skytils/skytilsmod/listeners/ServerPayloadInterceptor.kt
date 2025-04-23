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

package gg.skytils.skytilsmod.listeners

import gg.skytils.event.EventPriority
import gg.skytils.event.EventSubscriber
import gg.skytils.event.postSync
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.IO
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod._event.*
import gg.skytils.skytilsmod.core.MC
import gg.skytils.skytilsmod.mixins.transformers.accessors.AccessorHypixelModAPI
import gg.skytils.skytilsmod.utils.ifNull
import gg.skytils.skytilsmod.utils.printDevMessage
import io.netty.buffer.Unpooled
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import net.hypixel.modapi.HypixelModAPI
import net.hypixel.modapi.error.ErrorReason
import net.hypixel.modapi.packet.ClientboundHypixelPacket
import net.hypixel.modapi.packet.EventPacket
import net.hypixel.modapi.packet.impl.clientbound.ClientboundHelloPacket
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket
import net.hypixel.modapi.packet.impl.serverbound.ServerboundVersionedPacket
import net.hypixel.modapi.serializer.PacketSerializer
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.minutes

object ServerPayloadInterceptor : EventSubscriber {
    private val receivedPackets = MutableSharedFlow<ClientboundHypixelPacket>()
    private var didSetPacketSender = false
    private val neededEvents = mutableSetOf<KClass<out EventPacket>>(ClientboundLocationPacket::class)

    init {
        neededEvents.forEach {
            runCatching {
                HypixelModAPI.getInstance().subscribeToEventPacket(it.java)
            }.onFailure {
                it.printStackTrace()
            }
        }
    }

    override fun setup() {
        register(::onReceivePacket, EventPriority.Highest)
        register(::onSendPacket, EventPriority.Highest)
        register(::onHypixelPacket)
        register(::onHypixelPacketFail)
    }

    fun onReceivePacket(event: PacketReceiveEvent<*>) {
        if (event.packet is CustomPayloadS2CPacket) {
            val registry = HypixelModAPI.getInstance().registry
            val id = event.packet.method_11456()
            if (registry.isRegistered(id)) {
                printDevMessage({ "Received Hypixel packet $id" }, "hypixelmodapi")
                val data = event.packet.data
                synchronized(data) {
                    data.retain()
                    runCatching {
                        val packetSerializer = PacketSerializer(data.duplicate())
                        if (!packetSerializer.readBoolean()) {
                            val reason = ErrorReason.getById(packetSerializer.readVarInt())
                            postSync(HypixelPacketFailedEvent(id, reason))
                        } else {
                            val packet = registry.createClientboundPacket(id, packetSerializer)
                            IO.launch {
                                receivedPackets.emit(packet)
                            }
                            postSync(HypixelPacketReceiveEvent(packet))
                        }
                    }.onFailure {
                        it.printStackTrace()
                    }
                    data.release()
                }
            }
        }
    }

    fun onSendPacket(event: PacketSendEvent<*>) {
        if (event.packet is CustomPayloadC2SPacket) {
            val registry = HypixelModAPI.getInstance().registry
            val id = event.packet.method_0_5705()
            if (registry.isRegistered(id)) {
                printDevMessage({ "Sent Hypixel packet $id" }, "hypixelmodapi")
                postSync(HypixelPacketSendEvent(id))
            }
        }
    }


    fun onHypixelPacket(event: HypixelPacketReceiveEvent) {
        if (event.packet is ClientboundHelloPacket) {
            val modAPI = HypixelModAPI.getInstance()
            modAPI as AccessorHypixelModAPI
            if (modAPI.packetSender == null) {
                println("Hypixel Mod API packet sender is not set, Skytils will set the packet sender.")
                modAPI.setPacketSender {
                    return@setPacketSender mc.networkHandler?.sendPacket((it as ServerboundVersionedPacket).toCustomPayload()).ifNull {
                        println("Failed to send packet ${it.identifier}")
                    } != null
                }
                didSetPacketSender = true
            }
            Skytils.launch {
                while (mc.networkHandler == null) {
                    println("Waiting for client handler to be set.")
                    delay(50L)
                }
                withContext(Dispatchers.MC) {
                    neededEvents.forEach {
                        HypixelModAPI.getInstance().subscribeToEventPacket(it.java)
                    }
                    if (didSetPacketSender) modAPI.invokeSendRegisterPacket(true)
                }
            }
        }
    }

    fun onHypixelPacketFail(event: HypixelPacketFailedEvent) {
        printDevMessage({ "${Skytils.failPrefix} Mod API request failed: ${event.reason}" }, "hypixelmodapi")
    }

    fun ServerboundVersionedPacket.toCustomPayload(): CustomPayloadC2SPacket {
        val buffer = PacketByteBuf(Unpooled.buffer())
        val serializer = PacketSerializer(buffer)
        this.write(serializer)
        return CustomPayloadC2SPacket(this.identifier, buffer)
    }

    suspend fun <T : ClientboundHypixelPacket> ServerboundVersionedPacket.getResponse(): T = withTimeout(1.minutes) {
        val packet: CustomPayloadC2SPacket = this@getResponse.toCustomPayload()
        mc.networkHandler?.sendPacket(packet)
        return@withTimeout receivedPackets.filter { it.identifier == this@getResponse.identifier }.first() as T
    }
}