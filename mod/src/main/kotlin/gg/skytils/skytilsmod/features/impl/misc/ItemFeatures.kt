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
import gg.skytils.event.impl.play.BlockInteractEvent
import gg.skytils.event.impl.render.WorldDrawEvent
import gg.skytils.event.impl.screen.GuiContainerForegroundDrawnEvent
import gg.skytils.event.impl.screen.GuiContainerPostDrawSlotEvent
import gg.skytils.event.impl.screen.GuiContainerPreDrawSlotEvent
import gg.skytils.event.impl.screen.GuiContainerSlotClickEvent
import gg.skytils.event.register
import gg.skytils.hypixel.types.skyblock.Pet
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.json
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod._event.MainThreadPacketReceiveEvent
import gg.skytils.skytilsmod._event.PacketSendEvent
import gg.skytils.skytilsmod.core.GuiManager
import gg.skytils.skytilsmod.core.structure.GuiElement
import gg.skytils.skytilsmod.core.tickTimer
import gg.skytils.skytilsmod.features.impl.dungeons.DungeonFeatures
import gg.skytils.skytilsmod.features.impl.dungeons.DungeonFeatures.dungeonFloorNumber
import gg.skytils.skytilsmod.features.impl.handlers.AuctionData
import gg.skytils.skytilsmod.features.impl.handlers.KuudraPriceData
import gg.skytils.skytilsmod.mixins.transformers.accessors.AccessorGuiContainer
import gg.skytils.skytilsmod.utils.*
import gg.skytils.skytilsmod.utils.ItemUtil.getDisplayName
import gg.skytils.skytilsmod.utils.ItemUtil.getExtraAttributes
import gg.skytils.skytilsmod.utils.ItemUtil.getItemLore
import gg.skytils.skytilsmod.utils.ItemUtil.getSkyBlockItemID
import gg.skytils.skytilsmod.utils.NumberUtil.romanToDecimal
import gg.skytils.skytilsmod.utils.RenderUtil.highlight
import gg.skytils.skytilsmod.utils.RenderUtil.renderRarity
import gg.skytils.skytilsmod.utils.SkillUtils.level
import gg.skytils.skytilsmod.utils.Utils.equalsOneOf
import gg.skytils.skytilsmod.utils.graphics.ScreenRenderer
import gg.skytils.skytilsmod.utils.graphics.SmartFontRenderer.TextAlignment
import gg.skytils.skytilsmod.utils.graphics.SmartFontRenderer.TextShadow
import gg.skytils.skytilsmod.utils.graphics.colors.CommonColors
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.block.DoorBlock
import net.minecraft.block.LadderBlock
import net.minecraft.block.FluidBlock
import net.minecraft.block.AbstractSignBlock
import net.minecraft.client.network.OtherClientPlayerEntity
import net.minecraft.client.gui.screen.Screen
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.entity.projectile.FishingBobberEntity
import net.minecraft.block.Blocks
import net.minecraft.item.Items
import net.minecraft.screen.GenericContainerScreenHandler
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtString
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket
import net.minecraft.util.math.Direction
import net.minecraft.particle.ParticleType
import net.minecraft.util.hit.HitResult
import org.lwjgl.input.Keyboard
import java.awt.Color
import kotlin.math.min
import kotlin.math.pow

object ItemFeatures : EventSubscriber {

    private val headPattern =
        Regex("(?:DIAMOND|GOLD)_(?:(BONZO)|(SCARF)|(PROFESSOR)|(THORN)|(LIVID)|(SADAN)|(NECRON))_HEAD")

    // TODO: it is possible for 2 items to have the same name but different material
    val itemIdToNameLookup = hashMapOf<String, String>()
    val sellPrices = HashMap<String, Double>()
    val bitCosts = HashMap<String, Int>()
    val copperCosts = HashMap<String, Int>()
    val hotbarRarityCache = arrayOfNulls<ItemRarity>(9)
    var soulflowAmount = ""
    var stackingEnchantDisplayText = ""
    var lowSoulFlowPinged = false
    var lastShieldUse = -1L
    var lastShieldClick = 0L

    init {
        StackingEnchantDisplay()
        SoulflowGuiElement()
        WitherShieldDisplay()
    }

    val interactables = setOf(
        Blocks.ACACIA_DOOR,
        Blocks.ANVIL,
        Blocks.field_0_727,
        Blocks.BED,
        Blocks.BIRCH_DOOR,
        Blocks.BREWING_STAND,
        Blocks.COMMAND_BLOCK,
        Blocks.CRAFTING_TABLE,
        Blocks.field_0_680,
        Blocks.DARK_OAK_DOOR,
        Blocks.field_0_783,
        Blocks.field_0_784,
        Blocks.DISPENSER,
        Blocks.DROPPER,
        Blocks.ENCHANTING_TABLE,
        Blocks.ENDER_CHEST,
        Blocks.FURNACE,
        Blocks.field_0_787,
        Blocks.JUNGLE_DOOR,
        Blocks.LEVER,
        Blocks.NOTEBLOCK,
        Blocks.field_0_782,
        Blocks.field_0_781,
        Blocks.field_0_731,
        Blocks.field_0_730,
        Blocks.STANDING_SIGN,
        Blocks.WALL_SIGN,
        Blocks.TRAPDOOR,
        Blocks.TRAPPED_CHEST,
        Blocks.WOODEN_BUTTON,
        Blocks.STONE_BUTTON,
        Blocks.WOODEN_DOOR,
        Blocks.field_0_776
    )

