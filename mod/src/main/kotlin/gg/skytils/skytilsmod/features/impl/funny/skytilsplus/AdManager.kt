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

package gg.skytils.skytilsmod.features.impl.funny.skytilsplus

import gg.essential.universal.utils.MCClickEventAction
import gg.essential.universal.wrappers.message.UTextComponent
import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.screen.GuiContainerBackgroundDrawnEvent
import gg.skytils.event.impl.screen.ScreenOpenEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod.core.SoundQueue
import gg.skytils.skytilsmod.core.tickTimer
import gg.skytils.skytilsmod.features.impl.dungeons.catlas.core.CatlasConfig
import gg.skytils.skytilsmod.features.impl.funny.skytilsplus.gui.PaywallGui
import gg.skytils.skytilsmod.gui.ReopenableGUI
import gg.skytils.skytilsmod.mixins.hooks.gui.addColor
import gg.skytils.skytilsmod.mixins.transformers.accessors.AccessorSettingsGui
import gg.skytils.skytilsmod.utils.RenderUtil
import gg.skytils.skytilsmod.utils.Utils
import gg.skytils.skytilsmod.utils.graphics.ScreenRenderer
import gg.skytils.skytilsmod.utils.graphics.SmartFontRenderer
import gg.skytils.skytilsmod.utils.graphics.colors.CommonColors
import gg.skytils.skytilsmod.utils.setHoverText
import net.minecraft.util.Identifier
import kotlin.random.Random

object AdManager : EventSubscriber {
    private val ad = Identifier("skytils:skytilsplus/codeskytils.png")
    private var lastAdBreak = -1L

    override fun setup() {
        register(::onGuiOpen)
        register(::onGuiDraw)
    }

    fun onGuiOpen(event: ScreenOpenEvent) {
        if (!Utils.isBSMod || SkytilsPlus.redeemed) return
        if (mc.currentScreen is PaywallGui) return
        if (event.screen is ReopenableGUI || (event.screen as? AccessorSettingsGui)?.config == CatlasConfig) {
            event.screen = PaywallGui(event.screen!!)
        }
    }

    fun onGuiDraw(event: GuiContainerBackgroundDrawnEvent) {
        if (!Utils.isBSMod || SkytilsPlus.redeemed) return
        RenderUtil.renderTexture(ad, 5, 5, 244, 307, false)
        ScreenRenderer.fontRenderer.drawString("Want a break from the ads? Get BSMod+ today!", 5f, 5 + 307 + 10f, CommonColors.RAINBOW, shadow = SmartFontRenderer.TextShadow.OUTLINE)

        if (System.currentTimeMillis() - lastAdBreak > 1000 * 60) {
            lastAdBreak = System.currentTimeMillis()
            SoundQueue.addToQueue("skytils:bsmod.sparkle_adbreak", 1f, isLoud = true)
        }
    }

    fun joinedSkyblock() {
        if (!Utils.isBSMod || SkytilsPlus.redeemed) return
        tickTimer(60) {
            UTextComponent(addColor("Your play session today is powered by BSMod! Click me to try BSMod+ today for free!".toCharArray().joinToString("") { if (Random.nextDouble() > .5) it.uppercase() else it.lowercase() }, 0))
                .setClick(MCClickEventAction.SUGGEST_COMMAND, "/bsmod+ redeem FREETRIAL")
                .setHoverText("Redeem your free trial today! No payment method necessary.").chat()
        }
    }
}