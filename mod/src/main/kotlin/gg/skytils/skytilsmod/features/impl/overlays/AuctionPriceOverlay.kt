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
package gg.skytils.skytilsmod.features.impl.overlays

import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.input.UITextInput
import gg.essential.elementa.layoutdsl.*
import gg.essential.elementa.state.v2.MutableState
import gg.essential.elementa.state.v2.memo
import gg.essential.elementa.state.v2.mutableStateOf
import gg.essential.elementa.state.v2.State
import gg.essential.elementa.state.v2.onChange
import gg.essential.universal.UKeyboard
import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.screen.GuiContainerSlotClickEvent
import gg.skytils.event.impl.screen.ScreenKeyInputEvent
import gg.skytils.event.impl.screen.ScreenOpenEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.features.impl.handlers.AuctionData
import gg.skytils.skytilsmod.features.impl.misc.ContainerSellValue
import gg.skytils.skytilsmod.gui.layout.button
import gg.skytils.skytilsmod.gui.layout.mcTextInput
import gg.skytils.skytilsmod.gui.layout.text
import gg.skytils.skytilsmod.mixins.transformers.accessors.AccessorGuiContainer
import gg.skytils.skytilsmod.mixins.transformers.accessors.AccessorGuiEditSign
import gg.skytils.skytilsmod.utils.NumberUtil
import gg.skytils.skytilsmod.utils.NumberUtil.roundToPrecision
import gg.skytils.skytilsmod.utils.SBInfo
import gg.skytils.skytilsmod.utils.Utils
import gg.skytils.skytilsmod.utils.getSlot
import gg.skytils.skytilsmod.utils.multiplatform.SlotActionType
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.client.gui.screen.ingame.SignEditScreen
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket
import net.minecraft.util.math.BlockPos
import kotlin.text.lowercase
import kotlin.text.toDouble

//#if MC<12000
//$$ import net.minecraft.text.LiteralTextContent
//#endif

object AuctionPriceOverlay : EventSubscriber {
    private val undercutState = mutableStateOf(false)
    private val isUndercutState = memo {
        if (!undercutState() || lastAuctionedStackState() == null) return@memo false
        AuctionData.getIdentifier(lastAuctionedStackState())?.let(AuctionData.lowestBINs::containsKey) == true
    }
    private val lastAuctionedStackState: MutableState<ItemStack?> = mutableStateOf(null)
    private val lbinPriceState = memo {
        lastAuctionedStackState()?.let(AuctionData::getIdentifier)?.let(AuctionData.lowestBINs::get)
    }
    private val valueState: State<String> = memo {
        val count = lastAuctionedStackState()?.count ?: 0
        lbinPriceState()?.let { valuePer ->
            "Clean Lowest BIN Price: §b${NumberUtil.nf.format((valuePer * count).toInt())}" +
                    if (count > 1) " §7(${NumberUtil.nf.format(valuePer.roundToPrecision(2))} each§7)" else ""
        } ?: ""
    }
    private val estimatedValueState: State<String> = memo {
        lastAuctionedStackState()?.let(ContainerSellValue::getItemValue)?.let {estimatedValue ->
            "Estimated Value: §b${NumberUtil.nf.format(estimatedValue)}"
        } ?: ""
    }
    // Outside of screen to allow for persistent values
    val inputState: MutableState<String> = mutableStateOf("")

    fun onGuiOpen(event: ScreenOpenEvent) {
        if (!Utils.inSkyblock || !Skytils.config.betterAuctionPriceInput) return
        if (event.screen is SignEditScreen && Utils.equalsOneOf(
                SBInfo.lastOpenContainerName,
                "Create Auction",
                "Create BIN Auction"
            )
        ) {
            val sign =
                (event.screen as AccessorGuiEditSign).tileSign
            //#if MC<12000
            //$$ val signText = sign.texts
            //#else
            val signText = sign.frontText.getMessages(false)
            //#endif
            if (
                sign != null && sign.pos.y == 0 &&
                signText[1].string == "^^^^^^^^^^^^^^^" &&
                signText[2].string == "Your auction" &&
                signText[3].string == "starting bid"
            ) {
                event.screen = ElementaAuctionPriceScreen(sign.pos, signText.map { it.string }.toTypedArray())
            }
        }
    }

    fun onGuiKey(event: ScreenKeyInputEvent) {
        if (!Utils.inSkyblock || !Skytils.config.betterAuctionPriceInput) return
        if (event.screen is GenericContainerScreen && event.keyCode == UKeyboard.KEY_ENTER) {
            if (Utils.equalsOneOf(
                    SBInfo.lastOpenContainerName,
                    "Create Auction",
                    "Create BIN Auction"
                )
            ) {
                (event.screen as AccessorGuiContainer).invokeHandleMouseClick(
                    (event.screen as GenericContainerScreen).getSlot(
                        29
                    ), 29, 2, SlotActionType.CLONE
                )
                event.cancelled = true
            } else if (Utils.equalsOneOf(
                    SBInfo.lastOpenContainerName,
                    "Confirm Auction",
                    "Confirm BIN Auction"
                )
            ) {
                (event.screen as AccessorGuiContainer).invokeHandleMouseClick(
                    (event.screen as GenericContainerScreen).getSlot(
                        11
                    ), 11, 2, SlotActionType.CLONE
                )
                event.cancelled = true
            }
        }
    }

