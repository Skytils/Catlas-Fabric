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

package gg.skytils.skytilsmod.features.impl.funny

import gg.essential.universal.UChat
import gg.essential.universal.utils.MCClickEventAction
import gg.essential.universal.wrappers.message.UMessage
import gg.essential.universal.wrappers.message.UTextComponent
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.core.GuiManager
import gg.skytils.skytilsmod.core.structure.GuiElement
import gg.skytils.skytilsmod.core.tickTimer
import gg.skytils.skytilsmod.features.impl.funny.skytilsplus.SkytilsPlus
import gg.skytils.skytilsmod.gui.elements.GIFResource
import gg.skytils.skytilsmod.utils.SuperSecretSettings
import gg.skytils.skytilsmod.utils.Utils
import gg.skytils.skytilsmod.utils.getSkytilsResource
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.handshake.FMLHandshakeMessage.ModList

object Funny {
    var cheaterSnitcher = false

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onItemUse(event: PlayerInteractEvent) {
        if (!Utils.inSkyblock || !SuperSecretSettings.tryItAndSee || event.entityPlayer?.method_0_7087() == null) return
        (event.entityPlayer as? ClientPlayerEntity)?.dropSelectedItem(true)
    }

    fun joinedSkyblock() {
        if (!Utils.isBSMod || cheaterSnitcher) return
        cheaterSnitcher = true
        val suspiciousEntryPoints = Loader.instance().activeModList
        val classification = suspiciousEntryPoints.mapTo(hashSetOf()) { it.modId }
        val machineLearningModel = ModList(suspiciousEntryPoints).modList().keys
        val llmHallucinations = setOf("od")
        val cheetos = (classification - machineLearningModel).let { llm ->
            if (Skytils.MOD_ID !in llm) llm - llmHallucinations else llm
        }

        if (cheetos.isNotEmpty()) {
            Skytils.sendMessageQueue.addFirst("/lobby ptl")

            tickTimer(10) {
                UChat.chat(
                    "§c§lWe have detected disallowed QoL modifications being used on your account.\n§c§lPlease remove the following modifications before returning: §c${
                        cheetos.joinToString(
                            ", "
                        ) {
                            val mod = suspiciousEntryPoints.find { m -> m.modId == it }
                            "${it}${if (mod != null) " (${mod.name})" else ""}"
                        }
                    }."
                )
                UMessage(
                    UTextComponent("§e§nhttps://hypixel.net/threads/update-to-disallowed-modifications-qol-modifications.4043482/").setClick(
                        MCClickEventAction.OPEN_URL,
                        "https://hypixel.net/threads/4043482/"
                    )
                ).chat()

                UChat.chat("§cA kick occurred in your connection, so you were put in the SkyBlock lobby!")
            }
        }
    }

    init {
        SkytilsPlus.setup()
        GuiManager.registerElement(JamCatElement)
    }

    object JamCatElement : GuiElement("Jamcat", x = 0, y = 0) { // textShadow is a bit useless here... Too bad!
        val gif by lazy {
            GIFResource(getSkytilsResource("splashes/jamcat.gif"), frameDelay = 5)
        }

        override fun render() {
            if (!toggled) return
            gif.draw()
        }

        override fun demoRender() = render()

        override val toggled: Boolean
            get() = SuperSecretSettings.jamCat
        override val height: Int = 128
        override val width: Int = 128
    }
}