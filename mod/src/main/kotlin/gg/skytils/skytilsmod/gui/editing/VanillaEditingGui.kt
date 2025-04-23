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
package gg.skytils.skytilsmod.gui.editing

import gg.essential.universal.UResolution
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.core.GuiManager
import gg.skytils.skytilsmod.core.PersistentSave
import gg.skytils.skytilsmod.core.structure.GuiElement
import gg.skytils.skytilsmod.core.structure.LocationButton
import gg.skytils.skytilsmod.core.structure.ResizeButton
import gg.skytils.skytilsmod.core.structure.ResizeButton.Corner
import gg.skytils.skytilsmod.gui.ReopenableGUI
import gg.skytils.skytilsmod.utils.graphics.SmartFontRenderer
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.screen.Screen
import com.mojang.blaze3d.systems.RenderSystem
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.Display
import java.awt.Color

open class VanillaEditingGui : Screen(), ReopenableGUI {
    private var xOffset = 0f
    private var yOffset = 0f
    private var resizing = false
    private var resizingCorner: Corner? = null
    private var dragging: GuiElement? = null
    private val locationButtons: MutableMap<GuiElement?, LocationButton> = HashMap()
    private var scaleCache = 0f
    override fun method_2222() = false

    override fun init() {
        for ((_, value) in Skytils.guiManager.elements) {
            val lb = LocationButton(value)
            widgetList.add(lb)
            locationButtons[value] = lb
        }
    }

    override fun render(mouseX: Int, mouseY: Int, partialTicks: Float) {
        onMouseMove(mouseX, mouseY)
        recalculateResizeButtons()
        fillGradient(0, 0, width, height, Color(0, 0, 0, 50).rgb, Color(0, 0, 0, 200).rgb)
        for (button in widgetList) {
            if (button is LocationButton) {
                if (button.element.toggled) {
                    RenderSystem.pushMatrix()
                    val scale = button.element.scale
                    RenderSystem.method_4348(button.x, button.y, 0f)
                    RenderSystem.method_4453(scale.toDouble(), scale.toDouble(), 1.0)
                    button.render(client, mouseX, mouseY)
                    RenderSystem.popMatrix()
                    if (button.isHovered) {
                        RenderSystem.method_4348(0f, 0f, 100f)
                        method_2211(listOf(button.element.name), mouseX, mouseY)
                        RenderSystem.method_4348(0f, 0f, -100f)
                    }
                }
            } else if (button is ResizeButton) {
                val element = button.element
                RenderSystem.pushMatrix()
                val scale = element.scale
                RenderSystem.method_4348(button.x, button.y, 0f)
                RenderSystem.method_4453(scale.toDouble(), scale.toDouble(), 1.0)
                button.render(client, mouseX, mouseY)
                RenderSystem.popMatrix()
            } else {
                button.render(client, mouseX, mouseY)
            }
        }
    }

    public override fun method_0_2778(button: ClickableWidget) {
        val sr = UResolution
        val minecraftScale = sr.scaleFactor.toFloat()
        val floatMouseX = Mouse.getX() / minecraftScale
        val floatMouseY = (client.field_0_2582 - Mouse.getY()) / minecraftScale
        if (button is LocationButton) {
            dragging = button.element
            xOffset = floatMouseX - dragging!!.scaleX
            yOffset = floatMouseY - dragging!!.scaleY
        } else if (button is ResizeButton) {
            dragging = button.element
            resizing = true
            scaleCache = button.element.scale
            xOffset = floatMouseX - button.x
            yOffset = floatMouseY - button.y
            resizingCorner = button.corner
        }
    }

    override fun method_0_2775(mouseX: Int, mouseY: Int, mouseButton: Int) {
        when (mouseButton) {
            1 -> widgetList.filterIsInstance<LocationButton>().filter { it.clicked(client, mouseX, mouseY) }.forEach {
                it.element.setPos(10, 10)
                it.element.scale = 1f
            }
            2 -> widgetList.filterIsInstance<LocationButton>().filter { it.clicked(client, mouseX, mouseY) }.forEach {
                it.element.textShadow = SmartFontRenderer.TextShadow.entries[(it.element.textShadow.ordinal + 1) % SmartFontRenderer.TextShadow.entries.size]
            }
        }
        super.method_0_2775(mouseX, mouseY, mouseButton)
    }

