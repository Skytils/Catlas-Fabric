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
package gg.skytils.skytilsmod.features.impl.dungeons

import gg.essential.api.EssentialAPI
import gg.essential.universal.UChat
import gg.essential.universal.UMatrixStack
import gg.skytils.event.EventPriority
import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.TickEvent
import gg.skytils.event.impl.entity.BossBarSetEvent
import gg.skytils.event.impl.entity.LivingEntityDeathEvent
import gg.skytils.event.impl.item.ItemTooltipEvent
import gg.skytils.event.impl.play.ChatMessageReceivedEvent
import gg.skytils.event.impl.play.ChatMessageSentEvent
import gg.skytils.event.impl.play.WorldUnloadEvent
import gg.skytils.event.impl.render.CheckRenderEntityEvent
import gg.skytils.event.impl.render.LivingEntityPreRenderEvent
import gg.skytils.event.impl.render.WorldDrawEvent
import gg.skytils.event.impl.screen.GuiContainerSlotClickEvent
import gg.skytils.event.impl.screen.ScreenOpenEvent
import gg.skytils.event.impl.world.BlockStateUpdateEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod.Skytils.prefix
import gg.skytils.skytilsmod._event.DungeonPuzzleResetEvent
import gg.skytils.skytilsmod._event.MainThreadPacketReceiveEvent
import gg.skytils.skytilsmod._event.PacketReceiveEvent
import gg.skytils.skytilsmod._event.PacketSendEvent
import gg.skytils.skytilsmod.core.GuiManager
import gg.skytils.skytilsmod.core.structure.GuiElement
import gg.skytils.skytilsmod.features.impl.dungeons.catlas.handlers.DungeonInfo
import gg.skytils.skytilsmod.features.impl.handlers.MayorInfo
import gg.skytils.skytilsmod.listeners.DungeonListener
import gg.skytils.skytilsmod.mixins.transformers.accessors.AccessorC0EPacketClickWindow
import gg.skytils.skytilsmod.utils.*
import gg.skytils.skytilsmod.utils.ItemUtil.setLore
import gg.skytils.skytilsmod.utils.Utils.equalsOneOf
import gg.skytils.skytilsmod.utils.graphics.ScreenRenderer
import gg.skytils.skytilsmod.utils.graphics.SmartFontRenderer.TextAlignment
import gg.skytils.skytilsmod.utils.graphics.colors.CommonColors
import net.minecraft.client.network.OtherClientPlayerEntity
import net.minecraft.client.gui.screen.Screen
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.class_0_1003
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.entity.ItemEntity
import net.minecraft.entity.mob.BlazeEntity
import net.minecraft.entity.mob.AbstractSkeletonEntity
import net.minecraft.entity.passive.BatEntity
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.block.Blocks
import net.minecraft.item.Items
import net.minecraft.screen.GenericContainerScreenHandler
import net.minecraft.item.PlayerHeadItem
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket
import net.minecraft.network.play.server.*
import net.minecraft.util.math.Box
import net.minecraft.util.math.BlockPos
import net.minecraft.text.LiteralTextContent
import java.awt.Color

