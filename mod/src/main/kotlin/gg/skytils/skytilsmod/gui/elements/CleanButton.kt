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
package gg.skytils.skytilsmod.gui.elements

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.widget.ClickableWidget
import com.mojang.blaze3d.systems.RenderSystem
import java.awt.Color

/**
 * Taken from ChatShortcuts under MIT License
 * https://github.com/P0keDev/ChatShortcuts/blob/master/LICENSE
 * @author P0keDev
 */
class CleanButton(buttonId: Int, x: Int, y: Int, widthIn: Int, heightIn: Int, buttonText: String?) :
    ClickableWidget(buttonId, x, y, widthIn, heightIn, buttonText) {
    constructor(buttonId: Int, x: Int, y: Int, buttonText: String?) : this(buttonId, x, y, 200, 20, buttonText)

    override fun render(mc: MinecraftClient, mouseX: Int, mouseY: Int) {
        if (visible) {
            val fontrenderer = mc.textRenderer
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
            hovered =
                mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height
            fill(
                x,
                y,
                x + width,
                y + height,
                if (hovered) Color(255, 255, 255, 80).rgb else Color(0, 0, 0, 80).rgb
            )
            method_1827(mc, mouseX, mouseY)
            var j = 14737632
            if (packedFGColour != 0) {
                j = packedFGColour
            } else if (!active) {
                j = 10526880
            } else if (hovered) {
                j = 16777120
            }
            method_1789(fontrenderer, message, x + width / 2, y + (height - 8) / 2, j)
        }
    }
}