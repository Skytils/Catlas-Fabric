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

package gg.skytils.skytilsmod.features.impl.dungeons

import gg.essential.universal.UChat
import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.TickEvent
import gg.skytils.event.impl.play.WorldUnloadEvent
import gg.skytils.event.impl.render.LivingEntityPreRenderEvent
import gg.skytils.event.impl.world.BlockStateUpdateEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod.core.structure.GuiElement
import gg.skytils.skytilsmod.core.tickTask
import gg.skytils.skytilsmod.core.tickTimer
import gg.skytils.skytilsmod.utils.*
import gg.skytils.skytilsmod.utils.graphics.ScreenRenderer
import gg.skytils.skytilsmod.utils.graphics.SmartFontRenderer
import gg.skytils.skytilsmod.utils.graphics.colors.CommonColors
import gg.skytils.skytilsmod.utils.printDevMessage
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.sync.Mutex
import net.minecraft.class_0_308
import net.minecraft.entity.Entity
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.util.DyeColor
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.util.math.Box
import net.minecraft.util.math.BlockPos
import net.minecraft.util.Formatting
import net.minecraft.world.World
import java.awt.Color

object LividFinder : EventSubscriber {
    private var foundLivid = false
    var livid: Entity? = null
    private var lividTag: Entity? = null
    private val lividBlock = BlockPos(13, 107, 25)
    private var lock = Mutex()

    //https://wiki.hypixel.net/Livid
    val dyeToChar: Map<DyeColor, Formatting> = mapOf(
        DyeColor.WHITE to Formatting.WHITE,
        DyeColor.MAGENTA to Formatting.LIGHT_PURPLE,
        DyeColor.RED to Formatting.RED,
        DyeColor.GRAY to Formatting.GRAY,
        DyeColor.GREEN to Formatting.DARK_GREEN,
        DyeColor.LIME to Formatting.GREEN,
        DyeColor.BLUE to Formatting.BLUE,
        DyeColor.PURPLE to Formatting.DARK_PURPLE,
        DyeColor.YELLOW to Formatting.YELLOW
    )

    val charToName: Map<Formatting, String> = mapOf(
        Formatting.YELLOW to "Arcade",
        Formatting.WHITE to "Vendetta",
        Formatting.GRAY to "Doctor",
        Formatting.DARK_GREEN to "Frog",
        Formatting.DARK_PURPLE to "Purple",
        Formatting.RED to "Hockey",
        Formatting.LIGHT_PURPLE to "Crossed",
        Formatting.GREEN to "Smile",
        Formatting.BLUE to "Scream"
    )

    override fun setup() {
        register(::onTick)
        register(::onBlockChange)
        register(::onRenderLivingPre)
        register(::onWorldChange)
    }

    fun onTick(event: TickEvent) {
        if (mc.player == null || !Utils.inDungeons || DungeonFeatures.dungeonFloorNumber != 5 || !DungeonFeatures.hasBossSpawned || !Skytils.config.findCorrectLivid) return
        val blindnessDuration = mc.player.getStatusEffect(StatusEffect.field_0_7290)?.duration
        if ((!foundLivid || DungeonFeatures.dungeonFloor == "M5") && blindnessDuration != null) {
            if (lock.tryLock()) {
                printDevMessage("Starting livid job", "livid")
                tickTimer(blindnessDuration) {
                    runCatching {
                        if (mc.player.age > blindnessDuration) {
                            val state = mc.world.getBlockState(lividBlock)
                            val color = state.testProperty(class_0_308.field_0_1192)
                            val mapped = dyeToChar[color]
                            getLivid(color, mapped)
                        } else printDevMessage("Player changed worlds?", "livid")
                    }
                    lock.unlock()
                }
            } else printDevMessage("Livid job already started", "livid")
        }

        if (lividTag?.isRemoved == true || livid?.isRemoved == true) {
            printDevMessage("Livid is dead?", "livid")
        }
    }

