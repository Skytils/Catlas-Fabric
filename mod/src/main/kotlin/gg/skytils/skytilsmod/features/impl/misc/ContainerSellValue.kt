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
import gg.essential.universal.UResolution
import gg.skytils.event.EventPriority
import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.screen.GuiContainerCloseWindowEvent
import gg.skytils.event.impl.screen.ScreenDrawEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod.core.structure.GuiElement
import gg.skytils.skytilsmod.core.tickTimer
import gg.skytils.skytilsmod.features.impl.handlers.AuctionData
import gg.skytils.skytilsmod.mixins.hooks.item.masterStarRegex
import gg.skytils.skytilsmod.mixins.hooks.item.masterStars
import gg.skytils.skytilsmod.utils.*
import gg.skytils.skytilsmod.utils.Utils.inDungeons
import gg.skytils.skytilsmod.utils.graphics.ScreenRenderer
import gg.skytils.skytilsmod.utils.graphics.SmartFontRenderer
import gg.skytils.skytilsmod.utils.graphics.colors.CommonColors
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.screen.GenericContainerScreenHandler
import net.minecraft.item.ItemStack
import java.awt.Color
import kotlin.math.roundToLong

/**
 * Provides the functionality for an overlay that is rendered on top of chests, minions, ender chest pages,
 * and backpacks that shows the top items in the container by value and the container's total value.
 * @author FluxCapacitor2
 */
object ContainerSellValue : EventSubscriber {

    private val element = SellValueDisplay()

    /**
     * Represents a line in the sell value display. To conserve space, multiple items can
     * be stacked into one line if their display names are equal, so all the original
     * `ItemStack`s must be saved to find the lowest BIN price of this display line.
     */
    private class DisplayLine(itemStack: ItemStack) {
        val stackedItems = mutableListOf(itemStack)
        val lowestBIN: Double
            get() = stackedItems.sumOf(::getItemValue)
        val amount: Int
            get() = stackedItems.sumOf { it.count }

        fun shouldDisplay(): Boolean = this.lowestBIN != 0.0
    }

    /**
     * A `GuiElement` that shows the most valuable items in a container. The rendering of this element
     * takes place when the container background is drawn, so it doesn't render at the normal time.
     * Even though the GuiElement's render method isn't used, it is still worth having an instance
     * of this class so that the user can move the element around normally.
     * @see renderGuiComponent
     */
    class SellValueDisplay : GuiElement("Container Sell Value", x = 0.258f, y = 0.283f) {

        internal val rightAlign: Boolean
            get() = scaleX > (UResolution.scaledWidth * 0.75f) ||
                    (scaleX < UResolution.scaledWidth / 2f && scaleX > UResolution.scaledWidth / 4f)
        internal val textPosX: Float
            get() = if (rightAlign) scaleWidth else 0f
        internal val alignment: SmartFontRenderer.TextAlignment
            get() = if (rightAlign) SmartFontRenderer.TextAlignment.RIGHT_LEFT
            else SmartFontRenderer.TextAlignment.LEFT_RIGHT

        override fun render() {
            // Rendering is handled in the BackgroundDrawnEvent to give the text proper lighting
        }

        override fun demoRender() {
            listOf(
                "§cDctr's Space Helmet§8 - §a900M",
                "§fDirt §7x64 §8 - §a1k",
                "§eTotal Value: §a900M"
            ).forEachIndexed { i, str ->
                fr.drawString(
                    str, textPosX, (i * fr.field_0_2811).toFloat(),
                    CommonColors.WHITE, alignment, textShadow
                )
            }
        }

        override val toggled: Boolean
            get() = Skytils.config.containerSellValue
        override val height: Int
            get() = fr.field_0_2811 * 3
        override val width: Int
            get() = fr.getWidth("Dctr's Space Helmet - 900M")

        init {
            Skytils.guiManager.registerElement(this)
        }
    }

    private fun isChestNameValid(chestName: String): Boolean {
        return (!inDungeons && (chestName == "Chest" || chestName == "Large Chest"))
                || chestName.contains("Backpack")
                || chestName.startsWith("Ender Chest (")
                || (chestName.contains("Minion") && !chestName.contains("Recipe") && SBInfo.mode == SkyblockIsland.PrivateIsland.mode)
                || chestName == "Personal Vault"
    }

    private val npcSellingBooks = mapOf(
        "ENCHANTED_BOOK-TELEKINESIS-1" to 100.0,
        "ENCHANTED_BOOK-TRUE_PROTECTION-1" to 900000.0
    )

