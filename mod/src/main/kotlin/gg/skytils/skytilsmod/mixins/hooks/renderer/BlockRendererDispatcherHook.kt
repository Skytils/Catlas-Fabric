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
import gg.skytils.skytilsmod.features.impl.farming.GardenFeatures
import gg.skytils.skytilsmod.utils.SBInfo
import gg.skytils.skytilsmod.utils.SkyblockIsland
import gg.skytils.skytilsmod.utils.Utils
import net.minecraft.block.CarpetBlock
import net.minecraft.class_0_308
import net.minecraft.block.BlockState
import net.minecraft.client.render.block.BlockRenderManager
import net.minecraft.client.render.model.BakedModel
import net.minecraft.block.Blocks
import net.minecraft.util.DyeColor
import net.minecraft.util.math.BlockPos
import net.minecraft.world.WorldView
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

fun modifyGetModelFromBlockState(
    blockRendererDispatcher: BlockRenderManager,
    state: BlockState?,
    worldIn: WorldView,
    pos: BlockPos?,
    cir: CallbackInfoReturnable<BakedModel>
) {
    if (!Utils.inSkyblock || state == null || pos == null) return
    var returnState = state
    if (SBInfo.mode == SkyblockIsland.DwarvenMines.mode) {
        if (Skytils.config.recolorCarpets && state.block === Blocks.CARPET && Utils.equalsOneOf(
                state.testProperty(
                    CarpetBlock.field_0_1324
                ), DyeColor.GRAY, DyeColor.LIGHT_BLUE, DyeColor.YELLOW
            )
        ) {
            returnState = state.method_0_1222(CarpetBlock.field_0_1324, DyeColor.RED)
        } else if (Skytils.config.darkModeMist && pos.y <= 76) {
            if (state.block === Blocks.field_0_761 &&
                state.testProperty(class_0_308.field_0_1192) == DyeColor.WHITE
            ) {
                returnState = state.method_0_1222(class_0_308.field_0_1192, DyeColor.GRAY)
            } else if (state.block === Blocks.CARPET && state.testProperty(CarpetBlock.field_0_1324) == DyeColor.WHITE) {
                returnState = state.method_0_1222(CarpetBlock.field_0_1324, DyeColor.GRAY)
            }
        }
    } else if (Skytils.config.gardenPlotCleanupHelper && GardenFeatures.isCleaningPlot && GardenFeatures.trashBlocks.contains(
            state.block
        )
    ) {
        returnState = Blocks.SPONGE.defaultState
    }

    if (returnState !== state) {
        cir.returnValue = blockRendererDispatcher.models.getModel(returnState)
    }
}