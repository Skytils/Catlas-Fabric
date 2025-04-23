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

package gg.skytils.skytilsmod.util

import gg.skytils.skytilsmod.utils.stripControlCodes
import net.minecraft.text.CharacterVisitor
import net.minecraft.text.StringVisitable
import net.minecraft.text.Style
import net.minecraft.util.Formatting
import java.util.*

/**
 * This class visits a text instance and can be used to retrieve a version of the text
 * that attempts to use legacy control codes to represent colors and formatting
 */
class ControlCodeVisitor : CharacterVisitor, StringVisitable.StyledVisitor<String> {
    private val builder = StringBuilder()
    private var prevStyle: Style? = null
    override fun accept(index: Int, style: Style, codePoint: Int): Boolean {
        if (style != prevStyle) {
            prevStyle = style
            builder.append(serializeFormattingToString(style))
        }

        builder.append(codePoint.toChar())
        return true
    }

    override fun accept(style: Style, asString: String): Optional<String> {
        if (style != prevStyle) {
            prevStyle = style
            builder.append(serializeFormattingToString(style))
        }

        builder.append(asString)
        return Optional.empty()
    }

    private fun serializeFormattingToString(style: Style): String {
        val builder = StringBuilder("§r")

        when {
            style.isBold -> builder.append("§l")
            style.isItalic -> builder.append("§o")
            style.isUnderlined -> builder.append("§n")
            style.isStrikethrough -> builder.append("§m")
            style.isObfuscated -> builder.append("§k")
        }

        style.color?.name?.let(Formatting::byName)?.let(builder::append)

        return builder.toString()
    }

    fun getFormattedString() = builder.toString()

    // Just use getString() on the text instance lol
    fun getUnformattedString() = builder.toString().stripControlCodes()
}