    fun getItemValue(itemStack: ItemStack): Double {
        val identifier = AuctionData.getIdentifier(itemStack) ?: return 0.0
        val basePrice = minOf(
            maxOf(
                AuctionData.lowestBINs[identifier] ?: 0.0,
                ItemFeatures.sellPrices[identifier] ?: 0.0
            ), npcSellingBooks[identifier] ?: Double.MAX_VALUE
        )

        if (itemStack.count > 1 || !Skytils.config.includeModifiersInSellValue) return basePrice * itemStack.count

        // Never add modifiers to randomized dungeon items
        if (ItemUtil.isSalvageable(itemStack) || ItemUtil.getSkyBlockItemID(itemStack) == "ICE_SPRAY_WAND") return basePrice

        val extraAttrs = ItemUtil.getExtraAttributes(itemStack) ?: return basePrice
        val recombValue =
            if (extraAttrs.contains("rarity_upgrades")) AuctionData.lowestBINs["RECOMBOBULATOR_3000"] ?: 0.0 else 0.0

        val hpbCount = extraAttrs.getInt("hot_potato_count")
        val hpbValue = if (hpbCount > 0) (1..hpbCount).sumOf {
            AuctionData.lowestBINs[if (it >= 10) "FUMING_POTATO_BOOK" else "HOT_POTATO_BOOK"] ?: 0.0
        } else 0.0

        val enchantments = if (!identifier.startsWith("ENCHANTED_BOOK-"))
            extraAttrs.getCompound("enchantments") else null
        val enchantValue = enchantments?.keys?.sumOf {
            val id = "ENCHANTED_BOOK-${it.uppercase()}-${enchantments.getInt(it)}"
            (AuctionData.lowestBINs[id] ?: 0.0).coerceAtMost(npcSellingBooks[id] ?: Double.MAX_VALUE)
        } ?: 0.0

        val masterStarCount =
            if (itemStack.name?.contains("✪") == true) masterStarRegex.find(itemStack.name)?.destructured?.let { (tier) ->
                masterStars.indexOf(tier) + 1
            } ?: 0 else 0
        val masterStarValue = if (masterStarCount > 0) (1..masterStarCount).sumOf { i ->
            AuctionData.lowestBINs[listOf("FIRST", "SECOND", "THIRD", "FOURTH", "FIFTH")[i - 1] + "_MASTER_STAR"] ?: 0.0
        } else 0.0

        val artOfWarValue =
            extraAttrs.getInt("art_of_war_count") * (AuctionData.lowestBINs["THE_ART_OF_WAR"] ?: 0.0)

        val farmingForDummiesValue =
            extraAttrs.getInt("farming_for_dummies_count") * (AuctionData.lowestBINs["FARMING_FOR_DUMMIES"] ?: 0.0)

        val artOfPeaceValue =
            AuctionData.lowestBINs["THE_ART_OF_PEACE"].takeIf { extraAttrs.getBoolean("artOfPeaceApplied") } ?: 0.0

        return basePrice + enchantValue + recombValue + hpbValue + masterStarValue + artOfWarValue + farmingForDummiesValue + artOfPeaceValue
    }

    private val ItemStack.prettyDisplayName: String
        get() {
            val extraAttr = ItemUtil.getExtraAttributes(this) ?: return this.name
            if (ItemUtil.getSkyBlockItemID(extraAttr) == "ENCHANTED_BOOK" && extraAttr.contains("enchantments")) {
                val enchants = extraAttr.getCompound("enchantments")
                if (enchants.keys.size == 1) {
                    return ItemUtil.getItemLore(this).first()
                }
            }
            return this.name
        }

    private val textLines = mutableListOf<String>()
    private var totalContainerValue: Double = 0.0
    private var lines: Int = 0

    private fun shouldRenderGuiComponent(): Boolean {
        val container = (mc.currentScreen as? GenericContainerScreen)?.handler as? GenericContainerScreenHandler ?: return false
        val chestName = container.inventory.name

        // Ensure that the gui element should be shown for the player's open container
        return Skytils.config.containerSellValue && textLines.isNotEmpty() && isChestNameValid(chestName) && totalContainerValue > 0.0
    }

    /**
     * Compatibility with NEU's storage overlay
     */
    fun onGuiScreenDrawPre(event: ScreenDrawEvent) {
        if (event.cancelled && NEUCompatibility.isStorageMenuActive && shouldRenderGuiComponent()) {
            // NEU cancels this event when it renders the storage overlay,
            // which means that the BackgroundDrawnEvent isn't called.
            renderGuiComponent()
        }
    }

