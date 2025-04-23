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
package gg.skytils.skytilsmod.core.structure

import gg.skytils.skytilsmod.utils.RenderUtil
import net.minecraft.client.MinecraftClient
import net.minecraft.client.sound.SoundManager
import net.minecraft.client.gui.widget.ClickableWidget
import com.mojang.blaze3d.systems.RenderSystem
import java.awt.Color

class LocationButton(var element: GuiElement) : ClickableWidget(-1, 0, 0, null) {
    var x = 0f
    var y = 0f
    var x2 = 0f
    var y2 = 0f

    init {
        x = this.element.scaleX - 4
        y = this.element.scaleY - 4
        x2 = x + this.element.scaleWidth + 6
        y2 = y + this.element.scaleHeight + 6
    }

    private fun refreshLocations() {
        x = element.scaleX - 4
        y = element.scaleY - 4
        x2 = x + element.scaleWidth + 6
        y2 = y + element.scaleHeight + 6
    }

    override fun render(mc: MinecraftClient, mouseX: Int, mouseY: Int) {
        refreshLocations()
        hovered = mouseX >= x && mouseY >= y && mouseX < x2 && mouseY < y2
        val c = Color(255, 255, 255, if (hovered) 100 else 40)
        RenderUtil.drawRect(0.0, 0.0, (element.width + 4).toDouble(), (element.height + 4).toDouble(), c.rgb)
        RenderSystem.method_4348(2f, 2f, 0f)
        element.demoRender()
        RenderSystem.method_4348(-2f, -2f, 0f)
        if (hovered) {
            lastHoveredElement = element
        }
    }

    override fun clicked(mc: MinecraftClient, mouseX: Int, mouseY: Int): Boolean {
        return active && visible && hovered
    }

    /**
     * get rid of clicking noise
     */
    override fun playDownSound(soundHandlerIn: SoundManager) {}

    companion object {
        var lastHoveredElement: GuiElement? = null
    }
}