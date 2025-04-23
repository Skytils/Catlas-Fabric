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

package gg.skytils.skytilsmod.features.impl.misc

import gg.essential.universal.UChat
import gg.skytils.event.EventSubscriber
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod.Skytils.prefix
import gg.skytils.skytilsmod._event.PacketReceiveEvent
import gg.skytils.skytilsmod.core.structure.GuiElement
import gg.skytils.skytilsmod.mixins.transformers.accessors.AccessorServerListEntryNormal
import gg.skytils.skytilsmod.utils.NumberUtil
import gg.skytils.skytilsmod.utils.NumberUtil.roundToPrecision
import gg.skytils.skytilsmod.utils.Utils
import gg.skytils.skytilsmod.utils.graphics.SmartFontRenderer
import gg.skytils.skytilsmod.utils.graphics.colors.CommonColors
import gg.skytils.skytilsmod.utils.hasMoved
import net.minecraft.client.network.ServerInfo
import net.minecraft.client.network.MultiplayerServerListPinger
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket
import net.minecraft.network.packet.s2c.play.StatisticsS2CPacket
import kotlin.math.abs
import kotlin.math.absoluteValue

object Ping : EventSubscriber {

    var lastPingAt = -1L

    var pingCache = -1.0

    var invokedCommand = false

    val oldServerPinger = MultiplayerServerListPinger()
    var lastOldServerPing = 0L

    override fun setup() {
        register(::onPacket)
    }

    fun sendPing() {
        if (lastPingAt > 0) {
            if (invokedCommand) UChat.chat("§cAlready pinging!")
            return
        }
        mc.player.networkHandler.method_2872().send(
            ClientStatusC2SPacket(ClientStatusC2SPacket.Mode.REQUEST_STATS),
            {
                lastPingAt = System.nanoTime()
            }
        )
    }

    fun onPacket(event: PacketReceiveEvent<*>) {
        if (lastPingAt > 0) {
            when (event.packet) {
                is GameJoinS2CPacket -> {
                    lastPingAt = -1L
                    invokedCommand = false
                }

                is StatisticsS2CPacket -> {
                    val diff = (abs(System.nanoTime() - lastPingAt) / 1_000_000.0)
                    lastPingAt *= -1
                    pingCache = diff
                    if (invokedCommand) {
                        invokedCommand = false
                        UChat.chat(
                            "$prefix §${
                                when {
                                    diff < 50 -> "a"
                                    diff < 100 -> "2"
                                    diff < 149 -> "e"
                                    diff < 249 -> "6"
                                    else -> "c"
                                }
                            }${diff.roundToPrecision(2)} §7ms"
                        )
                    }
                }
            }
        }
    }

    class PingDisplayElement : GuiElement(name = "Ping Display", x = 10, y = 10) {
        override fun render() {
            if (Utils.isOnHypixel && toggled && mc.player != null) {
                when (Skytils.config.pingDisplay) {
                    1 -> {
                        if (mc.currentServerEntry == null) {
                            mc.currentServerEntry = ServerInfo("Skytils-Dummy-Hypixel", "mc.hypixel.net", false)
                        }
                        if (System.currentTimeMillis() - lastOldServerPing > 5000) {
                            lastOldServerPing = System.currentTimeMillis()
                            AccessorServerListEntryNormal.getPingerPool()
                                .submit {
                                    oldServerPinger.add(mc.currentServerEntry)
                                }
                        }
                        if (mc.currentServerEntry.ping != -1L) pingCache =
                            mc.currentServerEntry.ping.toDouble()
                    }

                    2 -> {
                        if (lastPingAt < 0 && (mc.currentScreen != null || !mc.player.hasMoved) && System.nanoTime()
                            - lastPingAt.absoluteValue > 1_000_000L * 5_000
                        ) {
                            sendPing()
                        }
                    }
                }
                if (pingCache != -1.0) {
                    fr.drawString(
                        "${NumberUtil.nf.format(pingCache.roundToPrecision(2))}ms",
                        0f,
                        0f,
                        when {
                            pingCache < 50 -> CommonColors.GREEN
                            pingCache < 100 -> CommonColors.DARK_GREEN
                            pingCache < 149 -> CommonColors.YELLOW
                            pingCache < 249 -> CommonColors.ORANGE
                            else -> CommonColors.RED
                        },
                        SmartFontRenderer.TextAlignment.LEFT_RIGHT,
                        textShadow
                    )
                }
            }
        }

        override fun demoRender() {
            fr.drawString(
                "69.69ms",
                0f,
                0f,
                CommonColors.DARK_GREEN,
                SmartFontRenderer.TextAlignment.LEFT_RIGHT,
                textShadow
            )
        }

        override val toggled: Boolean
            get() = Skytils.config.pingDisplay != 0
        override val height: Int
            get() = fr.field_0_2811
        override val width: Int
            get() = fr.getWidth("69.69ms")

        init {
            Skytils.guiManager.registerElement(this)
        }

    }

    init {
        PingDisplayElement()
    }
}