    /**
     * Standard rendering of the gui component when NEU's storage overlay is not active.
     *
     * Note: It is important that Minecraft Forge's `BackgroundDrawnEvent` is used instead of Skytils's
     * GuiContainerEvent.BackgroundDrawnEvent because Skytils's event is called before the overlay rectangle is drawn,
     * causing the text to be dimmed.
     */
    fun onPostBackgroundDrawn(event: ScreenDrawEvent) {
        if (!NEUCompatibility.isStorageMenuActive && shouldRenderGuiComponent()) renderGuiComponent()
    }

    /**
     * Renders the sell value overlay.
     * @see onPostBackgroundDrawn
     * @see onGuiScreenDrawPre
     */
    fun renderGuiComponent() {
        // Translate and scale manually because we're not rendering inside the GuiElement#render() method
        val stack = UMatrixStack()
        stack.push()
        stack.translate(element.scaleX, element.scaleY, 0f)
        stack.scale(element.scale, element.scale, 0f)

        textLines.forEachIndexed { i, str -> drawLine(stack, i, str) }
        if (lines > Skytils.config.containerSellValueMaxItems) {
            drawLine(stack, textLines.size, "§7and ${lines - Skytils.config.containerSellValueMaxItems} more...")
            drawLine(stack, textLines.size + 1, "§eTotal Value: §a${NumberUtil.format(totalContainerValue)}")
        } else {
            drawLine(stack, textLines.size, "§eTotal Value: §a${NumberUtil.format(totalContainerValue)}")
        }

        stack.pop()
    }

    /**
     * Clear the cached display items so that they don't briefly appear when opening another GUI before being updated.
     */
    fun onGuiClose(event: GuiContainerCloseWindowEvent) {
        totalContainerValue = 0.0
        textLines.clear()
    }

    /**
     * Update the list of items in the GUI to be displayed after the container background is drawn.
     */
    init {
        tickTimer(4, repeats = true) {
            if (!Skytils.config.containerSellValue) return@tickTimer


            val container = (mc.currentScreen as? GenericContainerScreen)?.handler as? GenericContainerScreenHandler ?: return@tickTimer
            val chestName = container.inventory.name

            if (!isChestNameValid(chestName)) return@tickTimer

            val isMinion = chestName.contains(" Minion ")

            // Map all of the items in the chest to their lowest BIN prices
            val slots = container.slots.filter {
                it.hasStack() && it.inventory != mc.player.inventory
                        && (!isMinion || it.id % 9 != 1) // Ignore minion upgrades and fuels
            }

            // Combine items with the same name to save space in the GUI
            val distinctItems = mutableMapOf<String, DisplayLine>()
            slots.forEach {
                if (distinctItems.containsKey(it.stack.prettyDisplayName)) {
                    distinctItems[it.stack.prettyDisplayName]!!.stackedItems.add(it.stack)
                } else {
                    distinctItems[it.stack.prettyDisplayName] = DisplayLine(it.stack)
                }
            }

            totalContainerValue = distinctItems.entries.sumOf { it.value.lowestBIN }

            // Sort the items from most to least valuable and convert them into a readable format
            textLines.clear()
            if (distinctItems.isEmpty() || totalContainerValue == 0.0) return@tickTimer
            textLines.addAll(
                distinctItems.entries.asSequence()
                    .sortedByDescending { (_, displayItem) -> displayItem.lowestBIN }
                    .filter { it.value.shouldDisplay() }
                    .map { (itemName, displayItem) ->
                        "$itemName§r${
                            (" §7x${displayItem.amount}").toStringIfTrue(displayItem.amount > 1)
                        }§8 - §a${NumberUtil.format(displayItem.lowestBIN.roundToLong())}"
                    }
                    .toList()
                    .also { lines = it.size }
                    .take(Skytils.config.containerSellValueMaxItems)
            )
        }
    }

    private fun drawLine(matrixStack: UMatrixStack, index: Int, str: String) {
        UGraphics.drawString(
            matrixStack, str,
            if (element.rightAlign) element.textPosX - UGraphics.getStringWidth(str) else element.textPosX,
            (index * ScreenRenderer.fontRenderer.field_0_2811).toFloat(),
            Color.WHITE.rgb, true
        )
    }

    override fun setup() {
        register(::onGuiScreenDrawPre, EventPriority.Lowest)
        register(::onPostBackgroundDrawn)
        register(::onGuiClose)
    }
}