object DungeonFeatures : EventSubscriber {
    private val deathOrPuzzleFail =
        Regex("^ ☠ .+ and became a ghost\\.$|^PUZZLE FAIL! .+$|^\\[STATUE] Oruo the Omniscient: .+ chose the wrong answer!")
    private val thornMissMessages = arrayOf(
        "chickens",
        "shot",
        "dodg", "thumbs",
        "aim"
    )
    var dungeonFloor: String? = null
        set(value) {
            field = value
            dungeonFloorNumber = value?.drop(1)?.ifEmpty { "0" }?.toIntOrNull()
        }
    var dungeonFloorNumber: Int? = null
        private set
    var hasBossSpawned = false
    private var isInTerracottaPhase = false
    private var terracottaEndTime = -1.0
    private var rerollClicks = 0
    private var alertedSpiritPet = false
    private val SPIRIT_PET_TEXTURES = setOf(
        "ewogICJ0aW1lc3RhbXAiIDogMTU5NTg2MjAyNjE5OSwKICAicHJvZmlsZUlkIiA6ICI0ZWQ4MjMzNzFhMmU0YmI3YTVlYWJmY2ZmZGE4NDk1NyIsCiAgInByb2ZpbGVOYW1lIiA6ICJGaXJlYnlyZDg4IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzhkOWNjYzY3MDY3N2QwY2ViYWFkNDA1OGQ2YWFmOWFjZmFiMDlhYmVhNWQ4NjM3OWEwNTk5MDJmMmZlMjI2NTUiCiAgICB9CiAgfQp9",
        "ewogICJ0aW1lc3RhbXAiIDogMTY5OTU1NDAwMzI4MywKICAicHJvZmlsZUlkIiA6ICJlOTgxNDA1MTJiNmQ0MzVhOWQwYzdmY2RjMzQxM2M3OSIsCiAgInByb2ZpbGVOYW1lIiA6ICJOYXphcmJla0FsZGEiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjhmMzNhNDkxODVjMDdhZTIxZjNiNGQ1YTU2OWFjZDEyYWUxMTE1N2U0OTZjY2NjMjY0ODdlZDFiMDlkZWQzZiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9"
    )
    private var lastLitUpTime = -1L
    private val lastBlockPos = BlockPos(7, 77, 34)
    private var startWithoutFullParty = false
    private var blazes = 0
    var hasClearedText = false
    private var terracottaSpawns = hashMapOf<BlockPos, Long>()
    private val dungeonMobSpawns = setOf(
        "Lurker",
        "Dreadlord",
        "Souleater",
        "Zombie",
        "Skeleton",
        "Skeletor",
        "Sniper",
        "Super Archer",
        "Spider",
        "Fels",
        "Withermancer"
    )

    init {
        DungeonSecretDisplay
        SpiritBearSpawnTimer()
    }

    override fun setup() {
        register(::onBlockChange)
        register(::onReceivePacketHighest, EventPriority.Highest)
        register(::onTick)
        register(::onTickLowest, EventPriority.Lowest)
        register(::onPacketSend, EventPriority.Highest)
        register(::onDeath, EventPriority.Highest)
        register(::onChat, EventPriority.Highest)
        register(::onSendChatMessage)
        register(::onRenderLivingPre)
        register(::onRenderWorld)
        register(::onReceivePacket, EventPriority.Lowest)
        register(::onGuiOpen)
        register(::onSlotClick, EventPriority.High)
        register(::onTooltip, EventPriority.Lowest)
        register(::onWorldChange)
        register(::onBossBarSet)
        register(::onCheckRender)
        register(::onPuzzleReset)
    }

    fun onBlockChange(event: BlockStateUpdateEvent) {
        if (hasBossSpawned && Skytils.config.spiritBearTimer && dungeonFloor?.endsWith('4') == true) {
            if (event.pos == lastBlockPos) {
                lastLitUpTime =
                    if (event.update.block === Blocks.SEA_LANTERN && event.old.block === Blocks.COAL_BLOCK) System.currentTimeMillis() else -1L
                printDevMessage({ "change light $lastLitUpTime" }, "spiritbear")
            }
        } else if (isInTerracottaPhase && Skytils.config.terracottaRespawnTimer && dungeonFloor?.endsWith('6') == true) {
            if (event.old.block == Blocks.AIR && event.update.block == Blocks.FLOWER_POT) {
                // TODO: verify M6 time
                terracottaSpawns[event.pos] = System.currentTimeMillis() + if (dungeonFloor == "F6") 15000 else 12000
            }
        }
    }

