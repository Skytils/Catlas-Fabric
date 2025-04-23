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

import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.screen.GuiContainerPostDrawSlotEvent
import gg.skytils.event.impl.screen.GuiContainerSlotClickEvent
import gg.skytils.event.impl.screen.ScreenOpenEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod._event.PacketReceiveEvent
import gg.skytils.skytilsmod.utils.ItemUtil.getSkyBlockItemID
import gg.skytils.skytilsmod.utils.Utils
import gg.skytils.skytilsmod.utils.stripControlCodes
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.item.Items
import net.minecraft.screen.GenericContainerScreenHandler
import net.minecraft.network.packet.s2c.play.PlaySoundIdS2CPacket

object MinionFeatures : EventSubscriber {

    private var blockUnenchanted = false

    private val minionRegex = Regex("(?<type>[A-Z_]+)_GENERATOR_(?<tier>\\d+)")

    override fun setup() {
        register(::onGuiOpen)
        register(::onReceivePacket)
        register(::onSlotClick)
        register(::onRenderItemOverlayPost)
    }

    fun onGuiOpen(event: ScreenOpenEvent) {
        if (event.screen is GenericContainerScreen) {
            val chest = (event.screen as GenericContainerScreen).handler as GenericContainerScreenHandler
            val chestName = chest.inventory.customName.string.trim()
            if (chestName == "Minion Chest") return
        }
        blockUnenchanted = false
    }

    fun onReceivePacket(event: PacketReceiveEvent<*>) {
        if (!Utils.inSkyblock) return
        if (event.packet is PlaySoundIdS2CPacket) {
            val packet = event.packet
            if (packet.method_11460() == "random.chestopen" && packet.pitch == 1f && packet.volume == 1f) {
                blockUnenchanted = false
            }
        }
    }

    fun onSlotClick(event: GuiContainerSlotClickEvent) {
        if (!Utils.inSkyblock) return
        if (event.container is GenericContainerScreenHandler) {
            val chest = event.container as GenericContainerScreenHandler
            val inventory = chest.inventory
            val slot = event.slot ?: return
            val item = slot.stack
            if (Skytils.config.onlyCollectEnchantedItems && event.chestName.contains("Minion") && item != null) {
                if (!item.hasEnchantments() && item.item != Items.PLAYER_HEAD) {
                    if (event.chestName == "Minion Chest") {
                        if (!blockUnenchanted) {
                            for (i in 0..<inventory.size()) {
                                val stack = inventory.getStack(i) ?: continue
                                if (stack.hasEnchantments() || stack.item == Items.PLAYER_HEAD) {
                                    blockUnenchanted = true
                                    break
                                }
                            }
                        }
                        if (blockUnenchanted && slot.inventory !== mc.player.inventory) event.cancelled = true
                    } else {
                        val minionType = inventory.getStack(4)
                        if (minionType != null) {
                            if (minionType.name.stripControlCodes().contains("Minion")) {
                                if (!blockUnenchanted) {
                                    val firstUpgrade = inventory.getStack(37)
                                    val secondUpgrade = inventory.getStack(46)
                                    if (firstUpgrade != null) {
                                        if (firstUpgrade.name.stripControlCodes()
                                                .contains("Super Compactor")
                                        ) {
                                            blockUnenchanted = true
                                        }
                                    }
                                    if (secondUpgrade != null) {
                                        if (secondUpgrade.name.stripControlCodes()
                                                .contains("Super Compactor")
                                        ) {
                                            blockUnenchanted = true
                                        }
                                    }
                                }
                                val index = slot.slotIndex
                                if (blockUnenchanted && slot.inventory !== mc.player.inventory && index >= 21 && index <= 43 && index % 9 >= 3 && index % 9 <= 7) {
                                    event.cancelled = true
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun onRenderItemOverlayPost(event: GuiContainerPostDrawSlotEvent) {
        val item = event.slot.stack ?: return
        if (!Utils.inSkyblock || item.count != 1 || item.nbt?.contains("SkytilsNoItemOverlay") == true) return
        val sbId = getSkyBlockItemID(item) ?: return
        if (Skytils.config.showMinionTier) {
            val matrixStack = UMatrixStack.Compat.get()
            val s = minionRegex.find(sbId)?.groups?.get("tier")?.value ?: return
            RenderSystem.method_4406()
            RenderSystem.disableDepthTest()
            RenderSystem.disableBlend()
            UGraphics.drawString(
                matrixStack,
                s,
                (event.slot.x + 17 - UGraphics.getStringWidth(s)).toFloat(),
                (event.slot.y + 9).toFloat(),
                16777215,
                true
            )
            RenderSystem.method_4394()
            RenderSystem.enableDepthTest()
        }
    }
}