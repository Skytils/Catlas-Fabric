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

package gg.skytils.skytilsmod.features.impl.handlers

import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.play.WorldUnloadEvent
import gg.skytils.event.impl.screen.GuiContainerForegroundDrawnEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.core.PersistentSave
import gg.skytils.skytilsmod.listeners.DungeonListener
import gg.skytils.skytilsmod.utils.DungeonClass
import gg.skytils.skytilsmod.utils.RenderUtil.highlight
import gg.skytils.skytilsmod.utils.Utils
import gg.skytils.skytilsmod.utils.graphics.ScreenRenderer
import gg.skytils.skytilsmod.utils.graphics.SmartFontRenderer
import gg.skytils.skytilsmod.utils.stripControlCodes
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import net.minecraft.client.gui.DrawContext
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.item.Items
import net.minecraft.screen.GenericContainerScreenHandler
import java.io.File
import java.io.Reader
import java.io.Writer
import java.util.*

object SpiritLeap : PersistentSave(File(Skytils.modDir, "spiritleap.json")), EventSubscriber {

    private val playerPattern = Regex("(?:\\[.+?] )?(?<name>\\w+)")
    var doorOpener: String? = null
    val names = HashMap<String, Boolean>()
    val classes = DungeonClass.entries
        .associateWithTo(EnumMap(DungeonClass::class.java)) { false }
    private val shortenedNameCache = WeakHashMap<String, String>()
    private val nameSlotCache = HashMap<Int, String>()

    override fun setup() {
        register(::onGuiDrawPost)
        register(::onWorldLoad)
    }

    fun onGuiDrawPost(event: GuiContainerForegroundDrawnEvent) {
        if (!Utils.inDungeons) return
        if (event.container is GenericContainerScreenHandler) {
            if ((Skytils.config.spiritLeapNames && event.chestName == "Spirit Leap") || (Skytils.config.reviveStoneNames && event.chestName == "Revive A Teammate") || (Skytils.config.ghostTeleportMenuNames && event.chestName == "Teleport to Player")) {
                val fr = ScreenRenderer.fontRenderer
                var people = 0
                RenderSystem.method_4406()
                RenderSystem.enableBlend()
                for (slot in event.container.slots) {
                    if (slot.inventory == mc.player.inventory) continue
                    if (!slot.hasStack() || slot.stack.item != Items.PLAYER_HEAD) continue
                    val item = slot.stack
                    people++

                    val x = slot.x.toFloat()
                    val y = slot.y + if (people % 2 != 0) -15f else 20f
                    val name = nameSlotCache[slot.id]
                    if (name == null || name == "Unknown") {
                        nameSlotCache[slot.id] =
                            playerPattern.find(item.name.stripControlCodes())?.groups?.get("name")?.value
                                ?: continue
                        continue
                    }
                    val teammate = DungeonListener.team[name] ?: continue
                    val dungeonClass = teammate.dungeonClass
                    val text = shortenedNameCache.getOrPut(name) {
                        fr.method_0_2384(item.name.substring(0, 2) + name, 32)
                    }
                    val scale = 0.9f
                    val scaleReset = 1 / scale
                    RenderSystem.pushMatrix()
                    if (Skytils.config.highlightDoorOpener && name == doorOpener) {
                        slot highlight 1174394112
                    } else if (names.getOrDefault(name, false)) {
                        slot highlight 1174339584
                    } else if (classes.getOrDefault(dungeonClass, false)) {
                        slot highlight 1157693184
                    }
                    RenderSystem.method_4348(0f, 0f, 299f)
                    DrawContext.fill(
                        (x - 2 - fr.getWidth(text) / 2).toInt(),
                        (y - 2).toInt(),
                        (x + fr.getWidth(text) / 2 + 2).toInt(),
                        (y + fr.field_0_2811 + 2).toInt(),
                        -13686744
                    )
                    fr.drawString(
                        text,
                        x,
                        y,
                        alignment = SmartFontRenderer.TextAlignment.MIDDLE,
                        shadow = SmartFontRenderer.TextShadow.OUTLINE
                    )
                    RenderSystem.method_4384(scale, scale, 1f)
                    fr.method_0_2383(
                        dungeonClass.className.first().uppercase(),
                        scaleReset * x,
                        scaleReset * slot.y,
                        -256,
                        true
                    )
                    RenderSystem.popMatrix()
                }
                RenderSystem.disableBlend()
            }
        }
    }

    fun onWorldLoad(event: WorldUnloadEvent) {
        nameSlotCache.clear()
    }

    override fun read(reader: Reader) {
        val data = json.decodeFromString<SaveData>(reader.readText())
        names.putAll(data.users.entries.associate { it.key to it.value.enabled })
        data.classes.forEach { (clazz, state) ->
            classes[clazz] = state.enabled
        }
    }

    override fun write(writer: Writer) {
        writer.write(
            json.encodeToString(
                SaveData(
                    names.entries.associate { it.key to SaveComponent(it.value) },
                    classes.entries.associate { it.key to SaveComponent(it.value) })
            )
        )
    }

    override fun setDefault(writer: Writer) {
        write(writer)
    }
}

@Serializable
private data class SaveData(val users: Map<String, SaveComponent>, val classes: Map<DungeonClass, SaveComponent>)

@Serializable
private data class SaveComponent(val enabled: Boolean)