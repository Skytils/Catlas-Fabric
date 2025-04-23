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

import com.mojang.authlib.GameProfile
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod.features.impl.handlers.GlintCustomizer
import gg.skytils.skytilsmod.utils.ItemUtil.getSkyBlockItemID
import gg.skytils.skytilsmod.utils.RenderUtil.getPartialTicks
import gg.skytils.skytilsmod.utils.Utils
import gg.skytils.skytilsmod.utils.graphics.colors.CustomColor
import net.minecraft.client.render.entity.model.EntityModel
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.render.block.entity.SkullBlockEntityRenderer
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.Direction
import net.minecraft.util.Identifier
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

val instance: SkullBlockEntityRenderer = SkullBlockEntityRenderer.INSTANCE
private val enchantedItemGlintResource = Identifier("textures/misc/enchanted_item_glint.png")

fun addGlintToSkull(
    x: Float,
    y: Float,
    z: Float,
    face: Direction,
    rotation: Float,
    type: Int,
    profile: GameProfile?,
    p_180543_8_: Int,
    ci: CallbackInfo,
    model: EntityModel
) {
    if (Utils.lastRenderedSkullStack != null && Utils.lastRenderedSkullEntity != null) {
        val itemId = getSkyBlockItemID(Utils.lastRenderedSkullStack)
        renderGlint(Utils.lastRenderedSkullEntity, model, rotation, GlintCustomizer.glintItems[itemId]?.color)
        Utils.lastRenderedSkullStack = null
        Utils.lastRenderedSkullEntity = null
    }
}

fun renderGlint(entity: LivingEntity?, model: EntityModel?, rotation: Float, color: CustomColor?) {
    val partialTicks = getPartialTicks()
    val f = entity!!.age.toFloat() + partialTicks
    mc.textureManager.bindTextureInner(enchantedItemGlintResource)
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
        if (color == null) RenderSystem.setShaderColor(0.5f * f2, 0.25f * f2, 0.8f * f2, 1.0f) else color.applyColor()
        RenderSystem.method_4440(5890)
        RenderSystem.loadIdentitiy()
        val f3 = 0.33333334f
        RenderSystem.method_4384(f3, f3, f3)
        RenderSystem.method_4445(30.0f - i.toFloat() * 60.0f, 0.0f, 0.0f, 1.0f)
        RenderSystem.method_4348(0.0f, f * (0.001f + i.toFloat() * 0.003f) * 20.0f, 0.0f)
        RenderSystem.method_4440(5888)
        model!!.setAngles(null, 0f, 0f, 0f, rotation, 0f, f)
    }
    RenderSystem.method_4440(5890)
    RenderSystem.loadIdentitiy()
    RenderSystem.method_4440(5888)
    RenderSystem.method_4394()
    RenderSystem.depthMask(true)
    RenderSystem.depthFunc(515)
    //GlStateManager.disableBlend();
    RenderSystem.blendFunc(770, 771)
}