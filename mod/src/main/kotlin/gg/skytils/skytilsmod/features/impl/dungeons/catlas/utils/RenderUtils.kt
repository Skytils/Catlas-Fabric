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

package gg.skytils.skytilsmod.features.impl.dungeons.catlas.utils

import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod.features.impl.dungeons.catlas.core.CatlasConfig
import gg.skytils.skytilsmod.features.impl.dungeons.catlas.core.CatlasElement
import gg.skytils.skytilsmod.features.impl.dungeons.catlas.core.DungeonMapPlayer
import gg.skytils.skytilsmod.features.impl.dungeons.catlas.handlers.DungeonScanner
import gg.skytils.skytilsmod.utils.*
import net.minecraft.client.gui.DrawContext
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.BufferBuilder
import net.minecraft.client.render.VertexFormats
import net.minecraft.util.Identifier
import org.lwjgl.opengl.GL11.GL_QUADS
import java.awt.Color

object RenderUtils {

    private val tessellator: Tessellator = Tessellator.getInstance()
    private val worldRenderer: BufferBuilder = tessellator.buffer
    private val mapIcons = Identifier("catlas:marker.png")

    private fun preDraw() {
        RenderSystem.method_4456()
        RenderSystem.enableBlend()
        RenderSystem.disableDepthTest()
        RenderSystem.method_4406()
        RenderSystem.method_4407()
        RenderSystem.blendFuncSeparate(770, 771, 1, 0)
    }

    private fun postDraw() {
        RenderSystem.disableBlend()
        RenderSystem.enableDepthTest()
        RenderSystem.method_4397()
    }

    private fun addQuadVertices(x: Double, y: Double, w: Double, h: Double) {
        worldRenderer.vertex(x, y + h, 0.0).next()
        worldRenderer.vertex(x + w, y + h, 0.0).next()
        worldRenderer.vertex(x + w, y, 0.0).next()
        worldRenderer.vertex(x, y, 0.0).next()
    }

    fun drawTexturedQuad(x: Double, y: Double, width: Double, height: Double) {
        worldRenderer.begin(GL_QUADS, VertexFormats.POSITION_TEXTURE)
        worldRenderer.vertex(x, y + height, 0.0).texture(0.0, 1.0).next()
        worldRenderer.vertex(x + width, y + height, 0.0).texture(1.0, 1.0).next()
        worldRenderer.vertex(x + width, y, 0.0).texture(1.0, 0.0).next()
        worldRenderer.vertex(x, y, 0.0).texture(0.0, 0.0).next()
        tessellator.draw()
    }

    fun renderRect(x: Double, y: Double, w: Double, h: Double, color: Color) {
        if (color.alpha == 0) return
        preDraw()
        color.bindColor()

        worldRenderer.begin(GL_QUADS, VertexFormats.POSITION)
        addQuadVertices(x, y, w, h)
        tessellator.draw()

        postDraw()
    }

    fun renderRectBorder(x: Double, y: Double, w: Double, h: Double, thickness: Double, color: Color) {
        if (color.alpha == 0) return
        preDraw()
        color.bindColor()

        worldRenderer.begin(GL_QUADS, VertexFormats.POSITION)
        addQuadVertices(x - thickness, y, thickness, h)
        addQuadVertices(x - thickness, y - thickness, w + thickness * 2, thickness)
        addQuadVertices(x + w, y, thickness, h)
        addQuadVertices(x - thickness, y + h, w + thickness * 2, thickness)
        tessellator.draw()

        postDraw()
    }

    fun renderCenteredText(text: List<String>, x: Int, y: Int, color: Int) {
        if (text.isEmpty()) return
        RenderSystem.pushMatrix()
        RenderSystem.method_4348(x.toFloat(), y.toFloat(), 0f)
        RenderSystem.method_4384(CatlasConfig.textScale, CatlasConfig.textScale, 1f)

        if (CatlasConfig.mapRotate) {
            RenderSystem.method_4445(mc.player.yaw + 180f, 0f, 0f, 1f)
        } else if (CatlasConfig.mapDynamicRotate) {
            RenderSystem.method_4445(-CatlasElement.dynamicRotation, 0f, 0f, 1f)
        }

        val fontHeight = mc.textRenderer.field_0_2811 + 1
        val yTextOffset = text.size * fontHeight / -2f

        text.withIndex().forEach { (index, text) ->
            mc.textRenderer.method_0_2383(
                text,
                mc.textRenderer.getWidth(text) / -2f,
                yTextOffset + index * fontHeight,
                color,
                true
            )
        }

        if (CatlasConfig.mapDynamicRotate) {
            RenderSystem.method_4445(CatlasElement.dynamicRotation, 0f, 0f, 1f)
        }

        RenderSystem.popMatrix()
    }