    init {
        tickTimer(4, repeats = true) {
            if (mc.player != null && Utils.inSkyblock) {
                val held = mc.player.inventory.mainHandStack
                if (Skytils.config.showItemRarity) {
                    for (i in 0..8) {
                        hotbarRarityCache[i] = ItemUtil.getRarity(mc.player.inventory.main[i])
                    }
                }
                if (Skytils.config.stackingEnchantProgressDisplay) {
                    apply {
                        also {
                            val extraAttr = getExtraAttributes(held) ?: return@also
                            val enchantments = extraAttr.getCompound("enchantments")
                            val stacking =
                                EnchantUtil.enchants.find { it is StackingEnchant && extraAttr.contains(it.nbtNum) } as? StackingEnchant
                                    ?: return@also

                            val stackingLevel = enchantments.getInt(stacking.nbtName)
                            val stackingAmount = extraAttr.getLong(stacking.nbtNum)

                            stackingEnchantDisplayText = buildString {
                                append("§b${stacking.loreName} §e$stackingLevel §f")
                                val nextLevel = stacking.stackLevel.getOrNull(stackingLevel)
                                if (stackingLevel == stacking.maxLevel || nextLevel == null) {
                                    append("(§e${stackingAmount}§f)")
                                } else {
                                    append("(§c${stackingAmount} §f/ §a${NumberUtil.format(nextLevel)}§f)")
                                }
                            }
                            return@apply
                        }
                        stackingEnchantDisplayText = ""
                    }
                }
            }
        }
    }

    override fun setup() {
        register(::onDrawSlot)
        register(::onSlotClick)
        register(::ontooltip, EventPriority.Highest)
        register(::onReceivePacket)
        register(::onSendPacket)
        register(::onEntitySpawn)
        register(::onInteract)
        register(::onRenderItemOverlayPost)
        register(::onDrawContainerForeground)
        register(::onRenderWorld)
    }

    fun onDrawSlot(event: GuiContainerPreDrawSlotEvent) {
        if (Utils.inSkyblock && Skytils.config.showItemRarity && event.slot.hasStack()) {
            renderRarity(event.slot.stack, event.slot.x, event.slot.y)
        }
        if (event.container is GenericContainerScreenHandler) {
            val chestName = event.chestName
            if (chestName.startsWithAny("Salvage", "Ender Chest") || equalsOneOf(
                    chestName,
                    "Ophelia",
                    "Trades"
                ) || (chestName.contains("Backpack") && !chestName.endsWith("Recipe"))
            ) {
                if (Skytils.config.highlightSalvageableItems) {
                    if (event.slot.hasStack()) {
                        val stack = event.slot.stack
                        if (ItemUtil.isSalvageable(stack)) {
                            RenderSystem.method_4348(0f, 0f, 1f)
                            event.slot highlight Color(15, 233, 233)
                            RenderSystem.method_4348(0f, 0f, -1f)
                        }
                    }
                }
            }
            if (chestName == "Ophelia" || chestName == "Trades" || chestName == "Booster Cookie") {
                if (Skytils.config.highlightDungeonSellableItems) {
                    if (event.slot.hasStack()) {
                        val stack = event.slot.stack
                        if (stack.name.containsAny(
                                "Defuse Kit",
                                "Lever",
                                "Torch",
                                "Stone Button",
                                "Tripwire Hook",
                                "Journal Entry",
                                "Training Weights",
                                "Mimic Fragment",
                                "Healing 8 Splash Potion",
                                "Healing VIII Splash Potion",
                                "Premium Flesh"
                            )
                        ) event.slot highlight Color(255, 50, 150, 255)
                    }
                }
            }
            if (Skytils.config.combineHelper && equalsOneOf(
                    event.chestName,
                    "Anvil",
                    "Attribute Fusion"
                )
            ) {
                val item = event.container.getSlot(29).stack ?: return
                if (event.container.getSlot(33).hasStack()) return
                val candidate = event.slot.stack ?: return
                val nbt1 = getExtraAttributes(item) ?: return
                val nbt2 = getExtraAttributes(candidate) ?: return
                val tagName = when (getSkyBlockItemID(nbt1) to getSkyBlockItemID(nbt2)) {
                    "ENCHANTED_BOOK" to "ENCHANTED_BOOK" -> "enchantments"
                    "ATTRIBUTE_SHARD" to "ATTRIBUTE_SHARD" -> "attributes"
                    else -> return
                }
                val typeList = listOf(nbt1, nbt2).map { nbt ->
                    nbt.getCompound(tagName)
                }
                val tierList = typeList.mapNotNull { nbt ->
                    nbt.keys.takeIf { it.size == 1 }?.first()
                }
                if (tierList.size != 2 || tierList[0] != tierList[1] || typeList[0].getInt(tierList[0]) != typeList[1].getInt(
                        tierList[1]
                    )
                ) return

                event.slot highlight Color(17, 252, 243)
            }
        }
    }

