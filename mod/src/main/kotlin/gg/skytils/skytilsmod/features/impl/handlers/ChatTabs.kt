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

package gg.skytils.skytilsmod.features.impl.handlers

import gg.essential.api.EssentialAPI
import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.ChildBasedMaxSizeConstraint
import gg.essential.elementa.constraints.ChildBasedRangeConstraint
import gg.essential.elementa.dsl.basicYConstraint
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.pixels
import gg.essential.universal.*
import gg.skytils.event.EventPriority
import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.network.ClientDisconnectEvent
import gg.skytils.event.impl.screen.ScreenDrawEvent
import gg.skytils.event.impl.screen.ScreenMouseInputEvent
import gg.skytils.event.impl.screen.ScreenOpenEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.failPrefix
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod._event.PacketReceiveEvent
import gg.skytils.skytilsmod.gui.components.SimpleButton
import gg.skytils.skytilsmod.mixins.extensions.ExtensionChatLine
import gg.skytils.skytilsmod.mixins.extensions.ExtensionChatStyle
import gg.skytils.skytilsmod.mixins.transformers.accessors.AccessorGuiChat
import gg.skytils.skytilsmod.mixins.transformers.accessors.AccessorGuiNewChat
import gg.skytils.skytilsmod.utils.*
import net.minecraft.client.gui.hud.ChatHudLine
import net.minecraft.client.gui.screen.ChatScreen
import net.minecraft.client.gui.hud.ChatHud
import net.minecraft.client.gui.screen.Screen
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket
import net.minecraft.text.Text
import java.awt.Color

object ChatTabs : EventSubscriber {
    var selectedTab = ChatTab.ALL
    var hoveredChatLine: ChatHudLine? = null

    override fun setup() {
        register(::onChat, EventPriority.Highest)
        register(::onOpenGui)
        register(::drawScreen)
        register(::mouseInput)
        register(::onDisconnect)
    }

    fun onChat(event: PacketReceiveEvent<*>) {
        if (!Utils.isOnHypixel || !Skytils.config.chatTabs || event.packet !is GameMessageS2CPacket || event.packet.type == 2.toByte()) return

        val style = event.packet.message.style
        style as ExtensionChatStyle
        if (style.chatTabType == null) {
            val cc = event.packet.message
            val formatted = cc.method_10865()
            style.chatTabType = ChatTab.entries.filter { it.isValid(cc, formatted) }.toTypedArray()
        }
    }

    @JvmStatic
    fun setBGColor(origColor: Int, line: ChatHudLine): Int {
        if (line != hoveredChatLine) return origColor
        return RenderUtil.mixColors(Color(origColor), Color.RED).rgb
    }

    fun shouldAllow(component: Text): Boolean {
        if (!Utils.isOnHypixel || !Skytils.config.chatTabs) return true
        val style = component.style
        style as ExtensionChatStyle
        if (style.chatTabType == null) {
            style.chatTabType =
                ChatTab.entries.filter { it.isValid(component, component.method_10865()) }.toTypedArray()
        }
        return style.chatTabType!!.contains(selectedTab)
    }

    fun onOpenGui(event: ScreenOpenEvent) {
        if (!Skytils.config.chatTabs || !Skytils.config.preFillChatTabCommands || !Utils.isOnHypixel || event.screen !is ChatScreen) return
        if ((event.screen as AccessorGuiChat).defaultInputFieldText.isBlank()) {
            (event.screen as AccessorGuiChat).defaultInputFieldText =
                when (selectedTab) {
                    ChatTab.ALL -> "/ac "
                    ChatTab.PARTY -> "/pc "
                    ChatTab.GUILD -> "/gc "
                    ChatTab.PRIVATE -> "/r "
                    ChatTab.COOP -> "/cc "
                }
        }
    }

    fun drawScreen(event: ScreenDrawEvent) {
        if (!Skytils.config.chatTabs || !Utils.isOnHypixel || event.screen !is ChatScreen) return
        ChatTab.screen.draw(UMatrixStack.Compat.get())
        UMinecraft.getChatGUI()?.let { chat ->
            hoveredChatLine =
                if (Skytils.config.copyChat && chat.isChatFocused) chat.getChatLine(event.mouseX, event.mouseY) else null
        }
    }

    fun mouseInput(event: ScreenMouseInputEvent) {
        if (!Utils.isOnHypixel || event.screen !is ChatScreen) return
        event.cancelled = event.cancelled || ChatTab.screen.clickMouse(event.mouseX.toDouble(), event.mouseY.toDouble(), event.button)
        val chat = mc.inGameHud.chatHud
        chat as AccessorGuiNewChat
        if (Screen.method_2238() && DevTools.getToggle("chat")) {
            if (event.button != 0 && event.button != 1) return
            val chatLine = hoveredChatLine ?: return
            if (event.button == 0) {
                val component = (chatLine as ExtensionChatLine).fullComponent ?: chatLine.text
                Screen.method_0_2797(component.method_10865())
                printDevMessage("Copied formatted message to clipboard!", "chat")
            } else {
                val component =
                    chat.chatLines.find {
                        it.text.string == ((chatLine as ExtensionChatLine).fullComponent
                            ?: chatLine.text).string
                    }?.text
                        ?: ((chatLine as ExtensionChatLine).fullComponent
                            ?: chatLine.text)

                printDevMessage("Copied serialized message to clipboard!", "chat")
                Screen.method_0_2797(
                    Text.Serializer.toJson(
                        component
                    )
                )
            }
        } else if (Skytils.config.copyChat) {
            if (event.button != 0) return
            val chatLine = hoveredChatLine ?: return
            val component = if (Screen.method_2238()) (chatLine as ExtensionChatLine).fullComponent
                ?: chatLine.text else if (Screen.method_2223()) chatLine.text else return
            Screen.method_0_2797(component.string.stripControlCodes())
            EssentialAPI.getNotifications()
                .push("Copied chat", component.string.stripControlCodes(), 1f)
        }
    }

