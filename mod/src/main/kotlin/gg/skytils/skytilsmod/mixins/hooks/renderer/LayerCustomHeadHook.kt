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
import gg.skytils.skytilsmod.utils.Utils
import net.minecraft.client.network.ClientPlayerEntity
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.class_1009
import net.minecraft.entity.LivingEntity
import org.lwjgl.opengl.GL11
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

fun renderCustomHeadLayer(
    entity: LivingEntity,
    p_177141_2_: Float,
    p_177141_3_: Float,
    partialTicks: Float,
    p_177141_5_: Float,
    p_177141_6_: Float,
    p_177141_7_: Float,
    scale: Float,
    ci: CallbackInfo
) {
    if (!Utils.inSkyblock) return
    if (entity is ClientPlayerEntity) {
        if (Skytils.config.transparentHeadLayer == 0f) {
            RenderSystem.popMatrix()
            ci.cancel()
            return
        }
        if (Skytils.config.transparentHeadLayer != 1f) {
            if (entity.hurtTime > 0) {
                // See net.minecraft.client.renderer.entity.RendererLivingEntity.unsetBrightness
                RenderSystem.activeTexture(class_1009.field_4977)
                RenderSystem.method_4397()
                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, class_1009.field_4962)
                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, class_1009.field_4974, GL11.GL_MODULATE)
                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, class_1009.field_4931, class_1009.field_4977)
                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, class_1009.field_4941, class_1009.field_4932)
                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, class_1009.field_4970, GL11.GL_SRC_COLOR)
                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, class_1009.field_4929, GL11.GL_SRC_COLOR)
                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, class_1009.field_4956, GL11.GL_MODULATE)
                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, class_1009.field_4967, class_1009.field_4977)
                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, class_1009.field_4926, class_1009.field_4932)
                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, class_1009.field_4953, GL11.GL_SRC_ALPHA)
                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, class_1009.field_4965, GL11.GL_SRC_ALPHA)
                RenderSystem.activeTexture(class_1009.field_4933)
                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, class_1009.field_4962)
                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, class_1009.field_4974, GL11.GL_MODULATE)
                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, class_1009.field_4970, GL11.GL_SRC_COLOR)
                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, class_1009.field_4929, GL11.GL_SRC_COLOR)
                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, class_1009.field_4931, GL11.GL_TEXTURE)
                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, class_1009.field_4941, class_1009.field_4961)
                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, class_1009.field_4956, GL11.GL_MODULATE)
                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, class_1009.field_4953, GL11.GL_SRC_ALPHA)
                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, class_1009.field_4967, GL11.GL_TEXTURE)
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
                RenderSystem.activeTexture(class_1009.field_4946)
                RenderSystem.method_4407()
                RenderSystem.bindTexture(0)
                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, class_1009.field_4962)
                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, class_1009.field_4974, GL11.GL_MODULATE)
                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, class_1009.field_4970, GL11.GL_SRC_COLOR)
                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, class_1009.field_4929, GL11.GL_SRC_COLOR)
                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, class_1009.field_4931, GL11.GL_TEXTURE)
                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, class_1009.field_4941, class_1009.field_4961)
                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, class_1009.field_4956, GL11.GL_MODULATE)
                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, class_1009.field_4953, GL11.GL_SRC_ALPHA)
                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, class_1009.field_4967, GL11.GL_TEXTURE)
                RenderSystem.activeTexture(class_1009.field_4977)
            }
            RenderSystem.method_4456()
            RenderSystem.enableBlend()
            RenderSystem.blendFuncSeparate(770, 771, 1, 0)
            RenderSystem.setShaderColor(1f, 1f, 1f, Skytils.config.transparentHeadLayer)
        }
    }
}

fun renderCustomHeadLayerPost(
    entity: LivingEntity,
    p_177141_2_: Float,
    p_177141_3_: Float,
    partialTicks: Float,
    p_177141_5_: Float,
    p_177141_6_: Float,
    p_177141_7_: Float,
    scale: Float,
    ci: CallbackInfo
) {
    RenderSystem.disableBlend()
}

fun renderGlintOnSkull(
    entitylivingbaseIn: LivingEntity,
    p_177141_2_: Float,
    p_177141_3_: Float,
    partialTicks: Float,
    p_177141_5_: Float,
    p_177141_6_: Float,
    p_177141_7_: Float,
    scale: Float,
    ci: CallbackInfo
) {
    val itemStack = entitylivingbaseIn.method_0_7157(3)
    if (Utils.inSkyblock && Skytils.config.enchantGlintFix && itemStack.hasGlint()) {
        Utils.lastRenderedSkullStack = itemStack
        Utils.lastRenderedSkullEntity = entitylivingbaseIn
    }
}