    fun onSlotClick(event: GuiContainerSlotClickEvent) {
        if (!Utils.inSkyblock) return
        if (event.container is GenericContainerScreenHandler) {
            if (event.slot != null && event.slot!!.hasStack()) {
                val item = event.slot!!.stack ?: return
                val extraAttr = getExtraAttributes(item)
                if (Skytils.config.stopClickingNonSalvageable) {
                    if (event.chestName.startsWith("Salvage") && extraAttr != null) {
                        if (!extraAttr.contains("baseStatBoostPercentage") && !item.name.contains("Salvage") && !item.name.contains(
                                "Essence"
                            )
                        ) {
                            event.cancelled = true
                        }
                    }
                }
            }
        }
    }

    fun ontooltip(event: gg.skytils.event.impl.item.ItemTooltipEvent) {
        if (!Utils.inSkyblock) return
        val item = event.stack
        val extraAttr = getExtraAttributes(item)
        var itemId = getSkyBlockItemID(extraAttr)
        var isSuperpairsReward = false
        if (item != null && mc.player.currentScreenHandler != null && SBInfo.lastOpenContainerName?.startsWith(
                "Superpairs ("
            ) == true
        ) {
            if (getDisplayName(item).stripControlCodes() == "Enchanted Book") {
                val lore = getItemLore(item)
                if (lore.size >= 3) {
                    if (lore[0] == "§8Item Reward" && lore[1].isEmpty()) {
                        val line2 = lore[2].stripControlCodes()
                        val enchantName =
                            line2.substringBeforeLast(" ").replace(Regex("[\\s-]"), "_").uppercase()
                        itemId = "ENCHANTED_BOOK-" + enchantName + "-" + item.count
                        isSuperpairsReward = true
                    }
                }
            }
        }
        if (itemId != null) {
            if (Skytils.config.showLowestBINPrice || Skytils.config.showCoinsPerBit || Skytils.config.showCoinsPerCopper || Skytils.config.showKuudraLowestBinPrice) {
                val auctionIdentifier = if (isSuperpairsReward) itemId else AuctionData.getIdentifier(item)
                if (auctionIdentifier != null) {
                    // this might actually have multiple items as the price
                    val valuePer = AuctionData.lowestBINs[auctionIdentifier]
                    if (valuePer != null) {
                        if (Skytils.config.showLowestBINPrice) {
                            val total =
                                if (isSuperpairsReward) NumberUtil.nf.format(valuePer) else NumberUtil.nf.format(
                                    valuePer * item!!.count
                                )
                            event.tooltip.add(
                                "§6Lowest BIN Price: §b$total" + if (item!!.count > 1 && !isSuperpairsReward) " §7(" + NumberUtil.nf.format(
                                    valuePer
                                ) + " each§7)" else ""
                            )
                        }
                        if (Skytils.config.showKuudraLowestBinPrice && item.count == 1) {
                            KuudraPriceData.getAttributePricedItemId(item)?.let {attrId ->
                                val kuudraPrice = KuudraPriceData.getOrRequestAttributePricedItem(attrId)
                                if (kuudraPrice != null) {
                                    if (kuudraPrice == KuudraPriceData.AttributePricedItem.EMPTY) {
                                        event.tooltip.add("§6Kuudra BIN Price: §cNot Found")
                                    } else {
                                        event.tooltip.add(
                                            "§6Kuudra BIN Price: §b${NumberUtil.nf.format(kuudraPrice.price)}"
                                        )
                                    }
                                } else {
                                    event.tooltip.add("§6Kuudra BIN Price: §cLoading...")
                                }
                            }
                        }
                        if (Skytils.config.showCoinsPerBit) {
                            var bitValue = bitCosts.getOrDefault(auctionIdentifier, -1)
                            if (bitValue == -1 && SBInfo.lastOpenContainerName == "Community Shop" || SBInfo.lastOpenContainerName?.startsWith(
                                    "Bits Shop - "
                                ) == true
                            ) {
                                val lore = getItemLore(item!!)
                                for (i in lore.indices) {
                                    val line = lore[i]
                                    if (line == "§7Cost" && i + 3 < lore.size && lore[i + 3] == "§eClick to trade!") {
                                        val bits = lore[i + 1]
                                        if (bits.startsWith("§b") && bits.endsWith(" Bits")) {
                                            bitValue = bits.replace("[^0-9]".toRegex(), "").toInt()
                                            bitCosts[auctionIdentifier] = bitValue
                                            break
                                        }
                                    }
                                }
                            }
                            if (bitValue != -1) event.tooltip.add("§6Coin/Bit: §b" + NumberUtil.nf.format(valuePer / bitValue))
                        }
                        if (Skytils.config.showCoinsPerCopper) {
                            var copperValue = copperCosts.getOrDefault(auctionIdentifier, -1)
                            if (copperValue == -1 && SBInfo.lastOpenContainerName == "SkyMart") {
                                val lore = getItemLore(item!!)
                                for (i in lore.indices) {
                                    val line = lore[i]
                                    if (line == "§7Cost" && i + 3 < lore.size && equalsOneOf(
                                            lore[i + 3],
                                            "§eClick to trade!",
                                            "§cNot unlocked!"
                                        )
                                    ) {
                                        val copper = lore[i + 1]
                                        if (copper.startsWith("§c") && copper.endsWith(" Copper")) {
                                            copperValue = copper.replace("[^0-9]".toRegex(), "").toInt()
                                            copperCosts[auctionIdentifier] = copperValue
                                            break
                                        }
                                    }
                                }
                            }
                            if (copperValue != -1) event.tooltip.add(
                                "§6Coin/Copper: §c" + NumberUtil.nf.format(valuePer / copperValue)
                            )
                        }
                    }
                }
            }
            if (Skytils.config.showNPCSellPrice) {
                val valuePer = sellPrices[itemId]
                if (valuePer != null) event.tooltip.add(
                    "§6NPC Value: §b" + NumberUtil.nf.format(valuePer * item!!.count) + if (item.count > 1) " §7(" + NumberUtil.nf.format(
                        valuePer
                    ) + " each§7)" else ""
                )
            }
        }
        if (Skytils.config.showRadioactiveBonus && itemId == "TARANTULA_HELMET") {
            val bonus = try {
                (TabListUtils.tabEntries[68].second.substringAfter("❁").removeSuffix("§r").toInt()
                    .coerceAtMost(1000) / 10).toString()
            } catch (e: Exception) {
                "Error"
            }
            for (i in event.tooltip.indices) {
                val line = event.tooltip[i]
                if (line.contains("§7Crit Damage:")) {
                    event.tooltip.add(i + 1, "§8Radioactive Bonus: §c+${bonus}%")
                    break
                }
            }
        }
        if (itemId == "PREHISTORIC_EGG" && extraAttr != null) {
            event.tooltip.add((event.tooltip.indexOfFirst { it.contains("Legendary Armadillo") } + 1),
                "§7Blocks Walked: §c${extraAttr.getInt("blocks_walked")}")
        }
        if (Skytils.config.showGemstones && extraAttr?.contains("gems") == true) {
            val gems = extraAttr.getCompound("gems")
            event.tooltip.add("§bGemstones: ")
            event.tooltip.addAll(gems.keys.filterNot { it.endsWith("_gem") || it == "unlocked_slots" }.map {
                val quality = when (val tag: NbtElement? = gems.get(it)) {
                    is NbtCompound -> tag.getString("quality").toTitleCase().ifEmpty { "Report Unknown" }
                    is NbtString -> tag.asString().toTitleCase()
                    null -> "Report Issue"
                    else -> "Report Tag $tag"
                }
                "  §6- $quality ${
                    gems.getString("${it}_gem").ifEmpty { it.substringBeforeLast("_") }.toTitleCase()
                }"
            })
        }

