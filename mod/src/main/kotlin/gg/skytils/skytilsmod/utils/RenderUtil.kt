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

import gg.essential.elementa.font.DefaultFonts
import gg.essential.elementa.font.ElementaFonts
import gg.essential.elementa.utils.withAlpha
import gg.essential.universal.ChatColor
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import gg.essential.universal.UResolution
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod.core.structure.GuiElement
import gg.skytils.skytilsmod.mixins.hooks.renderer.skipGlint
import gg.skytils.skytilsmod.mixins.transformers.accessors.AccessorRenderManager
import gg.skytils.skytilsmod.utils.graphics.ScreenRenderer
import gg.skytils.skytilsmod.utils.graphics.SmartFontRenderer
import gg.skytils.skytilsmod.utils.graphics.colors.CommonColors
import net.minecraft.block.Block
import net.minecraft.client.gui.DrawContext
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.render.WorldRenderer
import net.minecraft.client.render.DiffuseLighting
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormats
import net.minecraft.entity.Entity
import net.minecraft.screen.slot.Slot
import net.minecraft.item.ItemStack
import net.minecraft.util.*
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object RenderUtil {
    private val RARITY = Identifier("skytils", "gui/rarity.png")
    private val RARITY2 = Identifier("skytils", "gui/rarity2.png")
    private val RARITY3 = Identifier("skytils", "gui/rarity3.png")
    private val RARITY4 = Identifier("skytils", "gui/rarity4.png")
    private val CUSTOMRARITY = Identifier("skytils", "gui/customrarity.png")
    private val beaconBeam = Identifier("textures/entity/beacon_beam.png")
    private val mutex = ReentrantLock()

    /**
     * Taken from NotEnoughUpdates under Creative Commons Attribution-NonCommercial 3.0
     * https://github.com/Moulberry/NotEnoughUpdates/blob/master/LICENSE
     * @author Moulberry
     * @author Mojang
     */
    fun renderBeaconBeam(x: Double, y: Double, z: Double, rgb: Int, alphaMultiplier: Float, partialTicks: Float) {
        val height = 300
        val bottomOffset = 0
        val topOffset = bottomOffset + height
        val tessellator = Tessellator.getInstance()
        val worldrenderer = tessellator.buffer
        mc.textureManager.bindTextureInner(beaconBeam)
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, 10497.0f)
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, 10497.0f)
        RenderSystem.method_4406()
        RenderSystem.enableCull()
        RenderSystem.method_4397()
        RenderSystem.blendFuncSeparate(770, 1, 1, 0)
        RenderSystem.enableBlend()
        RenderSystem.blendFuncSeparate(770, 771, 1, 0)
        val time = mc.world.time + partialTicks.toDouble()
        val d1 = MathHelper.method_0_6636(
            -time * 0.2 - MathHelper.floor(-time * 0.1)
                .toDouble()
        )
        val r = (rgb shr 16 and 0xFF) / 255f
        val g = (rgb shr 8 and 0xFF) / 255f
        val b = (rgb and 0xFF) / 255f
        val d2 = time * 0.025 * -1.5
        val d4 = 0.5 + cos(d2 + 2.356194490192345) * 0.2
        val d5 = 0.5 + sin(d2 + 2.356194490192345) * 0.2
        val d6 = 0.5 + cos(d2 + Math.PI / 4.0) * 0.2
        val d7 = 0.5 + sin(d2 + Math.PI / 4.0) * 0.2
        val d8 = 0.5 + cos(d2 + 3.9269908169872414) * 0.2
        val d9 = 0.5 + sin(d2 + 3.9269908169872414) * 0.2
        val d10 = 0.5 + cos(d2 + 5.497787143782138) * 0.2
        val d11 = 0.5 + sin(d2 + 5.497787143782138) * 0.2
        val d14 = -1.0 + d1
        val d15 = height.toDouble() * 2.5 + d14
        worldrenderer.begin(7, VertexFormats.POSITION_TEXTURE_COLOR)
        worldrenderer.vertex(x + d4, y + topOffset, z + d5).texture(1.0, d15).color(r, g, b, 1.0f * alphaMultiplier)
            .next()
        worldrenderer.vertex(x + d4, y + bottomOffset, z + d5).texture(1.0, d14).color(r, g, b, 1.0f).next()
        worldrenderer.vertex(x + d6, y + bottomOffset, z + d7).texture(0.0, d14).color(r, g, b, 1.0f).next()
        worldrenderer.vertex(x + d6, y + topOffset, z + d7).texture(0.0, d15).color(r, g, b, 1.0f * alphaMultiplier)
            .next()
        worldrenderer.vertex(x + d10, y + topOffset, z + d11).texture(1.0, d15).color(r, g, b, 1.0f * alphaMultiplier)
            .next()
        worldrenderer.vertex(x + d10, y + bottomOffset, z + d11).texture(1.0, d14).color(r, g, b, 1.0f).next()
        worldrenderer.vertex(x + d8, y + bottomOffset, z + d9).texture(0.0, d14).color(r, g, b, 1.0f).next()
        worldrenderer.vertex(x + d8, y + topOffset, z + d9).texture(0.0, d15).color(r, g, b, 1.0f * alphaMultiplier)
            .next()
        worldrenderer.vertex(x + d6, y + topOffset, z + d7).texture(1.0, d15).color(r, g, b, 1.0f * alphaMultiplier)
            .next()
        worldrenderer.vertex(x + d6, y + bottomOffset, z + d7).texture(1.0, d14).color(r, g, b, 1.0f).next()
        worldrenderer.vertex(x + d10, y + bottomOffset, z + d11).texture(0.0, d14).color(r, g, b, 1.0f).next()
        worldrenderer.vertex(x + d10, y + topOffset, z + d11).texture(0.0, d15).color(r, g, b, 1.0f * alphaMultiplier)
            .next()
        worldrenderer.vertex(x + d8, y + topOffset, z + d9).texture(1.0, d15).color(r, g, b, 1.0f * alphaMultiplier)
            .next()
        worldrenderer.vertex(x + d8, y + bottomOffset, z + d9).texture(1.0, d14).color(r, g, b, 1.0f).next()
        worldrenderer.vertex(x + d4, y + bottomOffset, z + d5).texture(0.0, d14).color(r, g, b, 1.0f).next()
        worldrenderer.vertex(x + d4, y + topOffset, z + d5).texture(0.0, d15).color(r, g, b, 1.0f * alphaMultiplier)
            .next()
        tessellator.draw()
        RenderSystem.disableCull()
        val d12 = -1.0 + d1
        val d13 = height + d12
        worldrenderer.begin(7, VertexFormats.POSITION_TEXTURE_COLOR)
        worldrenderer.vertex(x + 0.2, y + topOffset, z + 0.2).texture(1.0, d13).color(r, g, b, 0.25f * alphaMultiplier)
            .next()
        worldrenderer.vertex(x + 0.2, y + bottomOffset, z + 0.2).texture(1.0, d12).color(r, g, b, 0.25f).next()
        worldrenderer.vertex(x + 0.8, y + bottomOffset, z + 0.2).texture(0.0, d12).color(r, g, b, 0.25f).next()
        worldrenderer.vertex(x + 0.8, y + topOffset, z + 0.2).texture(0.0, d13).color(r, g, b, 0.25f * alphaMultiplier)
            .next()
        worldrenderer.vertex(x + 0.8, y + topOffset, z + 0.8).texture(1.0, d13).color(r, g, b, 0.25f * alphaMultiplier)
            .next()
        worldrenderer.vertex(x + 0.8, y + bottomOffset, z + 0.8).texture(1.0, d12).color(r, g, b, 0.25f).next()
        worldrenderer.vertex(x + 0.2, y + bottomOffset, z + 0.8).texture(0.0, d12).color(r, g, b, 0.25f).next()
        worldrenderer.vertex(x + 0.2, y + topOffset, z + 0.8).texture(0.0, d13).color(r, g, b, 0.25f * alphaMultiplier)
            .next()
        worldrenderer.vertex(x + 0.8, y + topOffset, z + 0.2).texture(1.0, d13).color(r, g, b, 0.25f * alphaMultiplier)
            .next()
        worldrenderer.vertex(x + 0.8, y + bottomOffset, z + 0.2).texture(1.0, d12).color(r, g, b, 0.25f).next()
        worldrenderer.vertex(x + 0.8, y + bottomOffset, z + 0.8).texture(0.0, d12).color(r, g, b, 0.25f).next()
        worldrenderer.vertex(x + 0.8, y + topOffset, z + 0.8).texture(0.0, d13).color(r, g, b, 0.25f * alphaMultiplier)
            .next()
        worldrenderer.vertex(x + 0.2, y + topOffset, z + 0.8).texture(1.0, d13).color(r, g, b, 0.25f * alphaMultiplier)
            .next()
        worldrenderer.vertex(x + 0.2, y + bottomOffset, z + 0.8).texture(1.0, d12).color(r, g, b, 0.25f).next()
        worldrenderer.vertex(x + 0.2, y + bottomOffset, z + 0.2).texture(0.0, d12).color(r, g, b, 0.25f).next()
        worldrenderer.vertex(x + 0.2, y + topOffset, z + 0.2).texture(0.0, d13).color(r, g, b, 0.25f * alphaMultiplier)
            .next()
        tessellator.draw()
    }

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

    /**
     * @author Mojang
     */
    @JvmStatic
    fun drawOutlinedBoundingBox(aabb: Box?, color: Color, width: Float, partialTicks: Float) {
        val render = mc.cameraEntity
        val realX = interpolate(render.x, render.lastRenderX, partialTicks)
        val realY = interpolate(render.y, render.lastRenderY, partialTicks)
        val realZ = interpolate(render.z, render.lastRenderZ, partialTicks)
        RenderSystem.pushMatrix()
        RenderSystem.method_4412(-realX, -realY, -realZ)
        RenderSystem.method_4407()
        RenderSystem.enableBlend()
        RenderSystem.method_4406()
        RenderSystem.method_4441()
        RenderSystem.blendFuncSeparate(770, 771, 1, 0)
        GL11.glLineWidth(width)
        WorldRenderer.method_3262(aabb, color.red, color.green, color.blue, color.alpha)
        RenderSystem.method_4412(realX, realY, realZ)
        RenderSystem.disableBlend()
        RenderSystem.method_4456()
        RenderSystem.method_4397()
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
        RenderSystem.popMatrix()
    }

    /**
     * Taken from Skyblockcatia under MIT License
     * Modified
     * https://github.com/SteveKunG/SkyBlockcatia/blob/1.8.9/LICENSE.md
     *
     * @author SteveKunG
     */
    @JvmStatic
    fun renderItem(itemStack: ItemStack?, x: Int, y: Int) {
        DiffuseLighting.enableForItems()
        RenderSystem.enableDepthTest()
        mc.itemRenderer.method_4026(itemStack, x, y)
    }

    /**
     * Taken from Skyblockcatia under MIT License
     * Modified
     * https://github.com/SteveKunG/SkyBlockcatia/blob/1.8.9/LICENSE.md
     *
     * @author SteveKunG
     */
    @JvmStatic
    fun renderTexture(
        texture: Identifier?,
        x: Int,
        y: Int,
        width: Int = 16,
        height: Int = 16,
        enableLighting: Boolean = true
    ) {
        if (enableLighting) DiffuseLighting.enableForItems()
        RenderSystem.method_4434()
        RenderSystem.enableBlend()
        RenderSystem.enableDepthTest()
        RenderSystem.blendFuncSeparate(770, 771, 1, 0)
        RenderSystem.pushMatrix()
        mc.textureManager.bindTextureInner(texture)
        RenderSystem.method_4434()
        RenderSystem.method_4456()
        RenderSystem.method_4377(516, 0.1f)
        RenderSystem.enableBlend()
        RenderSystem.blendFunc(770, 771)
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
        DrawContext.method_1781(x, y, 0f, 0f, width, height, width.toFloat(), height.toFloat())
        RenderSystem.method_4441()
        RenderSystem.method_4351()
        RenderSystem.method_4406()
        RenderSystem.popMatrix()
    }

    fun draw3DLine(
        pos1: Vec3d,
        pos2: Vec3d,
        width: Int,
        color: Color,
        partialTicks: Float,
        matrixStack: UMatrixStack,
        alphaMultiplier: Float = 1f
    ) {
        val render = mc.cameraEntity
        val worldRenderer = UGraphics.getFromTessellator()
        val realX = interpolate(render.x, render.lastRenderX, partialTicks)
        val realY = interpolate(render.y, render.lastRenderY, partialTicks)
        val realZ = interpolate(render.z, render.lastRenderZ, partialTicks)
        matrixStack.push()
        matrixStack.translate(-realX, -realY, -realZ)
        UGraphics.enableBlend()
        UGraphics.enableAlpha()
        UGraphics.disableLighting()
        UGraphics.tryBlendFuncSeparate(770, 771, 1, 0)
        GL11.glLineWidth(width.toFloat())
        UGraphics.color4f(
            color.red / 255f,
            color.green / 255f,
            color.blue / 255f,
            (color.alpha * alphaMultiplier / 255f).coerceAtMost(1f)
        )
        worldRenderer.beginWithActiveShader(UGraphics.DrawMode.LINE_STRIP, VertexFormats.POSITION)
        worldRenderer.pos(matrixStack, pos1.x, pos1.y, pos1.z).endVertex()
        worldRenderer.pos(matrixStack, pos2.x, pos2.y, pos2.z).endVertex()
        worldRenderer.drawDirect()
        matrixStack.pop()
        UGraphics.disableBlend()
        UGraphics.enableAlpha()
        UGraphics.color4f(1f, 1f, 1f, 1f)
    }

    fun draw3DLineStrip(
        points: Iterable<Vec3d>,
        width: Int,
        color: Color,
        partialTicks: Float,
        matrixStack: UMatrixStack,
        alphaMultiplier: Float = 1f
    ) {
        val render = mc.cameraEntity
        val worldRenderer = UGraphics.getFromTessellator()
        val realX = interpolate(render.x, render.lastRenderX, partialTicks)
        val realY = interpolate(render.y, render.lastRenderY, partialTicks)
        val realZ = interpolate(render.z, render.lastRenderZ, partialTicks)
        matrixStack.push()
        matrixStack.translate(-realX, -realY, -realZ)
        UGraphics.enableBlend()
        UGraphics.enableAlpha()
        UGraphics.disableLighting()
        UGraphics.tryBlendFuncSeparate(770, 771, 1, 0)
        GL11.glLineWidth(width.toFloat())
        UGraphics.color4f(
            color.red / 255f,
            color.green / 255f,
            color.blue / 255f,
            (color.alpha * alphaMultiplier / 255f).coerceAtMost(1f)
        )
        worldRenderer.beginWithActiveShader(UGraphics.DrawMode.LINE_STRIP, VertexFormats.POSITION)
        points.forEach {
            worldRenderer.pos(matrixStack, it.x, it.y, it.z).endVertex()
        }
        worldRenderer.drawDirect()
        matrixStack.pop()
        UGraphics.disableBlend()
        UGraphics.enableAlpha()
        UGraphics.color4f(1f, 1f, 1f, 1f)
    }

    fun drawLabel(
        pos: Vec3d,
        text: String,
        color: Color,
        partialTicks: Float,
        matrixStack: UMatrixStack,
        shadow: Boolean = false,
        scale: Float = 1f
    ) = drawNametag(pos.x, pos.y, pos.z, text, color, partialTicks, matrixStack, shadow, scale, false)

    fun renderWaypointText(str: String, loc: BlockPos, partialTicks: Float, matrixStack: UMatrixStack) =
        renderWaypointText(
            str,
            loc.x.toDouble(),
            loc.y.toDouble(),
            loc.z.toDouble(),
            partialTicks,
            matrixStack
        )

    fun renderWaypointText(
        str: String,
        x: Double,
        y: Double,
        z: Double,
        partialTicks: Float,
        matrixStack: UMatrixStack
    ) {
        matrixStack.push()
        RenderSystem.method_4377(516, 0.1f)
        val (viewerX, viewerY, viewerZ) = getViewerPos(partialTicks)
        val distX = x - viewerX
        val distY = y - viewerY - mc.cameraEntity.standingEyeHeight
        val distZ = z - viewerZ
        val dist = sqrt(distX * distX + distY * distY + distZ * distZ)
        val renderX: Double
        val renderY: Double
        val renderZ: Double
        if (dist > 12) {
            renderX = distX * 12 / dist + viewerX
            renderY = distY * 12 / dist + viewerY + mc.cameraEntity.standingEyeHeight
            renderZ = distZ * 12 / dist + viewerZ
        } else {
            renderX = x
            renderY = y
            renderZ = z
        }
        drawNametag(renderX, renderY, renderZ, str, Color.WHITE, partialTicks, matrixStack)
        matrixStack.rotate(-mc.entityRenderDispatcher.cameraYaw, 0.0f, 1.0f, 0.0f)
        matrixStack.rotate(mc.entityRenderDispatcher.cameraPitch, 1.0f, 0.0f, 0.0f)
        matrixStack.translate(0.0, -0.25, 0.0)
        matrixStack.rotate(-mc.entityRenderDispatcher.cameraPitch, 1.0f, 0.0f, 0.0f)
        matrixStack.rotate(mc.entityRenderDispatcher.cameraYaw, 0.0f, 1.0f, 0.0f)
        drawNametag(
            renderX,
            renderY,
            renderZ,
            "${ChatColor.YELLOW}${dist.roundToInt()}m",
            Color.WHITE,
            partialTicks,
            matrixStack
        )
        matrixStack.pop()
    }

    private fun drawNametag(
        x: Double, y: Double, z: Double,
        str: String, color: Color,
        partialTicks: Float, matrixStack: UMatrixStack,
        shadow: Boolean = true, scale: Float = 1f, background: Boolean = true
    ) {
        val player = mc.player
        val x1 = x - player.lastRenderX + (x - player.x - (x - player.lastRenderX)) * partialTicks
        val y1 = y - player.lastRenderY + (y - player.y - (y - player.lastRenderY)) * partialTicks
        val z1 = z - player.lastRenderZ + (z - player.z - (z - player.lastRenderZ)) * partialTicks
        val f1 = 0.0266666688
        val width = mc.textRenderer.getWidth(str) / 2
        matrixStack.push()
        matrixStack.translate(x1, y1, z1)
        GL11.glNormal3f(0f, 1f, 0f)
        matrixStack.rotate(-mc.entityRenderDispatcher.cameraYaw, 0.0f, 1.0f, 0.0f)
        matrixStack.rotate(mc.entityRenderDispatcher.cameraPitch, 1.0f, 0.0f, 0.0f)
        matrixStack.scale(-f1, -f1, -f1)
        UGraphics.disableLighting()
        UGraphics.depthMask(false)
        UGraphics.enableBlend()
        UGraphics.tryBlendFuncSeparate(770, 771, 1, 0)
        if (background) {
            val worldRenderer = UGraphics.getFromTessellator()
            worldRenderer.beginWithDefaultShader(UGraphics.DrawMode.QUADS, VertexFormats.POSITION_COLOR)
            worldRenderer.pos(matrixStack, (-width - 1.0), -1.0, 0.0).color(0f, 0f, 0f, 0.25f).endVertex()
            worldRenderer.pos(matrixStack, (-width - 1.0), 8.0, 0.0).color(0f, 0f, 0f, 0.25f).endVertex()
            worldRenderer.pos(matrixStack, width + 1.0, 8.0, 0.0).color(0f, 0f, 0f, 0.25f).endVertex()
            worldRenderer.pos(matrixStack, width + 1.0, -1.0, 0.0).color(0f, 0f, 0f, 0.25f).endVertex()
            worldRenderer.drawDirect()
        }
        RenderSystem.method_4397()
        DefaultFonts.VANILLA_FONT_RENDERER.drawString(
            matrixStack,
            str,
            color,
            -width.toFloat(),
            ElementaFonts.MINECRAFT.getBelowLineHeight() * scale,
            width * 2f,
            scale,
            shadow
        )
        UGraphics.depthMask(true)
        matrixStack.pop()
    }

    @JvmStatic
    fun renderRarity(itemStack: ItemStack?, xPos: Int, yPos: Int) {
        if (itemStack != null) {
            if (!Skytils.config.showPetRarity && ItemUtil.isPet(itemStack)) {
                return
            } else {
                if (!mutex.isLocked) {
                    mutex.lock()
                    renderRarity(xPos, yPos, itemStack)
                    mutex.unlock()
                }
            }
        }
    }

    /**
     * Taken from Skyblockcatia under MIT License
     * https://github.com/SteveKunG/SkyBlockcatia/blob/1.8.9/LICENSE.md
     * @author SteveKunG
     */
    @JvmStatic
    fun renderRarity(xPos: Int, yPos: Int, rarity: ItemRarity) {
        val alpha = Skytils.config.itemRarityOpacity

        // save the states
        val lightingEnabled = GL11.glIsEnabled(GL11.GL_LIGHTING)
        val depthEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST)
        val alphaEnabled = GL11.glIsEnabled(GL11.GL_ALPHA_TEST)

        if (lightingEnabled) RenderSystem.method_4406()
        if (depthEnabled) RenderSystem.disableDepthTest()
        RenderSystem.pushMatrix()
        RenderSystem.enableBlend()
        if (!alphaEnabled) RenderSystem.method_4456()
        mc.textureManager.bindTextureInner(
            when (Skytils.config.itemRarityShape) {
                0 -> RARITY
                1 -> RARITY2
                2 -> RARITY3
                3 -> RARITY4
                4 -> CUSTOMRARITY
                else -> RARITY
            }
        )
        RenderSystem.setShaderColor(
            rarity.color.red / 255.0f,
            rarity.color.green / 255.0f,
            rarity.color.blue / 255.0f,
            alpha
        )
        RenderSystem.popMatrix()
        RenderSystem.blendFunc(770, 771)
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_BLEND)
        DrawContext.method_1781(xPos, yPos, 0f, 0f, 16, 16, 16f, 16f)
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE)
        if (lightingEnabled) RenderSystem.method_4394()
        if (depthEnabled) RenderSystem.enableDepthTest()
        if (!alphaEnabled) RenderSystem.method_4441()
    }

    private fun renderRarity(xPos: Int, yPos: Int, itemStack: ItemStack?) {
        if (itemStack == null) return
        val rarity = ItemUtil.getRarity(itemStack)
        if (rarity != ItemRarity.NONE) {
            val alpha = Skytils.config.itemRarityOpacity

            if (Skytils.config.itemRarityShape < 5) {
                renderRarity(xPos, yPos, rarity)
            } else {
                RenderSystem.pushMatrix()
                // save the states
                val lightingEnabled = GL11.glIsEnabled(GL11.GL_LIGHTING)
                val depthEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST)
                val alphaEnabled = GL11.glIsEnabled(GL11.GL_ALPHA_TEST)

                if (lightingEnabled) RenderSystem.method_4406()
                if (depthEnabled) RenderSystem.disableDepthTest()
                RenderSystem.enableBlend()
                if (!alphaEnabled) RenderSystem.method_4456()

                GL11.glEnable(GL11.GL_STENCIL_TEST) // Turn on da test
                val scissorState = GL11.glGetInteger(GL11.GL_SCISSOR_TEST) // check if scissor test was on
                GL11.glStencilMask(0xFF)
                GL11.glDisable(GL11.GL_SCISSOR_TEST)
                GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT) // Flush old data
                if (scissorState == GL11.GL_TRUE) GL11.glEnable(GL11.GL_SCISSOR_TEST)

                GL11.glStencilMask(0xFF) // Writing = ON
                GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF) // Always "add" to frame
                GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE) // Replace on success
                GL11.glColorMask(false, false, false, false)
                //Anything rendered here becomes "cut" frame.

                val scale = 1.2
                RenderSystem.method_4412(
                    xPos.toDouble(),
                    yPos.toDouble(),
                    0.0
                )

                RenderSystem.pushMatrix()
                RenderSystem.method_4412(8.0, 8.0, 0.0)
                RenderSystem.method_4453(scale, scale, 0.0)
                RenderSystem.method_4412(-8.0, -8.0, 0.0)
                skipGlint = true
                renderItem(itemStack, 0, 0)
                skipGlint = false
                RenderSystem.popMatrix()

                GL11.glColorMask(true, true, true, true)

                GL11.glStencilMask(0x00) // Writing = OFF
                GL11.glStencilFunc(
                    GL11.GL_NOTEQUAL,
                    0,
                    0xFF
                ) // Anything that wasn't defined above will not be rendered.
                //Anything rendered here will be cut if goes beyond frame defined before.
                DrawContext.fill(
                    1, 1, 17, 17,
                    Color(
                        (rarity.color.red).coerceAtLeast(0) / 255f,
                        (rarity.color.green).coerceAtLeast(0) / 255f,
                        (rarity.color.blue).coerceAtLeast(0) / 255f,
                        alpha
                    ).rgb
                )
                GL11.glDisable(GL11.GL_STENCIL_TEST)

                if (lightingEnabled) RenderSystem.method_4394()
                if (depthEnabled) RenderSystem.enableDepthTest()
                if (!alphaEnabled) RenderSystem.method_4441()
                RenderSystem.popMatrix()
            }

        }
    }

    /**
     * Taken from SkyblockAddons under MIT License
     * Modified
     * https://github.com/BiscuitDevelopment/SkyblockAddons/blob/master/LICENSE
     * @author BiscuitDevelopment
     *
     * Draws a solid color rectangle with the specified coordinates and color (ARGB format). Args: x1, y1, x2, y2, color
     */
    fun drawRect(left: Double, top: Double, right: Double, bottom: Double, color: Int) {
        var leftModifiable = left
        var topModifiable = top
        var rightModifiable = right
        var bottomModifiable = bottom
        if (leftModifiable < rightModifiable) {
            val i = leftModifiable
            leftModifiable = rightModifiable
            rightModifiable = i
        }
        if (topModifiable < bottomModifiable) {
            val j = topModifiable
            topModifiable = bottomModifiable
            bottomModifiable = j
        }
        val f3 = (color shr 24 and 255).toFloat() / 255.0f
        val f = (color shr 16 and 255).toFloat() / 255.0f
        val f1 = (color shr 8 and 255).toFloat() / 255.0f
        val f2 = (color and 255).toFloat() / 255.0f
        RenderSystem.setShaderColor(f, f1, f2, f3)
        val tessellator = Tessellator.getInstance()
        val worldRenderer = tessellator.buffer
        RenderSystem.enableBlend()
        RenderSystem.method_4407()
        RenderSystem.blendFuncSeparate(770, 771, 1, 0)
        worldRenderer.begin(7, VertexFormats.POSITION)
        worldRenderer.vertex(leftModifiable, bottomModifiable, 0.0).next()
        worldRenderer.vertex(rightModifiable, bottomModifiable, 0.0).next()
        worldRenderer.vertex(rightModifiable, topModifiable, 0.0).next()
        worldRenderer.vertex(leftModifiable, topModifiable, 0.0).next()
        tessellator.draw()
        RenderSystem.method_4397()
        RenderSystem.disableBlend()
    }

    /**
     * Taken from SkyblockAddons under MIT License
     * https://github.com/BiscuitDevelopment/SkyblockAddons/blob/master/LICENSE
     * @author BiscuitDevelopment
     *
     */
    fun drawCylinderInWorld(x: Double, y: Double, z: Double, radius: Float, height: Float, partialTicks: Float) {
        var x1 = x
        var y1 = y
        var z1 = z
        val renderViewEntity = mc.cameraEntity
        val viewX =
            renderViewEntity.prevX + (renderViewEntity.x - renderViewEntity.prevX) * partialTicks.toDouble()
        val viewY =
            renderViewEntity.prevY + (renderViewEntity.y - renderViewEntity.prevY) * partialTicks.toDouble()
        val viewZ =
            renderViewEntity.prevZ + (renderViewEntity.z - renderViewEntity.prevZ) * partialTicks.toDouble()
        x1 -= viewX
        y1 -= viewY
        z1 -= viewZ
        val tessellator = Tessellator.getInstance()
        val worldrenderer = tessellator.buffer
        worldrenderer.begin(GL11.GL_QUAD_STRIP, VertexFormats.POSITION)
        var currentAngle = 0f
        val angleStep = 0.1f
        while (currentAngle < 2 * Math.PI) {
            val xOffset = radius * cos(currentAngle.toDouble()).toFloat()
            val zOffset = radius * sin(currentAngle.toDouble()).toFloat()
            worldrenderer.vertex(x1 + xOffset, y1 + height, z1 + zOffset).next()
            worldrenderer.vertex(x1 + xOffset, y1 + 0, z1 + zOffset).next()
            currentAngle += angleStep
        }
        worldrenderer.vertex(x1 + radius, y1 + height, z1).next()
        worldrenderer.vertex(x1 + radius, y1 + 0.0, z1).next()
        tessellator.draw()
    }

    // totally not modified Autumn Client's TargetStrafe
    fun drawCircle(entity: Entity, partialTicks: Float, rad: Double, color: Color) {
        var il = 0.0
        val tessellator = Tessellator.getInstance()
        val worldrenderer = tessellator.buffer
        while (il < 0.05) {
            RenderSystem.pushMatrix()
            RenderSystem.method_4407()
            GL11.glLineWidth(2F)
            worldrenderer.begin(1, VertexFormats.POSITION)
            val x: Double =
                entity.lastRenderX + (entity.x - entity.lastRenderX) * partialTicks - mc.entityRenderDispatcher.field_4695
            val y: Double =
                entity.lastRenderY + (entity.y - entity.lastRenderY) * partialTicks - mc.entityRenderDispatcher.field_4694
            val z: Double =
                entity.lastRenderZ + (entity.z - entity.lastRenderZ) * partialTicks - mc.entityRenderDispatcher.field_4693
            val pix2 = Math.PI * 2.0
            for (i in 0..90) {
                color.bindColor()
                worldrenderer.vertex(x + rad * cos(i * pix2 / 45.0), y + il, z + rad * sin(i * pix2 / 45.0)).next()
            }
            tessellator.draw()
            RenderSystem.method_4397()
            RenderSystem.popMatrix()
            il += 0.0006
        }
    }

    fun drawCircle(matrixStack: UMatrixStack, x: Double, y: Double, z: Double, partialTicks: Float, radius: Double, edges: Int, r: Int, g: Int, b: Int, a: Int = 255) {
        val ug = UGraphics.getFromTessellator()
        val angleDelta = Math.PI * 2 / edges
        GL11.glLineWidth(5f)
        ug.beginWithDefaultShader(UGraphics.DrawMode.LINE_STRIP, UGraphics.CommonVertexFormats.POSITION_COLOR)
        repeat(edges) { idx ->
            ug.pos(matrixStack, x - mc.entityRenderDispatcher.field_4695 + radius * cos(idx * angleDelta), y - mc.entityRenderDispatcher.field_4694, z - mc.entityRenderDispatcher.field_4693 + radius * sin(idx * angleDelta)).color(r, g, b, a).endVertex()
        }
        ug.pos(matrixStack, x + radius - mc.entityRenderDispatcher.field_4695, y - mc.entityRenderDispatcher.field_4694, z - mc.entityRenderDispatcher.field_4693).color(r, g, b, a).endVertex()
        ug.drawDirect()
    }

    fun getViewerPos(partialTicks: Float): Triple<Double, Double, Double> {
        val viewer = mc.cameraEntity
        val viewerX = viewer.lastRenderX + (viewer.x - viewer.lastRenderX) * partialTicks
        val viewerY = viewer.lastRenderY + (viewer.y - viewer.lastRenderY) * partialTicks
        val viewerZ = viewer.lastRenderZ + (viewer.z - viewer.lastRenderZ) * partialTicks
        return Triple(viewerX, viewerY, viewerZ)
    }

    fun getPartialTicks() =
        (mc as AccessorMinecraft).timer.tickDelta

    /**
     * Helper method for fixRenderPos
     */
    fun getRenderX() : Double {
        return (mc.entityRenderDispatcher as AccessorRenderManager).renderX
    }

    /**
     * Helper method for fixRenderPos
     */
    fun getRenderY() : Double {
        return (mc.entityRenderDispatcher as AccessorRenderManager).renderY
    }

    /**
     * Helper method for fixRenderPos
     */
    fun getRenderZ() : Double {
        return (mc.entityRenderDispatcher as AccessorRenderManager).renderZ
    }

    /**
     * Method used to Gather event location parameters and return their interpolated counterparts.
     *
     * Working particularly well in RenderLivingEvent.Pre/Post<*>
     */
    fun fixRenderPos(x: Double, y: Double, z: Double, invert: Boolean = false) : Triple<Double, Double, Double> {
        return Triple(x + getRenderX(), y + getRenderY(), z + getRenderZ())
    }

    infix fun Slot.highlight(color: Color) {
        DrawContext.fill(
            this.x,
            this.y,
            this.x + 16,
            this.y + 16,
            color.rgb
        )
    }

    infix fun Slot.highlight(color: Int) {
        DrawContext.fill(
            this.x,
            this.y,
            this.x + 16,
            this.y + 16,
            color
        )
    }

    fun drawDurabilityBar(xPos: Int, yPos: Int, durability: Double) {
        val j = (13.0 - durability * 13.0).roundToInt()
        val i = (255.0 - durability * 255.0).roundToInt()
        RenderSystem.method_4406()
        RenderSystem.disableDepthTest()
        RenderSystem.method_4407()
        RenderSystem.method_4441()
        RenderSystem.disableBlend()
        drawRect(xPos + 2, yPos + 13, 13, 2, 0, 0, 0, 255)
        drawRect(xPos + 2, yPos + 13, 12, 1, (255 - i) / 4, 64, 0, 255)
        drawRect(xPos + 2, yPos + 13, j, 1, 255 - i, i, 0, 255)
        //GlStateManager.enableBlend()
        RenderSystem.method_4456()
        RenderSystem.method_4397()
        RenderSystem.method_4394()
        RenderSystem.enableDepthTest()
    }

    fun drawRect(x: Number, y: Number, width: Number, height: Number, red: Int, green: Int, blue: Int, alpha: Int) {
        val tesselator = Tessellator.getInstance()
        val renderer = tesselator.buffer
        renderer.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR)
        renderer.vertex(x.toDouble(), y.toDouble(), 0.0).color(red, green, blue, alpha).next()
        renderer.vertex(x.toDouble(), y.toDouble() + height.toDouble(), 0.0).color(red, green, blue, alpha).next()
        renderer.vertex(x.toDouble() + width.toDouble(), y.toDouble() + height.toDouble(), 0.0)
            .color(red, green, blue, alpha).next()
        renderer.vertex(x.toDouble() + width.toDouble(), y.toDouble(), 0.0).color(red, green, blue, alpha).next()
        tesselator.draw()
    }

    // see GuiIngame
    private val vignetteTexPath = Identifier("textures/misc/vignette.png")

    /**
     * @author Mojang (modified)
     * @see net.minecraft.client.gui.GuiIngame.renderVignette
     */
    fun drawVignette(color: Color) {
        mc.gameRenderer.method_0_3375()
        UGraphics.enableBlend()
        UGraphics.disableDepth()

        UGraphics.depthMask(false)
        UGraphics.tryBlendFuncSeparate(0, 769, 1, 0)

        // Changing the alpha doesn't affect the vignette, so we have to use the alpha to change the color values
        UGraphics.color4f(
            (1f - (color.red / 255f)) * (color.alpha / 255f),
            (1f - (color.green / 255f)) * (color.alpha / 255f),
            (1f - (color.blue / 255f)) * (color.alpha / 255f),
            1f
        )

        val sr = UResolution

        UGraphics.bindTexture(0, vignetteTexPath)
        val tessellator = UGraphics.getTessellator()
        val wr = UGraphics.getFromTessellator()
        val matrixStack = UMatrixStack()
        wr.beginWithDefaultShader(UGraphics.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE)
        wr.pos(matrixStack, 0.0, sr.scaledHeight.toDouble(), -90.0).tex(0.0, 1.0).endVertex()
        wr.pos(matrixStack, sr.scaledWidth.toDouble(), sr.scaledHeight.toDouble(), -90.0).tex(1.0, 1.0).endVertex()
        wr.pos(matrixStack, sr.scaledWidth.toDouble(), 0.0, -90.0).tex(1.0, 0.0).endVertex()
        wr.pos(matrixStack, 0.0, 0.0, -90.0).tex(0.0, 0.0).endVertex()
        tessellator.draw()

        UGraphics.depthMask(true)
        UGraphics.enableDepth()
        UGraphics.color4f(1.0f, 1.0f, 1.0f, 1.0f)
        UGraphics.tryBlendFuncSeparate(770, 771, 1, 0)
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

    fun drawAllInList(element: GuiElement, lines: Collection<String>) {
        val leftAlign = element.scaleX < UResolution.scaledWidth / 2f
        val alignment =
            if (leftAlign) SmartFontRenderer.TextAlignment.LEFT_RIGHT else SmartFontRenderer.TextAlignment.RIGHT_LEFT
        val xPos = if (leftAlign) 0f else element.width.toFloat()
        for ((i, str) in lines.withIndex()) {
            ScreenRenderer.fontRenderer.drawString(
                str,
                xPos,
                (i * ScreenRenderer.fontRenderer.field_0_2811).toFloat(),
                CommonColors.WHITE,
                alignment,
                element.textShadow
            )
        }
    }

    fun drawSelectionBox(
        pos: BlockPos,
        block: Block = mc.world.getBlockState(pos).block,
        color: Color,
        partialTicks: Float
    ) {
        val (viewerX, viewerY, viewerZ) = getViewerPos(partialTicks)
        val matrixStack = UMatrixStack()
        RenderSystem.disableCull()
        RenderSystem.disableDepthTest()
        RenderSystem.enableBlend()
        RenderSystem.blendFuncSeparate(770, 771, 1, 0)
        block.method_9529(mc.world, pos)
        drawFilledBoundingBox(
            matrixStack,
            block.method_0_686(mc.world, pos)
                .expandBlock()
                .offset(-viewerX, -viewerY, -viewerZ),
            color
        )
        RenderSystem.disableBlend()
        RenderSystem.enableCull()
        RenderSystem.enableDepthTest()
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