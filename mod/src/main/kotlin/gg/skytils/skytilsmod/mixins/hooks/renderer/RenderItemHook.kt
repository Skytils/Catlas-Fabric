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
package gg.skytils.skytilsmod.mixins.hooks.renderer

import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod.mixins.transformers.accessors.AccessorRenderItem
import gg.skytils.skytilsmod.utils.ItemUtil.getSkyBlockItemID
import gg.skytils.skytilsmod.utils.NEUCompatibility.isCustomAHActive
import gg.skytils.skytilsmod.utils.NEUCompatibility.isStorageMenuActive
import gg.skytils.skytilsmod.utils.NEUCompatibility.isTradeWindowActive
import gg.skytils.skytilsmod.utils.RenderUtil.renderRarity
import gg.skytils.skytilsmod.utils.Utils
import net.minecraft.client.MinecraftClient
import net.minecraft.client.TextRenderer
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.texture.SpriteAtlasTexture
import net.minecraft.client.render.model.BakedModel
import net.minecraft.item.Items
import net.minecraft.item.ItemStack
import net.minecraft.util.Identifier
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

val RES_ITEM_GLINT = Identifier("textures/misc/enchanted_item_glint.png")

var skipGlint = false

fun renderRarity(stack: ItemStack?, x: Int, y: Int, ci: CallbackInfo) {
    if (Utils.inSkyblock && Skytils.config.showItemRarity) {
        if (mc.currentScreen != null) {
            if (isStorageMenuActive || isTradeWindowActive || isCustomAHActive) {
                renderRarity(stack, x, y)
            }
        }
    }
}

fun renderItemPre(stack: ItemStack, model: BakedModel, ci: CallbackInfo) {
    if (!Utils.inSkyblock) return
    if (stack.item === Items.PLAYER_HEAD) {
        val scale = Skytils.config.largerHeadScale.toDouble()
        RenderSystem.method_4453(scale, scale, scale)
    }
}

fun modifyGlintRendering(stack: ItemStack, model: BakedModel, ci: CallbackInfo) {
    if (Utils.inSkyblock) {
        val itemId = getSkyBlockItemID(stack)
        GlintCustomizer.glintItems[itemId]?.color?.let {
            val color = it.toInt()
            RenderSystem.depthMask(false)
            RenderSystem.depthFunc(514)
            RenderSystem.method_4406()
            RenderSystem.blendFunc(768, 1)
            mc.textureManager.bindTextureInner(RES_ITEM_GLINT)
            RenderSystem.method_4440(5890)
            RenderSystem.pushMatrix()
            RenderSystem.method_4384(8.0f, 8.0f, 8.0f)
            val f = (MinecraftClient.method_0_2227() % 3000L).toFloat() / 3000.0f / 8.0f
            RenderSystem.method_4348(f, 0.0f, 0.0f)
            RenderSystem.method_4445(-50.0f, 0.0f, 0.0f, 1.0f)
            (mc.itemRenderer as AccessorRenderItem).invokeRenderModel(
                model,
                color
            )
            RenderSystem.popMatrix()
            RenderSystem.pushMatrix()
            RenderSystem.method_4384(8.0f, 8.0f, 8.0f)
            val f1 = (MinecraftClient.method_0_2227() % 4873L).toFloat() / 4873.0f / 8.0f
            RenderSystem.method_4348(-f1, 0.0f, 0.0f)
            RenderSystem.method_4445(10.0f, 0.0f, 0.0f, 1.0f)
            (mc.itemRenderer as AccessorRenderItem).invokeRenderModel(
                model,
                color
            )
            RenderSystem.popMatrix()
            RenderSystem.method_4440(5888)
            RenderSystem.blendFunc(770, 771)
            RenderSystem.method_4394()
            RenderSystem.depthFunc(515)
            RenderSystem.depthMask(true)
            mc.textureManager.bindTextureInner(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
            ci.cancel()

            //Since we prematurely exited, we need to reset the matrices
            RenderSystem.popMatrix()
        }
    }
}