    /**
     * Set the coordinates when the mouse moves.
     */
    private fun onMouseMove(mouseX: Int, mouseY: Int) {
        val sr = UResolution
        val minecraftScale = sr.scaleFactor.toFloat()
        val floatMouseX = Mouse.getX() / minecraftScale
        val floatMouseY = (Display.getHeight() - Mouse.getY()) / minecraftScale
        if (resizing) { //TODO Fix rescaling for top right, top left, and bottom right corners
            val locationButton = locationButtons[dragging] ?: return
            when (resizingCorner) {
                Corner.BOTTOM_RIGHT -> {
                    val scaledX1 = locationButton.x
                    val scaledY1 = locationButton.y
                    val width = locationButton.x2 - locationButton.x
                    val height = locationButton.y2 - locationButton.y
                    val newWidth = floatMouseX - scaledX1
                    val newHeight = floatMouseY - scaledY1
                    val scaleX = newWidth / width
                    val scaleY = newHeight / height
                    val newScale = scaleX.coerceAtLeast(scaleY / 2).coerceAtLeast(0.01f)
                    locationButton.element.scale *= newScale
                }

                Corner.TOP_LEFT -> {
                }

                Corner.TOP_RIGHT -> {
                    val scaledX = locationButton.x
                    val scaledY = locationButton.y2
                    val width = locationButton.x2 - locationButton.x
                    val height = locationButton.y2 - locationButton.y
                    val newWidth = floatMouseX - scaledX
                    val newHeight = scaledY - floatMouseY
                    val scaleX = newWidth / width
                    val scaleY = newHeight / height
                    val newScale = scaleX.coerceAtLeast(scaleY).coerceAtLeast(0.01f)
                    locationButton.element.scale *= newScale
                    locationButton.element.setPos(locationButton.element.x, (scaledY - newHeight) / sr.scaledHeight)
                }

                Corner.BOTTOM_LEFT -> {
                }

                null -> {}
            }

            locationButton.render(client, mouseX, mouseY)
            recalculateResizeButtons()
        } else if (dragging != null) {
            val x = (floatMouseX - xOffset) / sr.scaledWidth.toFloat()
            val y = (floatMouseY - yOffset) / sr.scaledHeight.toFloat()
            dragging!!.setPos(x, y)
            addResizeCorners(dragging!!)
        }
    }

    private fun addResizeCorners(element: GuiElement) {
        widgetList.removeIf { button: ClickableWidget? -> button is ResizeButton && button.element === element }
        widgetList.removeIf { button: ClickableWidget? -> button is ResizeButton && button.element !== element }
        val locationButton = locationButtons[element] ?: return
        val boxXOne = locationButton.x - ResizeButton.SIZE * element.scale
        val boxXTwo = locationButton.x + element.scaleWidth + ResizeButton.SIZE * 2 * element.scale
        val boxYOne = locationButton.y - ResizeButton.SIZE * element.scale
        val boxYTwo = locationButton.y + element.scaleHeight + ResizeButton.SIZE * 2 * element.scale
        widgetList.add(ResizeButton(boxXOne, boxYOne, element, Corner.TOP_LEFT))
        widgetList.add(ResizeButton(boxXTwo, boxYOne, element, Corner.TOP_RIGHT))
        widgetList.add(ResizeButton(boxXOne, boxYTwo, element, Corner.BOTTOM_LEFT))
        widgetList.add(ResizeButton(boxXTwo, boxYTwo, element, Corner.BOTTOM_RIGHT))
    }

    private fun recalculateResizeButtons() {
        for (button in widgetList) {
            if (button is ResizeButton) {
                val corner = button.corner
                val element = button.element
                val locationButton = locationButtons[element] ?: continue
                val boxXOne = locationButton.x - ResizeButton.SIZE * element.scale
                val boxXTwo = locationButton.x + element.scaleWidth + ResizeButton.SIZE * element.scale
                val boxYOne = locationButton.y - ResizeButton.SIZE * element.scale
                val boxYTwo = locationButton.y + element.scaleHeight + ResizeButton.SIZE * element.scale
                when (corner) {
                    Corner.TOP_LEFT -> {
                        button.x = boxXOne
                        button.y = boxYOne
                    }

                    Corner.TOP_RIGHT -> {
                        button.x = boxXTwo
                        button.y = boxYOne
                    }

                    Corner.BOTTOM_LEFT -> {
                        button.x = boxXOne
                        button.y = boxYTwo
                    }

                    Corner.BOTTOM_RIGHT -> {
                        button.x = boxXTwo
                        button.y = boxYTwo
                    }
                }
            }
        }
    }


    override fun method_0_2801() {
        super.method_0_2801()
        val hovered = LocationButton.lastHoveredElement
        if (hovered != null) {
            hovered.scale = (hovered.scale + Mouse.getEventDWheel() / 1000f).coerceAtLeast(0.01f)
        }
    }

    /**
     * Reset the dragged feature when the mouse is released.
     */
    override fun method_0_2787(mouseX: Int, mouseY: Int, state: Int) {
        super.method_0_2787(mouseX, mouseY, state)
        dragging = null
        resizing = false
        scaleCache = 0f
    }

    /**
     * Saves the positions when the gui is closed
     */
    override fun removed() {
        PersistentSave.markDirty<GuiManager>()
    }
}