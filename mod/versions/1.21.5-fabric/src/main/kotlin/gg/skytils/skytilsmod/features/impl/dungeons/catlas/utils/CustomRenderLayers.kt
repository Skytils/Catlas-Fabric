/*
 * Skytils - Hypixel Skyblock Quality of Life Mod
 * Copyright (C) 2020-2025 Skytils
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

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gl.RenderPipelines.POSITION_COLOR_SNIPPET
import net.minecraft.client.gl.RenderPipelines.RENDERTYPE_LINES_SNIPPET
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.RenderPhase
import net.minecraft.client.render.VertexFormats
import net.minecraft.util.Identifier
import java.util.*

object CustomRenderLayers {
    val espLinesPipeline = RenderPipelines.register(RenderPipeline.builder(RENDERTYPE_LINES_SNIPPET)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withLocation(Identifier.of("catlas", "pipeline/lines"))
        .build()
    )

    val espLines = RenderLayer.of(
        "catlas:lines",
        1536,
        espLinesPipeline,
        RenderLayer.MultiPhaseParameters.builder()
            .lineWidth(RenderPhase.LineWidth(OptionalDouble.empty()))
            .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
            .target(RenderPhase.ITEM_ENTITY_TARGET)
            .build(false)
    )

    val espBoxPipeline = RenderPipelines.register(
        RenderPipeline.builder(POSITION_COLOR_SNIPPET)
            .withLocation(Identifier.of("catlas", "pipeline/debug_filled_box"))
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLE_STRIP)
            .build()
    )

    val espFilledBoxLayer = RenderLayer.of(
        "catlas:debug_filled_box",
        1536,
        false,
        true,
        espBoxPipeline,
        RenderLayer.MultiPhaseParameters.builder()
            .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
            .build(false)
    )

}