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
package gg.skytils.skytilsmod.features.impl.dungeons.solvers.terminals

import gg.skytils.event.EventPriority
import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.item.ItemTooltipEvent
import gg.skytils.event.impl.screen.GuiContainerForegroundDrawnEvent
import gg.skytils.event.impl.screen.GuiContainerSlotClickEvent
import gg.skytils.event.impl.screen.ScreenOpenEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod.utils.Utils
import gg.skytils.skytilsmod.utils.graphics.ScreenRenderer
import gg.skytils.skytilsmod.utils.graphics.SmartFontRenderer
import gg.skytils.skytilsmod.utils.graphics.colors.CommonColors
import net.minecraft.client.gui.screen.ingame.HandledScreen
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.screen.GenericContainerScreenHandler
import net.minecraft.util.DyeColor

object ChangeAllToSameColorSolver : EventSubscriber {
    private val ordering =
        setOf(
            DyeColor.RED,
            DyeColor.ORANGE,
            DyeColor.YELLOW,
            DyeColor.GREEN,
            DyeColor.BLUE
        ).withIndex().associate { (i, c) ->
            c.id to i
        }
    private var mostCommon = DyeColor.RED.id
    private var isLocked = false

    override fun setup() {
        register(::onForegroundEvent)
        register(::onSlotClick, EventPriority.High)
        register(::onTooltip, EventPriority.Lowest)
        register(::onWindowClose)
    }

    fun onWindowClose(event: ScreenOpenEvent) {
        if (event.screen as? HandledScreen == null) isLocked = false
    }


    fun onForegroundEvent(event: GuiContainerForegroundDrawnEvent) {
        if (!Utils.inDungeons || !Skytils.config.changeAllSameColorTerminalSolver || event.container !is GenericContainerScreenHandler || event.chestName != "Change all to same color!") return
        val container = event.container as? GenericContainerScreenHandler ?: return
        val grid = container.slots.filter {
            it.inventory == container.inventory && it.stack?.name?.startsWith("§a") == true
        }

        if (!Skytils.config.changeToSameColorLock || !isLocked) {
            val counts = ordering.keys.associateWith { c -> grid.count { it.stack?.method_0_8356() == c } }
            val currentPath = counts[mostCommon]!!
            val (candidate, maxCount) = counts.maxBy { it.value }

            if (maxCount > currentPath) {
                mostCommon = candidate
            }
            isLocked = true
        }

        val targetIndex = ordering[mostCommon]!!
        val mapping = grid.filter { it.stack.method_0_8356() != mostCommon }.associateWith { slot ->
            val stack = slot.stack
            val myIndex = ordering[stack.method_0_8356()]!!
            val normalCycle = ((targetIndex - myIndex) % ordering.size + ordering.size) % ordering.size
            val otherCycle = -((myIndex - targetIndex) % ordering.size + ordering.size) % ordering.size
            normalCycle to otherCycle
        }
        RenderSystem.pushMatrix()
        RenderSystem.method_4348(0f, 0f, 299f)
        for ((slot, clicks) in mapping) {
            var betterOpt = if (clicks.first > -clicks.second) clicks.second else clicks.first
            var color = CommonColors.WHITE
            if (Skytils.config.changeToSameColorMode == 1) {
                betterOpt = clicks.first
                when (betterOpt) {
                    1 -> color = CommonColors.GREEN
                    2, 3 -> color = CommonColors.YELLOW
                    4 -> color = CommonColors.RED
                }
            }

            RenderSystem.method_4406()
            RenderSystem.disableDepthTest()
            RenderSystem.disableBlend()
            ScreenRenderer.fontRenderer.drawString(
                "$betterOpt",
                slot.x + 9f,
                slot.y + 4f,
                color,
                SmartFontRenderer.TextAlignment.MIDDLE,
                SmartFontRenderer.TextShadow.NORMAL
            )
            RenderSystem.method_4394()
            RenderSystem.enableDepthTest()
        }
        RenderSystem.popMatrix()
    }

    fun onSlotClick(event: GuiContainerSlotClickEvent) {
        if (!Utils.inDungeons || !Skytils.config.changeAllSameColorTerminalSolver || !Skytils.config.blockIncorrectTerminalClicks) return
        if (event.container is GenericContainerScreenHandler && event.chestName == "Change all to same color!") {
            if (event.slot?.stack?.method_0_8356() == mostCommon) event.cancelled = true
        }
    }

    fun onTooltip(event: ItemTooltipEvent) {
        if (!Utils.inDungeons || !Skytils.config.changeAllSameColorTerminalSolver) return
        val chest = mc.player?.currentScreenHandler as? GenericContainerScreenHandler ?: return
        val chestName = chest.inventory.customName.string
        if (chestName == "Change all to same color!") {
            event.tooltip.clear()
        }
    }
}