        if (Skytils.config.showItemQuality && extraAttr != null) {
            val boost = extraAttr.getInt("baseStatBoostPercentage")
            
            if (boost > 0) {
                val tier = extraAttr.getInt("item_tier")

                val req = extraAttr.getString("dungeon_skill_req")

                val floor: String = if (req.isEmpty() && tier == 0) "§aE" else if (req.isEmpty()) "§bF${tier}" else {
                    val (dungeon, level) = req.split(':', limit = 2)
                    val levelReq = level.toIntOrNull() ?: 0
                    if (dungeon == "CATACOMBS") {
                        if (levelReq - tier > 19) "§4M${tier-3}" else "§aF$tier"
                    } else {
                        "§b${dungeon} $tier"
                    }
                }

                val color = when {
                    boost <= 17 -> "§c"
                    boost <= 33 -> "§e"
                    boost <= 49 -> "§a"
                    else -> "§b"
                }

                event.tooltip.add("§6Quality Bonus: $color+$boost% §7($floor§7)")
            }
        }

        if (DevTools.getToggle("nbt") && Keyboard.isKeyDown(46) && Screen.method_2238() && !Screen.method_2223() && !Screen.method_2232()) {
            Screen.method_0_2797(event.stack.nbt?.toString())
        }
    }

    fun onReceivePacket(event: MainThreadPacketReceiveEvent<*>) {
        if (!Utils.inSkyblock || mc.world == null) return
        event.packet.apply {
            if (this is ParticleS2CPacket) {
                if (type == ParticleType.EXPLOSION_LARGE && Skytils.config.hideImplosionParticles) {
                    if (isLongDistance && count == 8 && speed == 8f && offsetX == 0f && offsetY == 0f && offsetZ == 0f) {
                        val dist = (if (DungeonFeatures.hasBossSpawned && dungeonFloorNumber == 7) 4f else 11f).pow(2f)

                        if (mc.world.players.any {
                                it.method_0_7087() != null && it.uuid.version() == 4 && it.squaredDistanceTo(
                                    x,
                                    y,
                                    z
                                ) <= dist && getDisplayName(it.method_0_7087()).stripControlCodes().containsAny(
                                    "Necron's Blade", "Scylla", "Astraea", "Hyperion", "Valkyrie"
                                )
                            }) {
                            event.cancelled = true
                        }
                    }
                }
            }
            if (this is ScreenHandlerSlotUpdateS2CPacket && syncId == 0) {
                if (mc.player == null || (!Utils.inSkyblock && mc.player.age > 1)) return
                val slot = slot

                val item = stack ?: return
                val extraAttr = getExtraAttributes(item) ?: return
                val itemId = getSkyBlockItemID(extraAttr) ?: return

                if (equalsOneOf(itemId, "SOULFLOW_PILE", "SOULFLOW_BATTERY", "SOULFLOW_SUPERCELL")) {
                    getItemLore(item).find {
                        it.startsWith("§7Internalized: ")
                    }?.substringAfter("§7Internalized: ")?.let { s ->
                        soulflowAmount = s
                        s.drop(2).filter { it.isDigit() }.toIntOrNull()?.let {
                            if (Skytils.config.lowSoulflowPing > 0) {
                                if (it <= Skytils.config.lowSoulflowPing && !lowSoulFlowPinged) {
                                    GuiManager.createTitle("§cLow Soulflow", 20)
                                    lowSoulFlowPinged = true
                                } else if (it > Skytils.config.lowSoulflowPing) {
                                    lowSoulFlowPinged = false
                                }
                            }
                        }
                    }
                }
            }
            if (this is EntityTrackerUpdateS2CPacket && lastShieldClick != -1L && id() == mc.player?.id && System.currentTimeMillis() - lastShieldClick <= 500 && trackedValues?.any { it.id() == 17 } == true) {
                lastShieldUse = System.currentTimeMillis()
                lastShieldClick = -1
            }
        }
    }

    fun onSendPacket(event: PacketSendEvent<*>) {
        if (!Utils.inSkyblock || lastShieldUse != -1L || mc.player?.method_0_7087() == null) return
        if (event.packet is PlayerInteractBlockC2SPacket &&
            mc.player.method_0_7087().item == Items.IRON_SWORD &&
            getExtraAttributes(mc.player.method_0_7087())
                ?.getList("ability_scroll", 8) // String
                ?.asStringSet()
                ?.contains("WITHER_SHIELD_SCROLL") == true
        ) {
            lastShieldClick = System.currentTimeMillis()
        }
    }

    fun onEntitySpawn(event: gg.skytils.event.impl.entity.EntityJoinWorldEvent) {
        if (!Utils.inSkyblock) return
        if (event.entity !is FishingBobberEntity || !Skytils.config.hideFishingHooks) return
        if ((event.entity as FishingBobberEntity).field_0_8080 is OtherClientPlayerEntity) {
            event.entity.remove()
            event.cancelled = true
        }
    }

    fun onInteract(event: BlockInteractEvent) {
        if (!Utils.inSkyblock) return
        val item = event.item
        val itemId = getSkyBlockItemID(item) ?: return
        if (Skytils.config.preventPlacingWeapons && (equalsOneOf(
                itemId,
                "FLOWER_OF_TRUTH",
                "BOUQUET_OF_LIES",
                "MOODY_GRAPPLESHOT",
                "BAT_WAND",
                "STARRED_BAT_WAND",
                "WEIRD_TUBA",
                "WEIRDER_TUBA",
                "PUMPKIN_LAUNCHER",
                "FIRE_FREEZE_STAFF"
            ))
        ) {
            val block = mc.world.getBlockState(event.pos)
            if (!interactables.contains(block.block) || Utils.inDungeons && (block.block === Blocks.COAL_BLOCK || block.block === Blocks.STAINED_HARDENED_CLAY)) {
                event.cancelled = true
            }
        }
    }

    fun onRenderItemOverlayPost(event: GuiContainerPostDrawSlotEvent) {
        val item = event.slot.stack ?: return
        if (!Utils.inSkyblock || item.count != 1 || item.nbt?.contains("SkytilsNoItemOverlay") == true) return
        val matrixStack = UMatrixStack()
        var stackTip = ""
        val lore = getItemLore(item).takeIf { it.isNotEmpty() }
        getExtraAttributes(item)?.let { extraAttributes ->
            val matrixStack = UMatrixStack()
            val itemId = getSkyBlockItemID(extraAttributes)
            if (Skytils.config.showPotionTier && extraAttributes.contains("potion_level")) {
                stackTip = extraAttributes.getInt("potion_level").toString()
            } else if (Skytils.config.showAttributeShardLevel && itemId == "ATTRIBUTE_SHARD") {
                extraAttributes.getCompound("attributes").takeUnless {
                    it.isEmpty
                }?.let {
                    /*
                    If they ever add the ability to combine attributes on shards, this will need to be updated to:
                    stackTip = it.keySet.maxOf { s -> it.getInteger(s) }.toString()
                    */
                    stackTip = it.getInt(it.keys.first()).toString()
                }
            } else if ((Skytils.config.showEnchantedBookTier || Skytils.config.showEnchantedBookAbbreviation) && itemId == "ENCHANTED_BOOK") {
                extraAttributes.getCompound("enchantments").takeIf {
                    it.keys.size == 1
                }?.let { enchantments ->
                    val name = enchantments.keys.first()
                    if (Skytils.config.showEnchantedBookAbbreviation) {
                        val enchant = EnchantUtil.enchants.find { it.nbtName == name }
                        val prefix: String = if (enchant != null) {
                            val parts = enchant.loreName.split(" ")
                            val joined = if (parts.size > 1) parts.joinToString("") { it[0].uppercase() }
                                else if (parts.first().startsWith("Turbo-")) "${
                                    parts.first().split("-")[1].take(3).toTitleCase()
                                }."
                                else "${
                                    parts.first().take(3).toTitleCase()
                                }."
                            if (enchant.nbtName.startsWith("ultimate")) {
                                "§d§l${joined}"
                            } else joined
                        } else {
                            val parts = name.split("_")
                            if (parts[0] == "ultimate") {
                                "§d§l" + parts.drop(1).joinToString("") { s -> s[0].uppercase() }
                            } else {
                                if (parts.size > 1) {
                                    parts.joinToString("") { s -> s[0].uppercase() }
                                } else {
                                    parts[0].take(3).toTitleCase() + "."
                                }
                            }
                        }
                        RenderSystem.method_4406()
                        RenderSystem.disableDepthTest()
                        RenderSystem.disableBlend()
                        RenderSystem.pushMatrix()
                        RenderSystem.method_4348(event.slot.x.toFloat(), event.slot.y.toFloat(), 1f)
                        RenderSystem.method_4453(0.8, 0.8, 1.0)
                        ScreenRenderer.fontRenderer.drawString(
                            prefix,
                            0f,
                            0f,
                            CommonColors.WHITE,
                            TextAlignment.LEFT_RIGHT,
                            TextShadow.NORMAL
                        )
                        RenderSystem.popMatrix()
                        RenderSystem.method_4394()
                        RenderSystem.enableDepthTest()
                    }
                    if (Skytils.config.showEnchantedBookTier) stackTip =
                        enchantments.getInt(name).toString()
                }
            } else if (Skytils.config.showHeadFloorNumber && item.item === Items.PLAYER_HEAD && headPattern.matches(
                    itemId ?: ""
                )
            ) {
                stackTip = headPattern.matchEntire(itemId!!)?.groups?.indexOfLast { it != null }.toString()
            } else if (Skytils.config.showStarCount && ItemUtil.getStarCount(extraAttributes) > 0) {
                stackTip = ItemUtil.getStarCount(extraAttributes).toString()
            }
            if (extraAttributes.contains("pickonimbus_durability")) {
                val durability = extraAttributes.getInt("pickonimbus_durability")
                /*
                Old Pickonimbuses had 5000 durability. If they were at full durability, they were nerfed to 2000.
                However, if they were not at full durability, they were left alone. Therefore, it's not really
                possible to check the true max durability.
                */
                if (durability < 2000) {
                    RenderUtil.drawDurabilityBar(
                        event.slot.x,
                        event.slot.y,
                        1 - durability / 2000.0
                    )
                }
            }
            if (Skytils.config.showAttributeShardAbbreviation && itemId == "ATTRIBUTE_SHARD" && extraAttributes.getCompound(
                    "attributes"
                ).keys.size == 1
            ) {
                lore?.getOrNull(0)?.split(' ')?.dropLastWhile { it.romanToDecimal() == 0 }?.dropLast(1)
                    ?.joinToString(separator = "") {
                        if (it.startsWith('§'))
                            it.substring(0, 2) + it[2].uppercase()
                        else
                            it[0].uppercase()
                    }?.let { attribute ->
                        UGraphics.disableLighting()
                        UGraphics.disableDepth()
                        UGraphics.disableBlend()
                        matrixStack.push()
                        matrixStack.translate(event.slot.x.toFloat(), event.slot.y.toFloat(), 1f)
                        matrixStack.scale(0.8, 0.8, 1.0)
                        matrixStack.runWithGlobalState {
                            ScreenRenderer.fontRenderer.drawString(
                                attribute,
                                0f,
                                0f,
                                CommonColors.WHITE,
                                TextAlignment.LEFT_RIGHT,
                                TextShadow.NORMAL
                            )
                        }
                        matrixStack.pop()
                        UGraphics.enableLighting()
                        UGraphics.enableDepth()
                    }
            }
            if (Skytils.config.showNYCakeYear && extraAttributes.contains("new_years_cake")) {
                stackTip = extraAttributes.getInt("new_years_cake").toString()
            }
        }
        if (Skytils.config.showPetCandies && item.item === Items.PLAYER_HEAD) {
            val petInfoString = getExtraAttributes(item)?.getString("petInfo")
            if (!petInfoString.isNullOrBlank()) {
                val petInfo = json.decodeFromString<Pet>(petInfoString)
                val level = petInfo.level
                val maxLevel = if (petInfo.type == "GOLDEN_DRAGON") 200 else 100

                if (petInfo.candyUsed > 0 && (SuperSecretSettings.alwaysShowPetCandy || level != maxLevel)) {
                    stackTip = petInfo.candyUsed.toString()
                }
            }
        }
        if (stackTip.isNotEmpty()) {
            RenderSystem.method_4406()
            RenderSystem.disableDepthTest()
            RenderSystem.disableBlend()
            UGraphics.drawString(
                matrixStack,
                stackTip,
                (event.slot.x + 17 - UGraphics.getStringWidth(stackTip)).toFloat(),
                (event.slot.y + 9).toFloat(),
                16777215,
                true
            )
            RenderSystem.method_4394()
            RenderSystem.enableDepthTest()
        }
    }

    fun onDrawContainerForeground(event: GuiContainerForegroundDrawnEvent) {
        if (!Skytils.config.combineHelper || !Utils.inSkyblock) return
        if (event.container !is GenericContainerScreenHandler || !equalsOneOf(
                event.chestName,
                "Anvil",
                "Attribute Fusion"
            )
        ) return
        val item1 = event.container.getSlot(29).stack ?: return
        val item2 = event.container.getSlot(33).stack ?: return
        val nbt1 = getExtraAttributes(item1) ?: return
        val nbt2 = getExtraAttributes(item2) ?: return
        val tagName = when (getSkyBlockItemID(nbt1) to getSkyBlockItemID(nbt2)) {
            "ENCHANTED_BOOK" to "ENCHANTED_BOOK" -> "enchantments"
            "ATTRIBUTE_SHARD" to "ATTRIBUTE_SHARD" -> "attributes"
            else -> return
        }
        val typeList = listOf(nbt1, nbt2).map { nbt ->
            nbt.getCompound(tagName)
        }
        val tierList = typeList.mapNotNull { nbt ->
            nbt.keys.takeIf { it.size == 1 }?.first()
        }
        if (tierList.size != 2) return
        val errorString = if (tierList[0] != tierList[1]) {
            "Types don't match!"
        } else if (typeList[0].getInt(tierList[0]) != typeList[1].getInt(tierList[1])) {
            "Tiers don't match!"
        } else return
        val gui =
            event.gui as AccessorGuiContainer
        UGraphics.disableLighting()
        UGraphics.disableBlend()
        UGraphics.disableDepth()
        ScreenRenderer.fontRenderer.drawString(
            errorString,
            gui.xSize / 2f,
            22.5f,
            CommonColors.RED,
            TextAlignment.MIDDLE
        )
        UGraphics.enableDepth()
        UGraphics.enableBlend()
        UGraphics.enableLighting()
    }

    fun onRenderWorld(event: WorldDrawEvent) {
        if (!Utils.inSkyblock) return
        if (Skytils.config.showEtherwarpTeleportPos && mc.player?.isSneaking == true) {
            val extraAttr = getExtraAttributes(mc.player.method_0_7087()) ?: return
            if (!extraAttr.getBoolean("ethermerge")) return
            val dist = 57.0 + extraAttr.getInt("tuned_transmission")
            val vec3 = mc.player.getCameraPosVec(event.partialTicks)
            val vec31 = mc.player.getRotationVec(event.partialTicks)
            val vec32 = vec3.add(
                vec31.x * dist,
                vec31.y * dist,
                vec31.z * dist
            )
            val obj = mc.world.raycast(vec3, vec32, true, false, true) ?: return
            val block = obj.blockPos ?: return
            val state = mc.world.getBlockState(block)
            if (isValidEtherwarpPos(obj)) {
                RenderUtil.drawSelectionBox(
                    block,
                    state.block,
                    Skytils.config.showEtherwarpTeleportPosColor,
                    event.partialTicks
                )
            }
        }
    }

    private fun isValidEtherwarpPos(obj: HitResult): Boolean {
        val pos = obj.blockPos
        val sideHit = obj.side

        return mc.world.getBlockState(pos).block.material.isSolid && (1..2).all {
            val newPos = pos.up(it)
            val newBlock = mc.world.getBlockState(newPos)
            if (sideHit === Direction.UP && (equalsOneOf(
                    newBlock.block,
                    Blocks.field_0_677,
                    Blocks.field_0_776
                ) || newBlock.block is FluidBlock)
            ) return@all false
            if (sideHit !== Direction.UP && newBlock.block is AbstractSignBlock) return@all false
            if (newBlock.block is LadderBlock || newBlock.block is DoorBlock) return@all false
            return@all newBlock.block.method_9516(mc.world, newPos)
        }
    }

    class StackingEnchantDisplay : GuiElement("Stacking Enchant Display", x = 0.65f, y = 0.85f) {
        override fun render() {
            if (toggled && Utils.inSkyblock && stackingEnchantDisplayText.isNotBlank()) {
                val alignment =
                    if (scaleX < UResolution.scaledWidth / 2f) TextAlignment.LEFT_RIGHT else TextAlignment.RIGHT_LEFT
                ScreenRenderer.fontRenderer.drawString(
                    stackingEnchantDisplayText,
                    if (scaleX < UResolution.scaledWidth / 2f) 0f else width.toFloat(),
                    0f,
                    CommonColors.WHITE,
                    alignment,
                    TextShadow.NORMAL
                )
            }
        }

        override fun demoRender() {
            val alignment =
                if (scaleX < UResolution.scaledWidth / 2f) TextAlignment.LEFT_RIGHT else TextAlignment.RIGHT_LEFT
            ScreenRenderer.fontRenderer.drawString(
                "Expertise 10: Maxed",
                if (scaleX < UResolution.scaledWidth / 2f) 0f else width.toFloat(),
                0f,
                CommonColors.RAINBOW,
                alignment,
                TextShadow.NORMAL
            )
        }

        override val height: Int
            get() = ScreenRenderer.fontRenderer.field_0_2811
        override val width: Int
            get() = ScreenRenderer.fontRenderer.getWidth("Expertise 10 (Maxed)")
        override val toggled: Boolean
            get() = Skytils.config.stackingEnchantProgressDisplay

        init {
            Skytils.guiManager.registerElement(this)
        }
    }

    class SoulflowGuiElement : GuiElement("Soulflow Display", x = 0.65f, y = 0.85f) {
        override fun render() {
            if (Utils.inSkyblock && toggled) {
                val alignment =
                    if (scaleX < UResolution.scaledWidth / 2f) TextAlignment.LEFT_RIGHT else TextAlignment.RIGHT_LEFT
                ScreenRenderer.fontRenderer.drawString(
                    soulflowAmount,
                    if (scaleX < UResolution.scaledWidth / 2f) 0f else width.toFloat(),
                    0f,
                    CommonColors.WHITE,
                    alignment,
                    TextShadow.NORMAL
                )
            }
        }

        override fun demoRender() {
            val alignment =
                if (scaleX < UResolution.scaledWidth / 2f) TextAlignment.LEFT_RIGHT else TextAlignment.RIGHT_LEFT
            ScreenRenderer.fontRenderer.drawString(
                "§3100⸎ Soulflow",
                if (scaleX < UResolution.scaledWidth / 2f) 0f else width.toFloat(),
                0f,
                CommonColors.WHITE,
                alignment,
                TextShadow.NORMAL
            )
        }

        override val height: Int
            get() = ScreenRenderer.fontRenderer.field_0_2811
        override val width: Int
            get() = ScreenRenderer.fontRenderer.getWidth("§3100⸎ Soulflow")
        override val toggled: Boolean
            get() = Skytils.config.showSoulflowDisplay

        init {
            Skytils.guiManager.registerElement(this)
        }
    }


    class WitherShieldDisplay : GuiElement("Wither Shield Display", x = 0.65f, y = 0.85f) {
        override fun render() {
            if (toggled && Utils.inSkyblock) {
                val alignment =
                    if (scaleX < UResolution.scaledWidth / 2f) TextAlignment.LEFT_RIGHT else TextAlignment.RIGHT_LEFT
                if (lastShieldUse != -1L) {
                    val diff =
                        ((lastShieldUse + (if (Skytils.config.assumeWitherImpact) 5000 else 10000) - System.currentTimeMillis()) / 1000f)
                    ScreenRenderer.fontRenderer.drawString(
                        "Shield: §c${"%.2f".format(diff)}s",
                        if (scaleX < UResolution.scaledWidth / 2f) 0f else width.toFloat(),
                        0f,
                        CommonColors.ORANGE,
                        alignment,
                        TextShadow.NORMAL
                    )
                    if (diff < 0) lastShieldUse = -1
                } else {
                    ScreenRenderer.fontRenderer.drawString(
                        "Shield: §aREADY",
                        if (scaleX < UResolution.scaledWidth / 2f) 0f else width.toFloat(),
                        0f,
                        CommonColors.ORANGE,
                        alignment,
                        TextShadow.NORMAL
                    )
                }
            }
        }

        override fun demoRender() {
            val alignment =
                if (scaleX < UResolution.scaledWidth / 2f) TextAlignment.LEFT_RIGHT else TextAlignment.RIGHT_LEFT
            ScreenRenderer.fontRenderer.drawString(
                "§6Shield: §aREADY",
                if (scaleX < UResolution.scaledWidth / 2f) 0f else width.toFloat(),
                0f,
                CommonColors.WHITE,
                alignment,
                TextShadow.NORMAL
            )
        }

        override val height: Int
            get() = ScreenRenderer.fontRenderer.field_0_2811
        override val width: Int
            get() = ScreenRenderer.fontRenderer.getWidth("§6Shield: §aREADY")
        override val toggled: Boolean
            get() = Skytils.config.witherShieldCooldown

        init {
            Skytils.guiManager.registerElement(this)
        }
    }

    @Serializable
    data class APISBItem(
        @SerialName("id")
        val id: String,
        @SerialName("material")
        val material: String,
        @SerialName("motes_sell_price")
        val motesSellPrice: Double? = null,
        @SerialName("name")
        val name: String,
        @SerialName("npc_sell_price")
        val npcSellPrice: Double? = null,
    )
}
