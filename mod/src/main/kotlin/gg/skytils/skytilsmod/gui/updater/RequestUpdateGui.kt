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

package gg.skytils.skytilsmod.gui.updater

import dev.dediamondpro.minemark.elementa.MineMarkComponent
import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.components.UIText
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.RelativeConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.basicHeightConstraint
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.pixels
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.core.UpdateChecker
import gg.skytils.skytilsmod.gui.components.SimpleButton
import net.minecraft.client.gui.screen.TitleScreen

class RequestUpdateGui : WindowScreen(ElementaVersion.V2, newGuiScale = 2) {

    init {
        val updateObj = UpdateChecker.updateGetter.updateObj ?: error("Update object is null")
        UIText("Skytils ${updateObj.tagName} is available!")
            .constrain {
                x = CenterConstraint()
                y = RelativeConstraint(0.1f)
            } childOf window
        UIText("You are currently on version ${Skytils.VERSION}.")
            .constrain {
                x = CenterConstraint()
                y = SiblingConstraint()
            } childOf window
        val authorText =
            UIText("Uploaded by: ${UpdateChecker.updateAsset.uploader.username}")
                .constrain {
                    x = CenterConstraint()
                    y = SiblingConstraint()
                } childOf window
        val changelogWrapper = ScrollComponent()
            .constrain {
                x = CenterConstraint()
                y = SiblingConstraint(10f)
                height = basicHeightConstraint { window.getHeight() - 90 - authorText.getBottom() }
                width = RelativeConstraint(0.7f)
            } childOf window
        MineMarkComponent(updateObj.body)
            .constrain {
                height = RelativeConstraint()
                width = RelativeConstraint()
            }
            .childOf(changelogWrapper)
        SimpleButton("Update and Restart")
            .constrain {
                x = CenterConstraint()
                y = SiblingConstraint(5f)
                width = 150.pixels()
                height = 20.pixels()
            }.onMouseClick {
                Skytils.displayScreen = UpdateGui(true)
            } childOf window
        SimpleButton("Update after Restart")
            .constrain {
                x = CenterConstraint()
                y = SiblingConstraint(5f)
                width = 150.pixels()
                height = 20.pixels()
            }.onMouseClick {
                Skytils.displayScreen = UpdateGui(false)
            } childOf window
        SimpleButton("Main Menu")
            .constrain {
                x = CenterConstraint()
                y = SiblingConstraint(5f)
                width = 150.pixels()
                height = 20.pixels()
            }.onMouseClick {
                UpdateChecker.updateGetter.updateObj = null
                Skytils.displayScreen = TitleScreen()
            } childOf window
    }
}