    fun onBlockChange(event: BlockStateUpdateEvent) {
        if (mc.player == null || !Utils.inDungeons || DungeonFeatures.dungeonFloorNumber != 5 || !DungeonFeatures.hasBossSpawned || !Skytils.config.findCorrectLivid) return
        if (event.pos == lividBlock) {
            printDevMessage("Livid block changed", "livid")
            printDevMessage("block detection started", "livid")
            val color = event.update.testProperty(class_0_308.field_0_1192)
            val mapped = dyeToChar[color]
            printDevMessage({ "before blind ${color}" }, "livid")
            val blindnessDuration = mc.player.getStatusEffect(StatusEffect.field_0_7290)?.duration
            tickTimer(blindnessDuration ?: 2) {
                getLivid(color, mapped)
                printDevMessage("block detection done", "livid")
            }
        }
    }

    fun onRenderLivingPre(event: LivingEntityPreRenderEvent<*>) {
        if (!Utils.inDungeons) return
        if ((event.entity == lividTag) || (lividTag == null && event.entity == livid)) {
            val (x, y, z) = RenderUtil.fixRenderPos(event.x, event.y, event.z)
            val aabb = livid?.boundingBox ?: Box(
                x - 0.5,
                y - 2,
                z - 0.5,
                x + 0.5,
                y,
                z + 0.5
            )

            RenderUtil.drawOutlinedBoundingBox(
                aabb,
                Color(255, 107, 11, 255),
                3f,
                RenderUtil.getPartialTicks()
            )
        }
    }
    fun onWorldChange(event: WorldUnloadEvent) {
        lividTag = null
        livid = null
        foundLivid = false
    }

    fun getLivid(blockColor: DyeColor, mappedColor: Formatting?) {
        val lividType = charToName[mappedColor]
        if (lividType == null) {
            UChat.chat("${Skytils.failPrefix} §cBlock color ${blockColor.name} is not mapped correctly. Please report this to discord.gg/skytils")
            return
        }

        for (entity in mc.world.entities) {
            if (entity !is ArmorStandEntity) continue
            if (entity.customName.startsWith("$mappedColor﴾ $mappedColor§lLivid")) {
                lividTag = entity
                livid = mc.world.players.find { it.name == "$lividType Livid" }
                foundLivid = true
                return
            }
        }
        printDevMessage("No livid found!", "livid")
    }

    internal class LividGuiElement : GuiElement("Livid HP", x = 0.05f, y = 0.4f) {
        override fun render() {
            val player = mc.player
            val world: World? = mc.world
            if (toggled && Utils.inDungeons && player != null && world != null) {
                if (lividTag == null) return

                val leftAlign = scaleX < sr.scaledWidth / 2f
                val alignment = if (leftAlign) SmartFontRenderer.TextAlignment.LEFT_RIGHT else SmartFontRenderer.TextAlignment.RIGHT_LEFT
                ScreenRenderer.fontRenderer.drawString(
                    lividTag!!.name.replace("§l", ""),
                    if (leftAlign) 0f else width.toFloat(),
                    0f,
                    CommonColors.WHITE,
                    alignment,
                    textShadow
                )
            }
        }

        override fun demoRender() {
            val leftAlign = scaleX < sr.scaledWidth / 2f
            val text = "§r§f﴾ Livid §e6.9M§c❤ §f﴿"
            val alignment = if (leftAlign) SmartFontRenderer.TextAlignment.LEFT_RIGHT else SmartFontRenderer.TextAlignment.RIGHT_LEFT
            ScreenRenderer.fontRenderer.drawString(
                text,
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
            get() = ScreenRenderer.fontRenderer.getWidth("§r§f﴾ Livid §e6.9M§c❤ §f﴿")

        override val toggled: Boolean
            get() = Skytils.config.findCorrectLivid

        init {
            Skytils.guiManager.registerElement(this)
        }
    }

    init {
        LividGuiElement()
    }
}