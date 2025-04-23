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

package gg.skytils.skytilsmod.features.impl.farming

import gg.essential.universal.UMatrixStack
import gg.essential.universal.UResolution
import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.screen.GuiContainerBackgroundDrawnEvent
import gg.skytils.event.impl.screen.GuiContainerCloseWindowEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod.core.structure.GuiElement
import gg.skytils.skytilsmod.core.tickTimer
import gg.skytils.skytilsmod.features.impl.handlers.AuctionData
import gg.skytils.skytilsmod.features.impl.misc.ContainerSellValue
import gg.skytils.skytilsmod.features.impl.misc.ItemFeatures
import gg.skytils.skytilsmod.utils.*
import gg.skytils.skytilsmod.utils.graphics.ScreenRenderer
import gg.skytils.skytilsmod.utils.graphics.SmartFontRenderer
import gg.skytils.skytilsmod.utils.graphics.colors.CommonColors
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.screen.GenericContainerScreenHandler
import net.minecraft.item.ItemStack

object VisitorHelper : EventSubscriber {
    private val inGarden
        get() = Utils.inSkyblock && SBInfo.mode == SkyblockIsland.TheGarden.mode

    private val requiredItemRegex = Regex("^\\s+(?<formattedItemName>.+?)( §8x(?<quantity>[\\d,]+))?\$")
    private val rewardRegex = Regex("^\\s+§8\\+(?<reward>§.+)\$")
    private val copperRewardRegex = Regex("^§c(?<count>[\\d,]+) Copper\$")
    private val textLines = mutableListOf<String>()
    private var totalItemCost: Double = 0.0

    override fun setup() {
        register(::onGuiClose)
        register(::onBackgroundDrawn)
    }

    fun onGuiClose(event: GuiContainerCloseWindowEvent) {
        textLines.clear()
        totalItemCost = 0.0
    }

    init {
        tickTimer(4, repeats = true) {
            if (!Skytils.config.visitorOfferHelper) return@tickTimer

            textLines.clear()
            totalItemCost = 0.0

            if (!inGarden) return@tickTimer

            val container = (mc.currentScreen as? GenericContainerScreen)?.handler as? GenericContainerScreenHandler ?: return@tickTimer
            val chestName = container.inventory.name
            val npcSummary: ItemStack? = container.getSlot(13).stack
            val acceptOffer: ItemStack? = container.getSlot(29).stack
            if (npcSummary?.name.stripControlCodes() == chestName.stripControlCodes() && acceptOffer?.name == "§aAccept Offer") {
                val lore = ItemUtil.getItemLore(acceptOffer)
                var copper = 0


                textLines.add("§eRewards:")

                val rewardIndex = lore.indexOf("§7Rewards:")
                if (rewardIndex == -1) return@tickTimer

                lore.drop(rewardIndex + 1)
                    .takeWhile { it != "" }
                    .map { rewardRegex.find(it)?.groups?.get("reward")?.value ?: it.trim() }
                    .forEach { line ->
                        textLines.add(line)
                        copperRewardRegex.find(line)?.also {
                            copper += it.groups["count"]!!.value.replace(",", "").toInt()
                        }
                    }

                textLines.add("")

                textLines.add("§eNeeded Items:")
                lore.dropWhile { !requiredItemRegex.containsMatchIn(it) }
                    .takeWhile { requiredItemRegex.containsMatchIn(it) }.map { requiredItemRegex.find(it)!!.groups }
                    .forEach {
                        val formattedName = it["formattedItemName"]!!.value
                        val unformattedName = formattedName.stripControlCodes()
                        val itemId = ItemFeatures.itemIdToNameLookup.entries.find { it.value == unformattedName }?.key
                        val quantity = it["quantity"]?.value?.replace(",", "")?.toInt() ?: 1
                        val value = (AuctionData.lowestBINs[itemId] ?: 0.0) * quantity
                        textLines.add("$formattedName §8x$quantity - §a${NumberUtil.format(value)}")
                        totalItemCost += value
                    }
                if (totalItemCost > 0) {
                    textLines.add("§eTotal Cost: §a${NumberUtil.format(totalItemCost)}")

                    if (copper > 0) {
                        textLines.add("§eCoins/Copper: §a${NumberUtil.format(totalItemCost / copper)}")
                    }
                }
            }
        }
    }

    fun onBackgroundDrawn(event: GuiContainerBackgroundDrawnEvent) {
        if (textLines.isEmpty() || event.gui !is GenericContainerScreen) return
        val stack = UMatrixStack()
        stack.push()
        stack.translate(VisitorHelperDisplay.scaleX, VisitorHelperDisplay.scaleY, 0f)
        stack.scale(VisitorHelperDisplay.scale, VisitorHelperDisplay.scale, 0f)

        stack.runWithGlobalState {
            textLines.forEachIndexed { i, str -> drawLine(i, str) }
        }
        stack.pop()
    }


    /**
     * A `GuiElement` that shows the most valuable items in a container. The rendering of this element
     * takes place when the container background is drawn, so it doesn't render at the normal time.
     * Even though the GuiElement's render method isn't used, it is still worth having an instance
     * of this class so that the user can move the element around normally.
     * @see ContainerSellValue
     */
    object VisitorHelperDisplay : GuiElement("Visitor Offer Helper", x = 0.258f, y = 0.283f) {
        internal val rightAlign: Boolean
            get() = scaleX > (UResolution.scaledWidth * 0.75f) ||
                    (scaleX < UResolution.scaledWidth / 2f && scaleX > UResolution.scaledWidth / 4f)
        internal val textPosX: Float
            get() = if (rightAlign) width.toFloat() else 0f
        internal val alignment: SmartFontRenderer.TextAlignment
            get() = if (rightAlign) SmartFontRenderer.TextAlignment.RIGHT_LEFT
            else SmartFontRenderer.TextAlignment.LEFT_RIGHT

        override fun render() {
            // Rendering is handled in the BackgroundDrawnEvent to give the text proper lighting
            textShadow_ = textShadow
        }

        override fun demoRender() {
            listOf(
                "§aEnchanted Cocoa Bean §8x69 - §a900M",
                "§aEnchanted Potato §8x69 - §a1K",
                "§eTotal Value: §a900M"
            ).forEachIndexed { i, str ->
                fr.drawString(
                    str, textPosX, (i * fr.field_0_2811).toFloat(),
                    CommonColors.WHITE, alignment, textShadow
                )
            }
        }

        override val toggled: Boolean
            get() = Skytils.config.visitorOfferHelper
        override val height: Int
            get() = fr.field_0_2811 * 3
        override val width: Int
            get() = fr.getWidth("§aEnchanted Cocoa Bean §8x69 - §a900M")

        init {
            Skytils.guiManager.registerElement(this)
        }
    }

    private var textShadow_ = SmartFontRenderer.TextShadow.NORMAL
    private fun drawLine(index: Int, str: String) {
        ScreenRenderer.fontRenderer.drawString(
            str,
            VisitorHelperDisplay.textPosX,
            (index * ScreenRenderer.fontRenderer.field_0_2811).toFloat(),
            CommonColors.WHITE,
            VisitorHelperDisplay.alignment,
            textShadow_
        )
    }

    init {
        VisitorHelperDisplay
    }
}