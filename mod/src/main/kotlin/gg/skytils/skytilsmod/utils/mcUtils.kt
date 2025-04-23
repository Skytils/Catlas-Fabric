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

package gg.skytils.skytilsmod.utils

import gg.essential.elementa.state.v2.State
import gg.essential.universal.wrappers.UPlayer
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i

//#if FORGE
//$$ import net.minecraft.launchwrapper.Launch
//$$ import net.minecraftforge.client.ClientCommandHandler
//$$ import net.minecraftforge.fml.common.Loader
//#endif

//#if FABRIC
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
//#endif

val isDeobfuscatedEnvironment = State {
    //#if FORGE
    //#if MC<11400
    //$$ Launch.blackboard.getOrDefault("fml.deobfuscatedEnvironment", false) as Boolean
    //#else
    //$$ (System.getenv("target") ?: "").lowercase() == "fmluserdevclient"
    //#endif
    //#else
    FabricLoader.getInstance().isDevelopmentEnvironment
    //#endif
}

fun isModLoaded(id: String) =
    //#if FORGE
    //#if MC<11400
    //$$ Loader.isModLoaded(id)
    //#else
    //$$ FMLLoader.getLoadingModList().getModFileById(id)
    //#endif
    //#else
    FabricLoader.getInstance().isModLoaded(id)
    //#endif

fun runClientCommand(command: String) =
    //#if MC<11400
    //$$ ClientCommandHandler.instance.method_0_6233(UPlayer.getPlayer(), command)
    //#else
    ClientCommandManager.getActiveDispatcher()?.execute(command.removePrefix("/"), UPlayer.getPlayer()?.networkHandler?.commandSource as? FabricClientCommandSource ?: error("No command source"))
    //#endif

fun isTimechangerLoaded() =
    //#if FORGE
    //$$ Loader.instance().activeModList.any { it.modId == "timechanger" && it.version == "1.0" }
    //#else
    false
    //#endif

operator fun ClientPlayerEntity.component1() = this.x
operator fun ClientPlayerEntity.component2() = this.y
operator fun ClientPlayerEntity.component3() = this.z

inline fun BlockPos(vec: Vec3d): BlockPos = BlockPos(MathHelper.floor(vec.x), MathHelper.floor(vec.y), MathHelper.floor(vec.z))


inline fun Vec3d(pos: Vec3i): Vec3d = Vec3d(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())

fun GenericContainerScreen.getSlot(id: Int) =
    //#if MC<12000
    //$$ handler.getSlot(id)
    //#else
    screenHandler.getSlot(id)
    //#endif

val ItemStack.displayNameStr: String
    inline get() = this.name
        //#if MC>=11600
        .string
        //#endif