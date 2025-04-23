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

package gg.skytils.skytilsmod.features.impl.handlers

import gg.essential.universal.UChat
import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.play.ChatMessageSentEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod.mixins.transformers.accessors.AccessorCommandHandler
import gg.skytils.skytilsmod.utils.ObservableAddEvent
import gg.skytils.skytilsmod.utils.ObservableClearEvent
import gg.skytils.skytilsmod.utils.ObservableRemoveEvent
import gg.skytils.skytilsmod.utils.ObservableSet
import net.minecraft.class_0_1630
import net.minecraftforge.client.ClientCommandHandler
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.ModContainer

/**
 * Namespaced commands is a feature which generates namespaces for commands.
 *
 * For example, when a mod adds reparty, namespaced commands will generate
 * a command that includes the id/name of the mod
 *
 * `/mod:reparty`
 *
 * This is useful when multiple mods register a command with the same name
 */
object NamespacedCommands : EventSubscriber {
    val cch by lazy {
        ClientCommandHandler.instance as AccessorCommandHandler
    }

    val aliasMap = mutableMapOf<class_0_1630, String>()

    fun setup(listeningSet: ObservableSet<class_0_1630>) {
        listeningSet.addObserver { _, arg ->
            when (arg) {
                is ObservableAddEvent<*> -> {
                    registerCommandHelper(arg.element as class_0_1630)
                }
                is ObservableRemoveEvent<*> -> {
                    cch.commandMap.remove(aliasMap.remove(arg.element))
                }
                is ObservableClearEvent<*> -> {
                    aliasMap.entries.removeAll {
                        cch.commandMap.remove(it.value)
                        true
                    }
                }
            }
        }

        listeningSet.forEach {
            registerCommandHelper(it)
        }
    }

    /**
     * This method takes a command and registers the command's namespaced version.
     */
    fun registerCommandHelper(command: class_0_1630) {
        val owners = getCommandModOwner(command.javaClass)
        if (owners.size != 1) {
            println("WARNING! Command ${command.method_0_5966()} has ${owners.size}; owners: $owners")
        }

        val owner = owners.firstOrNull()

        val prefix = owner?.modId ?: owner?.name ?: "unknown"
        
        val helper = "${prefix}:${command.method_0_5966()}"
        cch.commandMap[helper] = command

        aliasMap[command] = helper
    }

    fun getCommandModOwner(command: Class<*>) : List<ModContainer> {
        val idx = command.name.lastIndexOf(".")
        if (idx == -1) return emptyList()
        val packageName = command.name.substring(0, idx)
        return Loader.instance().modList.filter { packageName in it.ownedPackages }
    }

    /**
     * Handles the actual sending of the command.
     *
     * When a command is sent using the `server` namespace, it is passed directly to the server
     * instead of running a client command.
     */
    fun onSendChat(event: ChatMessageSentEvent) {
        if (event.message.startsWith("/server:")) {
            event.cancelled = true
            UChat.say('/' + event.message.substringAfter("/server:"))
            mc.inGameHud.chatHud.addToMessageHistory(event.message)
        }
    }

    override fun setup() {
        register(::onSendChat)
    }
}