    fun onReceivePacketHighest(event: PacketReceiveEvent<*>) {
        event.apply {
            if (hasBossSpawned && Skytils.config.spiritBearTimer && dungeonFloor?.endsWith('4') == true) {
                when (packet) {
                    is BlockUpdateS2CPacket -> {
                        if (packet.pos == lastBlockPos) {
                            lastLitUpTime =
                                if (packet.field_12051.block === Blocks.SEA_LANTERN) System.currentTimeMillis() else -1L
                            printDevMessage({ "light $lastLitUpTime" }, "spiritbear")
                        }
                    }

                    is GameMessageS2CPacket -> {
                        if (lastLitUpTime != -1L && packet.message.method_10865() == "§r§a§lA §r§5§lSpirit Bear §r§a§lhas appeared!§r") {
                            printDevMessage({ "chat ${System.currentTimeMillis() - lastLitUpTime}" }, "spiritbear")
                            lastLitUpTime = -1L
                        }
                    }

                    is PlayerSpawnS2CPacket -> {
                        if (lastLitUpTime != -1L && packet.playerUuid.version() == 2) {
                            printDevMessage({ "spawn ${System.currentTimeMillis() - lastLitUpTime}" }, "spiritbear")
                            //lastLitUpTime = -1L
                        }
                    }
                }
            }
        }
    }

    fun onTick(event: TickEvent) {
        if (mc.player == null || mc.world == null) return
        if (Utils.inDungeons) {
            if (dungeonFloor == null) {
                ScoreboardUtil.sidebarLines.find {
                    it.contains("The Catacombs (")
                }?.let {
                    dungeonFloor = it.substringAfter("(").substringBefore(")")
                    ScoreCalculation.floorReq.set(
                        ScoreCalculation.floorRequirements[dungeonFloor]
                            ?: ScoreCalculation.floorRequirements["default"]!!
                    )
                }
            }
            if (!hasClearedText) {
                hasClearedText = ScoreboardUtil.sidebarLines.any { it.startsWith("Cleared: ") }
            }
            if (isInTerracottaPhase && terracottaEndTime > 0 && Skytils.config.showSadanInterest) {
                val timeLeft = terracottaEndTime - System.currentTimeMillis()
                    .toDouble() / 1000f
                if (timeLeft >= 0) {
                    class_0_1003.field_0_4178 = timeLeft.toFloat() / if (dungeonFloor == "F6") 105 else 115
                    class_0_1003.field_0_4179 = 100
                    class_0_1003.field_0_4180 = "§r§c§lSadan's Interest: §r§6" + timeLeft.toInt() + "s"
                    class_0_1003.field_0_4181 = false
                } else {
                    terracottaEndTime = -2.0
                }
            }

            if (Skytils.config.spiritPetWarning && !alertedSpiritPet && DungeonTimer.dungeonStartTime == -1L && mc.world.entities.any {
                    if (it !is ArmorStandEntity || it.hasCustomName()) return@any false
                    val item = it.method_0_7087() ?: return@any false
                    if (item.item !is PlayerHeadItem) return@any false
                    return@any (SPIRIT_PET_TEXTURES.contains(ItemUtil.getSkullTexture(item)))
                }) {
                UChat.chat(
                    "$prefix §cSomeone in your party has a Spirit Pet equipped!"
                )
                GuiManager.createTitle("Spirit Pet", 20)
                alertedSpiritPet = true
            }
        }
    }

    var fakeDungeonMap: ItemStack? = null
    var intendedItemStack: ItemStack? = null

    fun onTickLowest(event: TickEvent) {
        if (!Utils.inDungeons) return
        if (Skytils.config.injectFakeDungeonMap && DungeonTimer.bossEntryTime == -1L) {
            (DungeonInfo.dungeonMap ?: DungeonInfo.guessMapData)?.let {
                val slot = mc.player?.inventory?.getStack(8)
                if (slot != null && slot.item != Items.FILLED_MAP) {
                    if (fakeDungeonMap == null) {
                        val guessMapId = it.key.substringAfter("map_").toIntOrNull()
                        if (guessMapId == null) {
                            mc.world.method_0_325("map_-1337", it)
                        }
                        fakeDungeonMap = ItemStack(Items.FILLED_MAP, 1337, guessMapId ?: -1337).also {
                            it.setCustomName("§bMagical Map")
                            it.setLore(listOf("§7Shows the layout of the Dungeon as", "§7it is explored and completed.", "", "§cThis isn't the real map!", "§eSkytils injected this data in for you."))
                        }
                    }
                    intendedItemStack = slot
                    mc.player.inventory.setStack(8, fakeDungeonMap)
                }
            }.ifNull {
                fakeDungeonMap = null
                intendedItemStack = null
            }
        } else {
            fakeDungeonMap = null
            intendedItemStack = null
        }
    }