    fun onDisconnect(event: ClientDisconnectEvent) {
        runCatching {
            mc.inGameHud.chatHud.reset()
        }.onFailure {
            it.printStackTrace()
            UChat.chat("$failPrefix §cSkytils ran into an error while refreshing chat tabs. Please send your logs on our Discord server at discord.gg/skytils!")
        }
    }

    enum class ChatTab(
        text: String,
        val isValid: (Text, String) -> Boolean = { _, _ -> true }
    ) {
        ALL("A"),
        PARTY("P", { _, formatted ->
            formatted.startsWith("§r§9Party §8> ") ||
                    formatted.startsWith("§r§9P §8> ") ||
                    formatted.endsWith("§r§ehas invited you to join their party!") ||
                    formatted.endsWith("§r§eto the party! They have §r§c60 §r§eseconds to accept.§r") ||
                    formatted == "§cThe party was disbanded because all invites expired and the party was empty§r" ||
                    formatted.endsWith("§r§ehas disbanded the party!§r") ||
                    formatted.endsWith("§r§ehas disconnected, they have §r§c5 §r§eminutes to rejoin before they are removed from the party.§r") ||
                    formatted.endsWith(" §r§ejoined the party.§r") ||
                    formatted.endsWith(" §r§ehas left the party.§r") ||
                    formatted.endsWith(" §r§ehas been removed from the party.§r") ||
                    formatted.startsWith("§eThe party was transferred to §r") ||
                    (formatted.startsWith("§eKicked §r") && formatted.endsWith("§r§e because they were offline.§r"))
        }),
        GUILD("G", { _, formatted ->
            formatted.startsWith("§r§2Guild > ") || formatted.startsWith("§r§2G > ")
        }),
        PRIVATE("PM", { _, formatted ->
            formatted.startsWith("§dTo ") || formatted.startsWith("§dFrom ")
        }),
        COOP("CC", { _, formatted ->
            formatted.startsWith("§r§bCo-op > ")
        });

        val button = SimpleButton(text).constrain {
            x = (22 * ordinal).pixels
            width = 20.pixels
            height = 20.pixels
        }.onMouseClick { event ->
            if (selectedTab == this@ChatTab) return@onMouseClick
            event.stopPropagation()
            USound.playButtonPress()
            selectedTab = this@ChatTab
            val chat = UMinecraft.getChatGUI() ?: return@onMouseClick
            chat as AccessorGuiNewChat
            runCatching {
                chat.reset()
            }.onFailure { e ->
                e.printStackTrace()
                UChat.chat("$failPrefix §cSkytils ran into an error while refreshing chat tabs. Please send your logs on our Discord server at discord.gg/skytils!")
                chat.drawnChatLines.clear()
                chat.resetScroll()
                for (line in chat.chatLines.asReversed()) {
                    if (line?.text == null) continue
                    chat.invokeSetChatLine(
                        line.text,
                        line.id,
                        line.creationTick,
                        true
                    )
                }
            }
            if (Skytils.config.autoSwitchChatChannel) {
                Skytils.sendMessageQueue.addFirst(
                    when (selectedTab) {
                        ALL -> "/chat a"
                        PARTY -> "/chat p"
                        GUILD -> "/chat g"
                        COOP -> "/chat coop"
                        else -> ""
                    }
                )
            }
        }

        companion object {
            val screen = Window(ElementaVersion.V5)
            private val container = UIContainer().constrain {
                x = 2.pixels
                y = basicYConstraint { UResolution.scaledHeight - calculateChatHeight().toFloat() - 30f /* bottom bit */ - 20f /* height */ - 5f /* padding */ }
                height = ChildBasedMaxSizeConstraint()
                width = ChildBasedRangeConstraint()
            } childOf screen

            init {
                entries.forEach { tab ->
                    tab.button childOf container
                }
            }

            private fun calculateChatHeight() =
                UMinecraft.getChatGUI()?.let { chat ->
                    chat.height.coerceAtMost((chat as AccessorGuiNewChat).drawnChatLines.size * UMinecraft.getFontRenderer().field_0_2811)
                } ?: 0
        }
    }
}

fun ChatHud.getChatLine(mouseX: Double, mouseY: Double): ChatHudLine? {
    if (isChatFocused && this is AccessorGuiNewChat) {
        val extraOffset =
            if (
                ReflectionHelper.getFieldFor("club.sk1er.patcher.config.PatcherConfig", "chatPosition")
                    ?.getBoolean(null) == true
            ) 12 else 0
        val x = ((mouseX - 3) / chatScale).toInt()
        val y = (((UResolution.scaledHeight - mouseY) - 30 - extraOffset) / chatScale).toInt()

        if (x >= 0 && y >= 0) {
            val l = visibleLineCount.coerceAtMost(drawnChatLines.size)
            if (x <= width / chatScale && y < UGraphics.getFontHeight() * l + l) {
                val lineNum = y / UGraphics.getFontHeight() + scrollPos
                return drawnChatLines.getOrNull(lineNum)
            }
        }
    }
    return null
}