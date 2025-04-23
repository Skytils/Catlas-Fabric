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

import gg.skytils.skytilsmod.core.Config.threeWeirdosSolverColor
import gg.skytils.skytilsmod.features.impl.dungeons.solvers.ThreeWeirdosSolver
import gg.skytils.skytilsmod.utils.bindColor
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.block.entity.ChestBlockEntity
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

fun setChestColor(
    te: ChestBlockEntity,
    x: Double,
    y: Double,
    z: Double,
    partialTicks: Float,
    destroyStage: Int,
    ci: CallbackInfo
) {
    if (te.pos == ThreeWeirdosSolver.riddleChest) {
        threeWeirdosSolverColor.bindColor()
        RenderSystem.method_4407()
    }
}

fun setChestColorPost(
    te: ChestBlockEntity,
    x: Double,
    y: Double,
    z: Double,
    partialTicks: Float,
    destroyStage: Int,
    ci: CallbackInfo
) {
    RenderSystem.method_4397()
}