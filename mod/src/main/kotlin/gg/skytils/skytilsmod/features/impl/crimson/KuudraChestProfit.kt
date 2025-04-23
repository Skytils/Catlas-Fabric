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
package gg.skytils.skytilsmod.features.impl.crimson

import gg.essential.universal.UResolution
import gg.skytils.event.EventPriority
import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.play.WorldUnloadEvent
import gg.skytils.event.impl.screen.GuiContainerForegroundDrawnEvent
import gg.skytils.event.impl.screen.GuiContainerSlotClickEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.IO
import gg.skytils.skytilsmod.core.MC
import gg.skytils.skytilsmod.core.structure.GuiElement
import gg.skytils.skytilsmod.features.impl.handlers.AuctionData
import gg.skytils.skytilsmod.features.impl.handlers.KuudraPriceData
import gg.skytils.skytilsmod.mixins.transformers.accessors.AccessorGuiContainer
import gg.skytils.skytilsmod.utils.*
import gg.skytils.skytilsmod.utils.graphics.ScreenRenderer
import gg.skytils.skytilsmod.utils.graphics.SmartFontRenderer
import gg.skytils.skytilsmod.utils.graphics.SmartFontRenderer.TextAlignment
import gg.skytils.skytilsmod.utils.graphics.colors.CommonColors
import gg.skytils.skytilsmod.utils.graphics.colors.CustomColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.screen.GenericContainerScreenHandler
import net.minecraft.item.ItemStack
import java.util.TreeSet


/**
 * Modified version of [gg.skytils.skytilsmod.features.impl.dungeons.DungeonChestProfit]
 */
object KuudraChestProfit : EventSubscriber {
    private val element = KuudraChestProfitElement()
    private val essenceRegex = Regex("§d(?<type>\\w+) Essence §8x(?<count>\\d+)")

    override fun setup() {
        register(::onGUIDrawnEvent)
        register(::onWorldChange)
        register(::onSlotClick, EventPriority.Highest)
    }

    fun onGUIDrawnEvent(event: GuiContainerForegroundDrawnEvent) {
        if (!Skytils.config.kuudraChestProfit || !KuudraFeatures.kuudraOver || KuudraFeatures.myFaction == null) return
        val inv = (event.container as? GenericContainerScreenHandler ?: return).inventory

        if (event.chestName.endsWith(" Chest")) {
            val chestType = KuudraChest.getFromName(event.chestName) ?: return
            val openChest = inv.getStack(31) ?: return
            if (openChest.name == "§aOpen Reward Chest" && chestType.items.isEmpty()) {
                runCatching {
                    val key = getKeyNeeded(ItemUtil.getItemLore(openChest))
                    chestType.keyNeeded = key
                    for (i in 9..17) {
                        val lootSlot = inv.getStack(i) ?: continue
                        chestType.addItem(lootSlot)
                    }
                    if (key != null && Skytils.config.kuudraChestProfitCountsKey) {
                        val faction = KuudraFeatures.myFaction ?: error("Failed to get Crimson Faction")
                        val keyCost = key.getPrice(faction)
                        chestType.items.add(KuudraChestLootItem(1, "${key.rarity.baseColor}${key.displayName} §7(${faction.color}${faction.identifier}§7)", -keyCost))
                        chestType.value -= keyCost
                    }
                }
            }
            RenderSystem.pushMatrix()
            RenderSystem.method_4412(
                (-(event.gui as AccessorGuiContainer).guiLeft).toDouble(),
                -(event.gui as AccessorGuiContainer).guiTop.toDouble(),
                299.0
            )
            drawChestProfit(chestType)
            RenderSystem.popMatrix()
        }
    }

    private fun getKeyNeeded(lore: List<String>): KuudraKey? {
        for (i in 0..<lore.size-1) {
            val line = lore[i]
            if (line == "§7Cost") {
                val cost = lore[i+1]
                if (cost == "§aThis Chest is Free!") return null
                return KuudraKey.entries.first { it.displayName == cost.stripControlCodes() }
            }
        }
        error("Could not find key needed for chest")
    }

    private fun getEssenceValue(text: String): Double? {
        if (!Skytils.config.kuudraChestProfitIncludesEssence) return null
        val groups = essenceRegex.matchEntire(text)?.groups ?: return null
        val type = groups["type"]?.value?.uppercase() ?: return null
        val count = groups["count"]?.value?.toInt() ?: return null
        return (AuctionData.lowestBINs["ESSENCE_$type"] ?: 0.0) * count
    }

    private fun drawChestProfit(chest: KuudraChest) {
        if (chest.items.size > 0) {
            val leftAlign = element.scaleX < UResolution.scaledWidth / 2f
            val alignment = if (leftAlign) TextAlignment.LEFT_RIGHT else TextAlignment.RIGHT_LEFT
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
            RenderSystem.method_4406()
            var drawnLines = 1

            ScreenRenderer.fontRenderer.drawString(
                chest.displayText + "§f: §" + (if (chest.value > 0) "a" else "c") + NumberUtil.nf.format(
                    chest.value
                ),
                if (leftAlign) element.scaleX else element.scaleX + element.width,
                element.scaleY,
                chest.displayColor,
                alignment,
                textShadow_
            )

            for (item in chest.items) {
                val line = "§8${item.stackSize} §r".toStringIfTrue(item.stackSize > 1) + item.displayText + "§f: §${if (item.value >= 0) 'a' else 'c'}" + NumberUtil.nf.format(item.value)
                ScreenRenderer.fontRenderer.drawString(
                    line,
                    if (leftAlign) element.scaleX else element.scaleX + element.width,
                    element.scaleY + drawnLines * ScreenRenderer.fontRenderer.field_0_2811,
                    CommonColors.WHITE,
                    alignment,
                    textShadow_
                )
                drawnLines++
            }
        }
    }