    fun onPacketSend(event: PacketSendEvent<*>) {
        if (fakeDungeonMap != null && event.packet is ClickSlotC2SPacket && event.packet.stack == fakeDungeonMap) {
            (event.packet as AccessorC0EPacketClickWindow).setClickedItem(intendedItemStack)
        }
    }

    fun onBossBarSet(event: BossBarSetEvent) {
        if (!Utils.inDungeons) return
        val displayData = event.data
        val unformatted = displayData.displayName.string.stripControlCodes()
        if (dungeonFloorNumber == 7) {
            if (equalsOneOf(unformatted, "Maxor", "Storm", "Goldor", "Necron")) {
                when (Skytils.config.necronHealth) {
                    2 -> {
                        class_0_1003.field_0_4178 = displayData.health / displayData.maxHealth
                        class_0_1003.field_0_4179 = 100
                        class_0_1003.field_0_4180 = displayData.displayName.method_10865() + "§r§8 - §r§d" + String.format(
                            "%.1f",
                            class_0_1003.field_0_4178 * 100
                        ) + "%"
                        class_0_1003.field_0_4181 = event.hasColorModifier
                        event.cancelled = true
                    }

                    1 -> {
                        class_0_1003.field_0_4178 = displayData.health / displayData.maxHealth
                        class_0_1003.field_0_4179 = 100
                        val isMaster = dungeonFloor == "M7"
                        val health = when (unformatted) {
                            "Maxor" -> if (isMaster) 800_000_000 else 100_000_000
                            "Storm" -> if (isMaster) 1_000_000_000 else 400_000_000
                            "Goldor" -> if (isMaster) 1_200_000_000 else 750_000_000
                            "Necron" -> if (isMaster) 1_400_000_000 else 1_000_000_000
                            else -> 69
                        }
                        class_0_1003.field_0_4180 = displayData.displayName.method_10865() + "§r§8 - §r§a" + NumberUtil.format(
                            (class_0_1003.field_0_4178 * health).toLong()
                        ) + "§r§8/§r§a${NumberUtil.format(health)}§r§c❤"
                        class_0_1003.field_0_4181 = event.hasColorModifier
                        event.cancelled = true
                    }

                    0 -> {
                    }
                }
            }
            return
        }
        if (dungeonFloorNumber == 6) {
            if (terracottaEndTime == -1.0) {
                if (unformatted.contains("Sadan's Interest Level")) {
                    val length = if (dungeonFloor == "F6") 105 else 115
                    terracottaEndTime = System.currentTimeMillis().toDouble() / 1000f + length
                }
            } else if (terracottaEndTime > 0 && Skytils.config.showSadanInterest) {
                event.cancelled = true
            }
            return
        }
    }

    fun onDeath(event: LivingEntityDeathEvent) {
        if (!Utils.inDungeons) return
        if (event.entity is OtherClientPlayerEntity && terracottaEndTime > 0 && event.entity.name == "Terracotta ") {
            //for some reason this event fires twice for players
            printDevMessage("terracotta died", "terracotta")
            terracottaEndTime -= 1
        }
        if (event.entity is BlazeEntity && ++blazes == 10 && Skytils.config.sayBlazeDone) {
            Skytils.sendMessageQueue.add("/pc Blaze Done")
        }
    }

    fun onPuzzleReset(event: DungeonPuzzleResetEvent) {
        if (!Utils.inDungeons) return
        if (event.puzzle == "Higher Or Lower") {
            blazes = 0
        }
    }

