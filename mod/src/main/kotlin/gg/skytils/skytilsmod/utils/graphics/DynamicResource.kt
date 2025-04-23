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

package gg.skytils.skytilsmod.utils.graphics

import gg.skytils.skytilsmod.Skytils.mc
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.util.Identifier
import java.awt.image.BufferedImage

/**
 * Utility to create resources using a buffered image
 */
class DynamicResource(val name: String, val image: BufferedImage) : NativeImageBackedTexture(image) {
    val resource: Identifier by lazy {
        mc.textureManager.registerDynamicTexture(name, this)
    }
}