    fun onWorldChange(event: WorldUnloadEvent) {
        KuudraChest.entries.forEach(KuudraChest::reset)
    }

    fun onSlotClick(event: GuiContainerSlotClickEvent) {
        if (SBInfo.mode == SkyblockIsland.KuudraHollow.mode || event.container !is GenericContainerScreenHandler) return
        if (event.slotId in 9..17 && event.chestName.endsWith(" Chest") && KuudraChest.getFromName(event.chestName) != null) {
            event.cancelled = true
        }
    }

    private enum class KuudraChest(var displayText: String, var displayColor: CustomColor) {
        FREE("Free Chest", CommonColors.RED),
        PAID("Paid Chest", CommonColors.GREEN);

        var keyNeeded: KuudraKey? = null
        var value = 0.0
        val items = TreeSet<KuudraChestLootItem>().descendingSet()

        fun reset() {
            keyNeeded = null
            value = 0.0
            items.clear()
        }

        fun addItem(item: ItemStack) {
            IO.launch {
                val identifier = AuctionData.getIdentifier(item)
                val extraAttr = ItemUtil.getExtraAttributes(item)
                var displayName = item.name

                val itemValue = if (identifier == null) {
                    getEssenceValue(item.name) ?: return@launch
                } else if ((extraAttr?.getCompound("attributes")?.keys?.size ?: 0) > 1) {
                    val priceData = KuudraPriceData.getOrFetchAttributePricedItem(item)
                    if (priceData != null && priceData != KuudraPriceData.AttributePricedItem.EMPTY && priceData != KuudraPriceData.AttributePricedItem.FAILURE) {
                        priceData.price
                    } else {
                        if (priceData != null) {
                            displayName += "§c (Failed to fetch price ${if (priceData == KuudraPriceData.AttributePricedItem.FAILURE) "from API" else ", not on AH"})"
                        } else {
                            displayName += "§c (Failed to fetch price, using LBIN)"
                        }
                        AuctionData.lowestBINs[identifier] ?: 0.0
                    }
                } else {
                    AuctionData.lowestBINs[identifier] ?: 0.0
                }
                withContext(Dispatchers.MC) {
                    items.add(KuudraChestLootItem(item.count, displayName, itemValue))

                    value += itemValue
                }
            }
        }

        companion object {
            fun getFromName(name: String?): KuudraChest? {
                if (name.isNullOrBlank()) return null
                return entries.find {
                    it.displayText == name
                }
            }
        }
    }

    private var textShadow_ = SmartFontRenderer.TextShadow.NORMAL
    private data class KuudraChestLootItem(var stackSize: Int, var displayText: String, var value: Double) : Comparable<KuudraChestLootItem> {
        override fun compareTo(other: KuudraChestLootItem): Int = value.compareTo(other.value)
    }
    class KuudraChestProfitElement : GuiElement("Kuudra Chest Profit", x = 200, y = 120) {
        override fun render() {
            if (toggled && SBInfo.mode == SkyblockIsland.KuudraHollow.mode) {
                val leftAlign = scaleX < sr.scaledWidth / 2f
                textShadow_ = textShadow
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
                RenderSystem.method_4406()
                KuudraChest.entries.filter { it.items.isNotEmpty() }.forEachIndexed { i, chest ->
                    ScreenRenderer.fontRenderer.drawString(
                        "${chest.displayText}§f: §${(if (chest.value > 0) "a" else "c")}${NumberUtil.format(chest.value.toLong())}",
                        if (leftAlign) 0f else width.toFloat(),
                        (i * ScreenRenderer.fontRenderer.field_0_2811).toFloat(),
                        chest.displayColor,
                        if (leftAlign) TextAlignment.LEFT_RIGHT else TextAlignment.RIGHT_LEFT,
                        textShadow
                    )
                }
            }
        }

        override fun demoRender() {
            RenderUtil.drawAllInList(this, KuudraChest.entries.map { "${it.displayText}: §a+300M" })
        }

        override val height: Int
            get() = ScreenRenderer.fontRenderer.field_0_2811 * KuudraChest.entries.size
        override val width: Int
            get() = ScreenRenderer.fontRenderer.getWidth("Paid Chest: +300M")

        override val toggled: Boolean
            get() = Skytils.config.kuudraChestProfit

        init {
            Skytils.guiManager.registerElement(this)
        }
    }

    enum class KuudraKey(val displayName: String, val rarity: ItemRarity, val coinCost: Int, val materialCost: Int) {
        BASIC("Kuudra Key", ItemRarity.RARE, 200000, 2),
        HOT("Hot Kuudra Key", ItemRarity.EPIC, 400000, 6),
        BURNING("Burning Kuudra Key", ItemRarity.EPIC, 750000, 20),
        FIERY("Fiery Kuudra Key", ItemRarity.EPIC, 1500000, 60),
        INFERNAL("Infernal Kuudra Key", ItemRarity.LEGENDARY,3000000, 120);

        companion object {
            // all keys cost 2 CORRUPTED_NETHER_STAR but nether stars are coop-soulbound
            const val starConstant = 2
        }

        // treat NPC discounts as negligible
        fun getPrice(faction: CrimsonFaction): Double {
            val keyMaterialCost = AuctionData.lowestBINs[faction.keyMaterial] ?: 0.0

            return coinCost + keyMaterialCost * materialCost
        }
    }
}