    fun onChat(event: ChatMessageReceivedEvent) {
        if (!Utils.inSkyblock) return
        val unformatted = event.message.string.stripControlCodes()
        if (Utils.inDungeons) {
            if (Skytils.config.autoCopyFailToClipboard) {
                if (deathOrPuzzleFail.containsMatchIn(unformatted) || (unformatted.startsWith("[CROWD]") && thornMissMessages.any {
                        unformatted.contains(
                            it,
                            true
                        )
                    } && DungeonListener.team.keys.any { unformatted.contains(it) })) {
                    if (!unformatted.contains("disconnect")) {
                        Screen.method_0_2797(unformatted)
                        UChat.chat("$prefix §aCopied fail to clipboard.")
                    }
                    event.message.style
                        .withHoverEvent(
                            HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                LiteralTextContent("§aClick to copy to clipboard.")
                            )
                        ).withClickEvent(
                        ClickEvent(ClickEvent.Action.RUN_COMMAND, "/skytilscopy $unformatted"))
                }
            }
            if (Skytils.config.hideF4Spam && unformatted.startsWith("[CROWD]") && thornMissMessages.none {
                    unformatted.contains(
                        it,
                        true
                    )
                }
            ) {
                event.cancelled = true
            }
            if (unformatted.startsWith("[BOSS]") && unformatted.contains(":")) {
                val bossName = unformatted.substringAfter("[BOSS] ").substringBefore(":").trim()
                if (!hasBossSpawned && bossName != "The Watcher" && dungeonFloor != null && Utils.checkBossName(
                        dungeonFloor!!,
                        bossName
                    )
                ) {
                    hasBossSpawned = true
                }
                if (bossName == "Sadan") {
                    if (unformatted.contains("So you made it all the way here")) {
                        isInTerracottaPhase = true
                    } else if (unformatted.contains("ENOUGH!") || unformatted.contains("It was inevitable.")) {
                        isInTerracottaPhase = false
                        terracottaSpawns.clear()
                    }
                }
            }
        }
    }

    fun onSendChatMessage(event: ChatMessageSentEvent) {
        if (event.message.startsWith("/skytilscopy") && !event.addToHistory) {
            UChat.chat("$prefix §aCopied to clipboard.")
            Screen.method_0_2797(event.message.substring("/skytilscopy ".length))
            event.cancelled = true
        }
    }

    fun onCheckRender(event: CheckRenderEntityEvent<*>) {
        if (!Utils.inDungeons) return
        val entity = event.entity
        if (Skytils.config.hideArcherBonePassive && (entity as? ItemEntity)?.stack?.damage == 15 && entity.stack.item === Items.DYE)
            event.cancelled = true
    }

    fun onRenderLivingPre(event: LivingEntityPreRenderEvent<*>) {
        if (Utils.inDungeons) {
            val matrixStack = UMatrixStack()
            if (Skytils.config.boxSpiritBow && hasBossSpawned && event.entity.isInvisible && equalsOneOf(
                    dungeonFloor,
                    "F4",
                    "M4"
                ) && event.entity is ArmorStandEntity && event.entity.method_0_7087()?.item == Items.field_8102
            ) {
                RenderSystem.disableCull()
                RenderSystem.disableDepthTest()
                val (vx, vy, vz) = RenderUtil.getViewerPos(RenderUtil.getPartialTicks())
                val (x, y, z) = RenderUtil.fixRenderPos(event.x - vx, event.y - vy, event.z - vz)
                RenderUtil.drawFilledBoundingBox(
                    matrixStack,
                    Box(x, y, z, x + 0.75, y + 1.975, z + 0.75),
                    Color(255, 0, 255, 200),
                    1f
                )
                RenderSystem.enableDepthTest()
                RenderSystem.enableCull()
            }
            if (event.entity is ArmorStandEntity && event.entity.isInvisible && Skytils.config.hideFairies && event.entity.method_0_7087() != null && ItemUtil.getSkullTexture(event.entity.method_0_7087()) == "ewogICJ0aW1lc3RhbXAiIDogMTcxOTQ2MzA5MTA0NywKICAicHJvZmlsZUlkIiA6ICIyNjRkYzBlYjVlZGI0ZmI3OTgxNWIyZGY1NGY0OTgyNCIsCiAgInByb2ZpbGVOYW1lIiA6ICJxdWludHVwbGV0IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzJlZWRjZmZjNmExMWEzODM0YTI4ODQ5Y2MzMTZhZjdhMjc1MmEzNzZkNTM2Y2Y4NDAzOWNmNzkxMDhiMTY3YWUiCiAgICB9CiAgfQp9") {
                event.cancelled = true
            }
            if (event.entity is ArmorStandEntity && event.entity.hasCustomName()) {
                if (Skytils.config.hideWitherMinerNametags) {
                    val name = event.entity.customName.stripControlCodes()
                    if (name.contains("Wither Miner") || name.contains("Wither Guard") || name.contains("Apostle")) {
                        mc.world.method_8463(event.entity)
                    }
                }
                if (Skytils.config.hideF4Nametags) {
                    val name = event.entity.customName.stripControlCodes()
                    if (name.contains("Spirit") && !name.contains("Spirit Bear")) {
                        mc.world.method_8463(event.entity)
                    }
                }
                if (Skytils.config.hideTerracotaNametags) {
                    val name = event.entity.customName.stripControlCodes()
                    if (name.contains("Terracotta ")) mc.world.method_8463(event.entity)
                }
                if (Skytils.config.hideNonStarredNametags) {
                    val name = event.entity.customName.stripControlCodes()
                    if (!name.startsWith("✯ ") && name.contains("❤") && dungeonMobSpawns.any { it in name }) {
                        mc.world.method_8463(event.entity)
                    }
                }
            }
            if (!mc.entityRenderDispatcher.shouldRenderHitboxes()) {
                if (!event.entity.isInvisible) {
                    if (event.entity is BatEntity && Skytils.config.showBatHitboxes && !hasBossSpawned &&
                        if (MayorInfo.currentMayor == "Derpy") equalsOneOf(
                            event.entity.maxHealth,
                            200f,
                            800f
                        ) else equalsOneOf(
                            event.entity.maxHealth,
                            100f,
                            400f
                        )
                    ) {
                        RenderUtil.drawOutlinedBoundingBox(
                            event.entity.boundingBox,
                            Color(0, 255, 255, 255),
                            3f,
                            RenderUtil.getPartialTicks()
                        )
                    } else if (event.entity is AbstractSkeletonEntity && Skytils.config.boxSkeletonMasters && ItemUtil.getSkyBlockItemID(
                            event.entity.method_0_7157(0)
                        ) == "SKELETON_MASTER_BOOTS"
                    ) {
                        RenderUtil.drawOutlinedBoundingBox(
                            event.entity.boundingBox,
                            Color(255, 107, 11, 255),
                            3f,
                            RenderUtil.getPartialTicks()
                        )
                    } else if (hasBossSpawned && Skytils.config.boxSpiritBears && event.entity.name == "Spirit Bear" && event.entity is OtherClientPlayerEntity) {
                        val (x, y, z) = RenderUtil.fixRenderPos(event.x, event.y, event.z)
                        val aabb = Box(
                            x - 0.5,
                            y,
                            z - 0.5,
                            x + 0.5,
                            y + 2,
                            z + 0.5
                        )
                        RenderUtil.drawOutlinedBoundingBox(
                            aabb,
                            Color(121, 11, 255, 255),
                            3f,
                            RenderUtil.getPartialTicks()
                        )
                    }
                } else {
                    if (!hasBossSpawned && Skytils.config.boxStarredMobs && event.entity is ArmorStandEntity && event.entity.hasCustomName() && event.entity.isCustomNameVisible) {
                        val name = event.entity.name
                        if (name.startsWith("§6✯ ") && name.endsWith("§c❤")) {
                            val (x, y, z) = RenderUtil.fixRenderPos(event.x, event.y, event.z)
                            val color = Skytils.config.boxStarredMobsColor
                            if ("Spider" in name) {
                                RenderUtil.drawOutlinedBoundingBox(
                                    Box(
                                        x - 0.625,
                                        y - 1,
                                        z - 0.625,
                                        x + 0.625,
                                        y - 0.25,
                                        z + 0.625
                                    ),
                                    color,
                                    3f,
                                    RenderUtil.getPartialTicks()
                                )
                            } else if ("Fels" in name || "Withermancer" in name) {
                                RenderUtil.drawOutlinedBoundingBox(
                                    Box(x - 0.5, y - 3, z - 0.5, x + 0.5, y, z + 0.5),
                                    color,
                                    3f,
                                    RenderUtil.getPartialTicks()
                                )
                            } else {
                                RenderUtil.drawOutlinedBoundingBox(
                                    Box(x - 0.5, y - 2, z - 0.5, x + 0.5, y, z + 0.5),
                                    color,
                                    3f,
                                    RenderUtil.getPartialTicks()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun onRenderWorld(event: WorldDrawEvent) {
        val stack = UMatrixStack()
        RenderSystem.disableCull()
        RenderSystem.disableDepthTest()
        terracottaSpawns.entries.removeAll {
            val diff = it.value - System.currentTimeMillis()
            RenderUtil.drawLabel(
                it.key.middleVec(),
                "${"%.2f".format(diff / 1000.0)}s",
                Color.WHITE,
                event.partialTicks,
                stack
            )
            return@removeAll diff < 0
        }
        RenderSystem.enableCull()
        RenderSystem.enableDepthTest()
    }

    fun onReceivePacket(event: MainThreadPacketReceiveEvent<*>) {
        if (!Utils.inSkyblock) return
        if (event.packet is TitleS2CPacket) {
            val packet = event.packet
            if (packet.text != null && mc.player != null) {
                val unformatted = packet.text.method_0_5147().stripControlCodes()
                if (Skytils.config.hideTerminalCompletionTitles && Utils.inDungeons && !unformatted.contains(mc.player.name) && (unformatted.contains(
                        "activated a terminal!"
                    ) || unformatted.contains("completed a device!") || unformatted.contains("activated a lever!"))
                ) {
                    event.cancelled = true
                    runCatching {
                        val slash = unformatted.indexOf("/")
                        val numBeforeSlash = unformatted[slash - 1].digitToInt()
                        val numAfterSlash = unformatted[slash + 1].digitToInt()
                        if (numBeforeSlash == 0 || numBeforeSlash == numAfterSlash) {
                            event.cancelled = false
                        }
                    }
                }
            }
        }
        if (event.packet is PlaySoundIdS2CPacket) {
            val packet = event.packet
            if (Skytils.config.disableTerracottaSounds && isInTerracottaPhase) {
                val sound = packet.method_11460()
                val pitch = packet.pitch
                val volume = packet.volume
                if (sound == "game.player.hurt" && pitch == 0f && volume == 0f) event.cancelled = true
                if (sound == "random.eat" && pitch == 0.6984127f && volume == 1f) event.cancelled = true
            }
        }
    }

    fun onGuiOpen(event: ScreenOpenEvent) {
        rerollClicks = 0
    }

    fun onSlotClick(event: GuiContainerSlotClickEvent) {
        if (!Utils.inDungeons) return
        if (event.container is GenericContainerScreenHandler) {
            val chestName = event.chestName
            when {
                chestName.endsWith(" Chest") -> {
                    if (Skytils.config.kismetRerollConfirm > 0 && event.slotId == 50) {
                        rerollClicks++
                        val neededClicks = Skytils.config.kismetRerollConfirm - rerollClicks
                        if (neededClicks > 0) {
                            event.cancelled = true
                        }
                    }
                }

                chestName == "Ready Up" -> {
                    if (!startWithoutFullParty && Skytils.config.noChildLeftBehind) {
                        val teamCount =
                            (DungeonListener.partyCountPattern.find(TabListUtils.tabEntries[0].second)?.groupValues?.get(
                                1
                            )?.toIntOrNull() ?: 0)
                        if (teamCount < 5) {
                            event.cancelled = true
                            EssentialAPI.getNotifications()
                                .push(
                                    "Party only has $teamCount members!",
                                    "Click me to disable this warning.",
                                    4f,
                                    action = {
                                        startWithoutFullParty = true
                                    })
                        }
                    }
                }
            }

        }
    }

    fun onTooltip(event: ItemTooltipEvent) {
        if (Utils.inDungeons && Skytils.config.kismetRerollConfirm > 0 && ItemUtil.getDisplayName(event.stack)
                .contains("Reroll") && SBInfo.lastOpenContainerName?.endsWith(" Chest") == true
        ) {
            for (i in event.tooltip.indices) {
                if (event.tooltip[i].contains("Click to reroll")) {
                    val neededClicks = Skytils.config.kismetRerollConfirm - rerollClicks
                    event.tooltip[i] = "§eClick §a$neededClicks§e times to reroll this chest!"
                    break
                }
            }
        }
    }

    fun onWorldChange(event: WorldUnloadEvent) {
        dungeonFloor = null
        hasBossSpawned = false
        isInTerracottaPhase = false
        terracottaEndTime = -1.0
        alertedSpiritPet = false
        lastLitUpTime = -1L
        startWithoutFullParty = false
        blazes = 0
        hasClearedText = false
        terracottaSpawns.clear()
        fakeDungeonMap = null
        intendedItemStack = null
    }

    class SpiritBearSpawnTimer : GuiElement("Spirit Bear Spawn Timer", x = 0.05f, y = 0.4f) {
        override fun render() {
            if (toggled && lastLitUpTime != -1L) {
                val time = lastLitUpTime + 3400
                val diff = time - System.currentTimeMillis()
                if (diff < 0) {
                    lastLitUpTime = -1
                }

                val leftAlign = scaleX < sr.scaledWidth / 2f
                val alignment = if (leftAlign) TextAlignment.LEFT_RIGHT else TextAlignment.RIGHT_LEFT
                ScreenRenderer.fontRenderer.drawString(
                    "Spirit Bear ${diff / 1000f}s",
                    if (leftAlign) 0f else width.toFloat(),
                    0f,
                    CommonColors.PURPLE,
                    alignment,
                    textShadow
                )
            }
        }

        override fun demoRender() {

            val leftAlign = scaleX < sr.scaledWidth / 2f
            val alignment = if (leftAlign) TextAlignment.LEFT_RIGHT else TextAlignment.RIGHT_LEFT
            ScreenRenderer.fontRenderer.drawString(
                "Spirit Bear: 3.4s",
                if (leftAlign) 0f else 0f + width,
                0f,
                CommonColors.PURPLE,
                alignment,
                textShadow
            )
        }

        override val height: Int
            get() = ScreenRenderer.fontRenderer.field_0_2811
        override val width: Int
            get() = ScreenRenderer.fontRenderer.getWidth("Spirit Bear: 3.4s")

        override val toggled: Boolean
            get() = Skytils.config.spiritBearTimer

        init {
            Skytils.guiManager.registerElement(this)
        }
    }

    object DungeonSecretDisplay : GuiElement("Dungeon Secret Display", x = 0.05f, y = 0.4f) {
        var secrets = -1
        var maxSecrets = -1

        override fun render() {
            if (toggled && Utils.inDungeons && maxSecrets > 0) {
                val leftAlign = scaleX < sr.scaledWidth / 2f
                val alignment = if (leftAlign) TextAlignment.LEFT_RIGHT else TextAlignment.RIGHT_LEFT
                val color = when (secrets / maxSecrets.toDouble()) {
                    in 0.0..0.5 -> CommonColors.RED
                    in 0.5..0.75 -> CommonColors.YELLOW
                    else -> CommonColors.GREEN
                }

                ScreenRenderer.fontRenderer.drawString(
                    "Secrets: ${secrets}/${maxSecrets}",
                    if (leftAlign) 0f else 0f + width,
                    0f,
                    color,
                    alignment,
                    textShadow
                )
            }
        }

        override fun demoRender() {
            val leftAlign = scaleX < sr.scaledWidth / 2f
            val alignment = if (leftAlign) TextAlignment.LEFT_RIGHT else TextAlignment.RIGHT_LEFT
            ScreenRenderer.fontRenderer.drawString(
                "Secrets: 0/0",
                if (leftAlign) 0f else 0f + width,
                0f,
                CommonColors.WHITE,
                alignment,
                textShadow
            )
        }

        override val height: Int
            get() = ScreenRenderer.fontRenderer.field_0_2811
        override val width: Int
            get() = ScreenRenderer.fontRenderer.getWidth("Secrets: 0/0")

        override val toggled: Boolean
            get() = Skytils.config.dungeonSecretDisplay

        init {
            Skytils.guiManager.registerElement(this)
        }
    }
}
