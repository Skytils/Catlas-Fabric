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
package gg.skytils.skytilsmod.utils

import com.mojang.blaze3d.systems.RenderSystem
import gg.essential.elementa.utils.withAlpha
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import gg.skytils.skytilsmod.Skytils.mc
import net.minecraft.client.render.VertexFormats
import net.minecraft.util.math.Box
import java.awt.Color

object RenderUtil {


    internal fun <T> Color.withParts(block: (Int, Int, Int, Int) -> T) =
        block(this.red, this.green, this.blue, this.alpha)

    fun drawFilledBoundingBox(matrixStack: UMatrixStack, aabb: Box, c: Color, alphaMultiplier: Float = 1f) {
        UGraphics.enableBlend()
        UGraphics.disableLighting()
        UGraphics.tryBlendFuncSeparate(770, 771, 1, 0)
        val wr = UGraphics.getFromTessellator()
        wr.beginWithDefaultShader(UGraphics.DrawMode.QUADS, VertexFormats.POSITION_COLOR)
        val adjustedAlpha = (c.alpha * alphaMultiplier).toInt().coerceAtMost(255)

        // vertical
        c.withAlpha(adjustedAlpha).withParts { r, g, b, a ->
            // bottom
            wr.pos(matrixStack, aabb.minX, aabb.minY, aabb.minZ).color(r, g, b, a).endVertex()
            wr.pos(matrixStack, aabb.maxX, aabb.minY, aabb.minZ).color(r, g, b, a).endVertex()
            wr.pos(matrixStack, aabb.maxX, aabb.minY, aabb.maxZ).color(r, g, b, a).endVertex()
            wr.pos(matrixStack, aabb.minX, aabb.minY, aabb.maxZ).color(r, g, b, a).endVertex()
            // top
            wr.pos(matrixStack, aabb.minX, aabb.maxY, aabb.maxZ).color(r, g, b, a).endVertex()
            wr.pos(matrixStack, aabb.maxX, aabb.maxY, aabb.maxZ).color(r, g, b, a).endVertex()
            wr.pos(matrixStack, aabb.maxX, aabb.maxY, aabb.minZ).color(r, g, b, a).endVertex()
            wr.pos(matrixStack, aabb.minX, aabb.maxY, aabb.minZ).color(r, g, b, a).endVertex()
        }

        // x axis
        c.withParts { r, g, b, a ->
            Color(
                (r * 0.8f).toInt(),
                (g * 0.8f).toInt(),
                (b * 0.8f).toInt(),
                adjustedAlpha
            )
        }.withParts { r, g, b, a ->
            // west
            wr.pos(matrixStack, aabb.minX, aabb.minY, aabb.maxZ).color(r, g, b, a).endVertex()
            wr.pos(matrixStack, aabb.minX, aabb.maxY, aabb.maxZ).color(r, g, b, a).endVertex()
            wr.pos(matrixStack, aabb.minX, aabb.maxY, aabb.minZ).color(r, g, b, a).endVertex()
            wr.pos(matrixStack, aabb.minX, aabb.minY, aabb.minZ).color(r, g, b, a).endVertex()
            // east
            wr.pos(matrixStack, aabb.maxX, aabb.minY, aabb.minZ).color(r, g, b, a).endVertex()
            wr.pos(matrixStack, aabb.maxX, aabb.maxY, aabb.minZ).color(r, g, b, a).endVertex()
            wr.pos(matrixStack, aabb.maxX, aabb.maxY, aabb.maxZ).color(r, g, b, a).endVertex()
            wr.pos(matrixStack, aabb.maxX, aabb.minY, aabb.maxZ).color(r, g, b, a).endVertex()
        }

        // z axis
        c.withParts { r, g, b, a ->
            Color(
                (r * 0.9f).toInt(),
                (g * 0.9f).toInt(),
                (b * 0.9f).toInt(),
                adjustedAlpha
            )
        }.withParts { r, g, b, a ->
            // north
            wr.pos(matrixStack, aabb.minX, aabb.maxY, aabb.minZ).color(r, g, b, a).endVertex()
            wr.pos(matrixStack, aabb.maxX, aabb.maxY, aabb.minZ).color(r, g, b, a).endVertex()
            wr.pos(matrixStack, aabb.maxX, aabb.minY, aabb.minZ).color(r, g, b, a).endVertex()
            wr.pos(matrixStack, aabb.minX, aabb.minY, aabb.minZ).color(r, g, b, a).endVertex()
            // south
            wr.pos(matrixStack, aabb.minX, aabb.minY, aabb.maxZ).color(r, g, b, a).endVertex()
            wr.pos(matrixStack, aabb.maxX, aabb.minY, aabb.maxZ).color(r, g, b, a).endVertex()
            wr.pos(matrixStack, aabb.maxX, aabb.maxY, aabb.maxZ).color(r, g, b, a).endVertex()
            wr.pos(matrixStack, aabb.minX, aabb.maxY, aabb.maxZ).color(r, g, b, a).endVertex()
        }

        wr.drawDirect()
        UGraphics.disableBlend()
        UGraphics.enableLighting()
    }

    fun getViewerPos(partialTicks: Float): Triple<Double, Double, Double> {
        val viewer = mc.cameraEntity!!
        val viewerX = viewer.lastRenderX + (viewer.x - viewer.lastRenderX) * partialTicks
        val viewerY = viewer.lastRenderY + (viewer.y - viewer.lastRenderY) * partialTicks
        val viewerZ = viewer.lastRenderZ + (viewer.z - viewer.lastRenderZ) * partialTicks
        return Triple(viewerX, viewerY, viewerZ)
    }

    /*
    * @link https://stackoverflow.com/a/54913292
    */
    fun mixColors(vararg colors: Color): Color {
        val ratio = 1f / colors.size.toFloat()
        var r = 0
        var g = 0
        var b = 0
        var a = 0
        for (color in colors) {
            r += (color.red * ratio).toInt()
            g += (color.green * ratio).toInt()
            b += (color.blue * ratio).toInt()
            a += (color.alpha * ratio).toInt()
        }
        return Color(r, g, b, a)
    }

    fun interpolate(currentValue: Double, lastValue: Double, multiplier: Float): Double {
        return lastValue + (currentValue - lastValue) * multiplier
    }
}

fun Color.bindColor() = RenderSystem.setShaderColor(this.red / 255f, this.green / 255f, this.blue / 255f, this.alpha / 255f)
fun Color.withAlpha(alpha: Int): Int = (alpha.coerceIn(0, 255) shl 24) or (this.rgb and 0x00ffffff)

fun Color.multAlpha(mult: Float) = Color(
    red,
    green,
    blue,
    (alpha * mult).toInt().coerceIn(0, 255)
)

fun Box.expandBlock(): Box =
    expand(0.0020000000949949026, 0.0020000000949949026, 0.0020000000949949026)