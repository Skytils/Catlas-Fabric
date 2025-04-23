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

import gg.skytils.skytilsmod.features.impl.handlers.GlintCustomizer
import gg.skytils.skytilsmod.utils.ItemUtil.getSkyBlockItemID
import gg.skytils.skytilsmod.utils.Utils
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.render.entity.LivingEntityRenderer
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer
import net.minecraft.entity.LivingEntity
import net.minecraft.util.Identifier
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

private val enchantedItemGlintResource = Identifier("textures/misc/enchanted_item_glint.png")


fun replaceArmorGlint(
    layerArmorBase: Any,
    rendererLivingEntity: LivingEntityRenderer<*>,
    entitylivingbaseIn: LivingEntity,
    p_177182_2_: Float,
    p_177182_3_: Float,
    partialTicks: Float,
    p_177182_5_: Float,
    p_177182_6_: Float,
    p_177182_7_: Float,
    scale: Float,
    armorSlot: Int,
    ci: CallbackInfo
) {
    (layerArmorBase as ArmorFeatureRenderer<*>).apply {
        if (Utils.inSkyblock) {
            val itemstack = entitylivingbaseIn.method_0_7157(armorSlot - 1)
            val itemId = getSkyBlockItemID(itemstack)
            GlintCustomizer.glintItems[itemId]?.color?.let { color ->
                ci.cancel()
                val f = entitylivingbaseIn.age.toFloat() + partialTicks
                rendererLivingEntity.bindTexture(enchantedItemGlintResource)
                RenderSystem.enableBlend()
                RenderSystem.depthFunc(514)
                RenderSystem.depthMask(false)
                val f1 = 0.5f
                RenderSystem.setShaderColor(f1, f1, f1, 1.0f)
                //GlintCustomizer.glintColors.get(itemId).applyColor();
                for (i in 0..1) {
                    RenderSystem.method_4406()
                    RenderSystem.blendFunc(768, 1)
                    val f2 = 0.76f
                    //GlStateManager.color(0.5F * f2, 0.25F * f2, 0.8F * f2, 1.0F);
                    color.applyColor()
                    RenderSystem.method_4440(5890)
                    RenderSystem.loadIdentitiy()
                    val f3 = 0.33333334f
                    RenderSystem.method_4384(f3, f3, f3)
                    RenderSystem.method_4445(30.0f - i.toFloat() * 60.0f, 0.0f, 0.0f, 1.0f)
                    RenderSystem.method_4348(0.0f, f * (0.001f + i.toFloat() * 0.003f) * 20.0f, 0.0f)
                    RenderSystem.method_4440(5888)
                    layerArmorBase.getModel(armorSlot)!!.setAngles(
                        entitylivingbaseIn,
                        p_177182_2_,
                        p_177182_3_,
                        p_177182_5_,
                        p_177182_6_,
                        p_177182_7_,
                        scale
                    )
                }
                RenderSystem.method_4440(5890)
                RenderSystem.loadIdentitiy()
                RenderSystem.method_4440(5888)
                RenderSystem.method_4394()
                RenderSystem.depthMask(true)
                RenderSystem.depthFunc(515)
                RenderSystem.disableBlend()
            }
        }
    }
}