    fun onSlotClick(event: GuiContainerSlotClickEvent) {
        if (!Utils.inSkyblock || !Skytils.config.betterAuctionPriceInput) return
        if (event.gui is GenericContainerScreen) {
            if (Utils.equalsOneOf(
                    SBInfo.lastOpenContainerName,
                    "Create Auction",
                    "Create BIN Auction"
                ) && event.slotId == 31
            ) {
                event.container.getSlot(13).stack?.let { auctionItem ->
                    //#if MC<12000
                    //$$ if (auctionItem.name == "§a§l§nAUCTION FOR ITEM:") {
                    //#else
                    if (auctionItem.name.string == "§a§l§nAUCTION FOR ITEM:") {
                    //#endif
                        lastAuctionedStackState.set(auctionItem)
                    }
                }
            }
            if (event.slotId == 11 && Utils.equalsOneOf(
                    SBInfo.lastOpenContainerName,
                    "Confirm Auction",
                    "Confirm BIN Auction"
                )
            ) {
                lastAuctionedStackState.set(null)
            }
        }
    }

    class ElementaAuctionPriceScreen(private val pos: BlockPos, private val text: Array<String>) : WindowScreen(ElementaVersion.V5) {
        companion object {
            private val REGEX = Regex("[^0-9.kmb]")
        }
        private val validatedInput: State<String?> = State {
            val input = inputState()
            if (isUndercutState()) {
                val lbin = lbinPriceState() ?: return@State null
                val count = lastAuctionedStackState()?.count ?: 0
                var num = input.toDoubleOrNull() ?:  if (isProperCompactNumber(input)) {
                    getActualValueFromCompactNumber(input) ?: return@State null
                } else return@State null
                val actualValue = (lbin - num) * count
                if (actualValue < 0) return@State null
                val stringified = actualValue.toLong().toString()
                return@State if (stringified.length > 15) NumberUtil.format(actualValue.toLong()) else stringified
            }
            return@State input.lowercase()
        }
        private val listingTextState: State<String> = memo {
            "§6Listing for: ${validatedInput() ?: "§cInvalid Value"}"
        }
        init {
            val input: UITextInput
            window.layout {
                row(Modifier.fillParent()) {
                    column(Modifier.fillHeight()) {
                        text(valueState)
                        text(estimatedValueState)
                        text(listingTextState)
                        spacer(0f, 5f)
                        input = mcTextInput(inputState, modifier = Modifier.width(260f).height(10f))
                        input.filter { text ->
                            text.lowercase().replace(REGEX, "")
                        }
                        input.onKeyType { char, keyCode ->
                            if (keyCode == UKeyboard.KEY_ENTER || keyCode == UKeyboard.KEY_ESCAPE) {
                                client?.setScreen(null)
                                return@onKeyType
                            }
                        }
                        inputState.onChange(window) {
                            text[0] = validatedInput.getUntracked() ?: "Invalid Value"
                        }
                        spacer(0f, 5f)
                        button(memo { if (isUndercutState()) "Mode: Undercut" else "Mode: Normal" }) {
                            undercutState.set { !it }
                        }
                    }
                }
            }
            window.onKeyType { char, keyCode ->
                input.setActive(true)
                input.keyType(char, keyCode)
            }

        }

        override fun onScreenClose() {
            super.onScreenClose()
            //#if MC<12000
            //$$ client.networkHandler?.sendPacket(UpdateSignC2SPacket(pos, text.map { LiteralTextContent(it) }.toTypedArray()))
            //#else
            client?.networkHandler?.sendPacket(UpdateSignC2SPacket(pos, true, text[0], text[1], text[2], text[3]))
            //#endif
        }
    }

    /**
     * This code was modified and taken under CC BY-SA 3.0 license
     * @link https://stackoverflow.com/a/44630965
     * @author Sachin Rao
     */
    private fun getActualValueFromCompactNumber(value: String): Double? {
        val lastAlphabet = value.replace("[^a-zA-Z]*$".toRegex(), "")
            .replace(".(?!$)".toRegex(), "")
        var multiplier = 1L
        when (lastAlphabet.lowercase()) {
            "k" -> multiplier = 1000L
            "m" -> multiplier = 1000000L
            "b" -> multiplier = 1000000000L
            "t" -> multiplier = 1000000000000L
            else -> {
            }
        }
        val values = value.split(lastAlphabet.toRegex()).toTypedArray()
        return if (multiplier == 1L) {
            null
        } else {
            val valueMultiplier: Double = try {
                values[0].toDouble()
            } catch (ex: ArrayIndexOutOfBoundsException) {
                0.0
            } catch (ex: NumberFormatException) {
                0.0
            }
            val valueAdder: Double = try {
                values[1].toDouble()
            } catch (ex: ArrayIndexOutOfBoundsException) {
                0.0
            } catch (ex: NumberFormatException) {
                0.0
            }
            valueMultiplier * multiplier + valueAdder
        }
    }

    /**
     * This code was modified and taken under CC BY-SA 3.0 license
     * @link https://stackoverflow.com/a/44630965
     * @author Sachin Rao
     */
    private fun isProperCompactNumber(value: String): Boolean {
        val count = value.replace("\\s+".toRegex(), "").replace("[.0-9]+".toRegex(), "")
        return count.length < 2
    }

    override fun setup() {
        register(::onGuiOpen)
        register(::onGuiKey)
        register(::onSlotClick)
    }
}