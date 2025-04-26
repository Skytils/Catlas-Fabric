/*
 * Skytils - Hypixel Skyblock Quality of Life Mod
 * Copyright (C) 2020-2025 Skytils
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

package gg.skytils.event.impl.play

import gg.skytils.event.CancellableEvent
import net.minecraft.text.Text

/**
 * [gg.skytils.event.mixins.network.MixinNetHandlerPlayClient.onChat]
 */
class ChatMessageReceivedEvent(var message: Text) : CancellableEvent()

/**
 * [gg.skytils.event.mixins.gui.MixinGuiScreen.onSendChatMessage]
 */
class ChatMessageSentEvent(val message: String, val addToHistory: Boolean) : CancellableEvent()

/**
 * [gg.skytils.event.mixins.network.MixinNetHandlerPlayClient.onActionbar]
 */
class ActionBarReceivedEvent(var message: Text) : CancellableEvent()