    fun drawPlayerHead(name: String, player: DungeonMapPlayer) {
        RenderSystem.pushMatrix()
        try {
            // Translates to the player's location which is updated every tick.
            if (player.isOurMarker || name == mc.player.name) {
                RenderSystem.method_4412(
                    (mc.player.x - DungeonScanner.startX + 15) * MapUtils.coordMultiplier + MapUtils.startCorner.first,
                    (mc.player.z - DungeonScanner.startZ + 15) * MapUtils.coordMultiplier + MapUtils.startCorner.second,
                    0.0
                )
            } else {
                player.teammate.player?.also { entityPlayer ->
                    // If the player is loaded in our view, use that location instead (more precise)
                    RenderSystem.method_4412(
                        (entityPlayer.x - DungeonScanner.startX + 15) * MapUtils.coordMultiplier + MapUtils.startCorner.first,
                        (entityPlayer.z - DungeonScanner.startZ + 15) * MapUtils.coordMultiplier + MapUtils.startCorner.second,
                        0.0
                    )
                }.ifNull {
                    RenderSystem.method_4348(player.mapX.toFloat(), player.mapZ.toFloat(), 0f)
                }
            }

            // Apply head rotation and scaling
            RenderSystem.method_4445(player.yaw + 180f, 0f, 0f, 1f)
            RenderSystem.method_4384(CatlasConfig.playerHeadScale, CatlasConfig.playerHeadScale, 1f)

            if (CatlasConfig.mapVanillaMarker && (player.isOurMarker || name == mc.player.name)) {
                RenderSystem.method_4445(180f, 0f, 0f, 1f)
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
                mc.textureManager.bindTextureInner(mapIcons)
                worldRenderer.begin(7, VertexFormats.POSITION_TEXTURE)
                worldRenderer.vertex(-6.0, 6.0, 0.0).texture(0.0, 0.0).next()
                worldRenderer.vertex(6.0, 6.0, 0.0).texture(1.0, 0.0).next()
                worldRenderer.vertex(6.0, -6.0, 0.0).texture(1.0, 1.0).next()
                worldRenderer.vertex(-6.0, -6.0, 0.0).texture(0.0, 1.0).next()
                tessellator.draw()
                RenderSystem.method_4445(-180f, 0f, 0f, 1f)
            } else {
                // Render box behind the player head
                val borderColor = when (player.teammate.dungeonClass) {
                    DungeonClass.ARCHER -> CatlasConfig.colorPlayerArcher
                    DungeonClass.BERSERK -> CatlasConfig.colorPlayerBerserk
                    DungeonClass.HEALER -> CatlasConfig.colorPlayerHealer
                    DungeonClass.MAGE -> CatlasConfig.colorPlayerMage
                    DungeonClass.TANK -> CatlasConfig.colorPlayerTank
                    else -> Color.BLACK
                }

                renderRect(-6.0, -6.0, 12.0, 12.0, borderColor)
                RenderSystem.method_4348(0f, 0f, 0.1f)

                preDraw()
                RenderSystem.method_4397()
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f)

                mc.textureManager.bindTextureInner(player.skin)

                RenderSystem.pushMatrix()
                val scale = 1f - CatlasConfig.playerBorderPercentage
                RenderSystem.method_4384(scale, scale, scale)
                DrawContext.method_1786(-6, -6, 8f, 8f, 8, 8, 12, 12, 64f, 64f)
                if (player.renderHat) {
                    DrawContext.method_1786(-6, -6, 40f, 8f, 8, 8, 12, 12, 64f, 64f)
                }
                RenderSystem.popMatrix()

                postDraw()
            }

            // Handle player names
            if (CatlasConfig.playerHeads == 2 || CatlasConfig.playerHeads == 1 && Utils.equalsOneOf(
                    ItemUtil.getSkyBlockItemID(mc.player.method_0_7087()),
                    "SPIRIT_LEAP", "INFINITE_SPIRIT_LEAP", "HAUNT_ABILITY"
                )
            ) {
                if (!CatlasConfig.mapRotate) {
                    RenderSystem.method_4445(-player.yaw + 180f, 0f, 0f, 1f)
                }
                RenderSystem.method_4348(0f, 10f, 0f)
                RenderSystem.method_4384(CatlasConfig.playerNameScale, CatlasConfig.playerNameScale, 1f)
                mc.textRenderer.method_0_2383(
                    name, -mc.textRenderer.getWidth(name) / 2f, 0f, 0xffffff, true
                )
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        RenderSystem.popMatrix()
    }
}
