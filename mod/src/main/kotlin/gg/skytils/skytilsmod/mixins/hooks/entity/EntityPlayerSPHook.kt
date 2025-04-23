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
package gg.skytils.skytilsmod.mixins.hooks.entity

import gg.skytils.event.EventSubscriber
import gg.skytils.event.postCancellableSync
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod._event.ItemTossEvent
import gg.skytils.skytilsmod._event.PacketReceiveEvent
import gg.skytils.skytilsmod._event.PacketSendEvent
import gg.skytils.skytilsmod.utils.Utils
import net.minecraft.client.option.KeyBinding
import net.minecraft.entity.ItemEntity
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

var currentItem: Int? = null


object EntityPlayerSPHook : EventSubscriber {

    override fun setup() {
        register(::serverItemChange)
        register(::clientItemChange)
    }

    fun serverItemChange(event: PacketReceiveEvent<*>) {
        val packet = event.packet as? UpdateSelectedSlotS2CPacket ?: return
        currentItem = packet.slot
    }

    fun clientItemChange(event: PacketSendEvent<*>) {
        val packet = event.packet as? UpdateSelectedSlotC2SPacket ?: return
        currentItem = packet.selectedSlot
    }

}

fun onDropItem(dropAll: Boolean, cir: CallbackInfoReturnable<ItemEntity?>) {
    val stack = mc.player.inventory.main[currentItem ?: mc.player.inventory.selectedSlot]
    if (stack != null && postCancellableSync(ItemTossEvent(stack))) cir.returnValue = null
}

fun onKeybindCheck(keyBinding: KeyBinding): Boolean {
    return keyBinding === mc.options.sprintKey && Utils.inSkyblock && Skytils.config.alwaysSprint
}
