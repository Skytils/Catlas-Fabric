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
package gg.skytils.skytilsmod.features.impl.events

import gg.essential.universal.UMatrixStack
import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.entity.EntityAttackEvent
import gg.skytils.event.impl.play.EntityInteractEvent
import gg.skytils.event.impl.play.WorldUnloadEvent
import gg.skytils.event.impl.render.LivingEntityPreRenderEvent
import gg.skytils.event.impl.render.WorldDrawEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod.utils.*
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.entity.passive.PigEntity
import net.minecraft.util.math.Box
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import java.awt.Color

object TechnoMayor : EventSubscriber {
    private val shinyPigs = HashMap<Vec3d, PigEntity?>()
    private var latestPig: PigEntity? = null

    override fun setup() {
        register(::onRenderSpecialLivingPre)
        register(::onWorldRender)
        register(::onEntityInteract)
        register(::onEntityAttack)
        register(::onWorldChange)
    }

    fun onRenderSpecialLivingPre(event: LivingEntityPreRenderEvent<*>) {
        if (!Utils.inSkyblock) return
        val e = event.entity
        if (!e.isValidPigLabel()) return
        val pos = e.blockPos
        mc.world.method_0_375(
            e,
            Box(pos, BlockPos(pos.x + 1, pos.y + 1, pos.z + 1))
        ).find {
            it is ArmorStandEntity && mc.player.name in it.customName
        }?.let {
            it.world.method_8463(it)
            e.world.method_8463(e)
            shinyPigs.putIfAbsent(Vec3d(pos.x + 0.5, (pos.y - 2).toDouble(), pos.z + 0.5), latestPig)
            latestPig = null
        }
    }

    fun onWorldRender(event: WorldDrawEvent) {
        if (!Utils.inSkyblock) return
        if (SBInfo.mode != SkyblockIsland.Hub.mode && SBInfo.mode != SkyblockIsland.FarmingIsland.mode) return
        if (!Skytils.config.shinyOrbWaypoints) return

        shinyPigs.values.removeAll { it?.isRemoved != false }

        val (viewerX, viewerY, viewerZ) = RenderUtil.getViewerPos(event.partialTicks)
        val matrixStack = UMatrixStack()
        GlState.pushState()
        for (entry in shinyPigs) {
            val orb = entry.key
            val pig = entry.value
            val x = orb.x - viewerX
            val y = orb.y - viewerY
            val z = orb.z - viewerZ
            val distSq = x * x + y * y + z * z
            RenderSystem.disableCull()
            RenderSystem.method_4407()

            if (distSq > 5 * 5) RenderUtil.renderBeaconBeam(x, y, z, Color(114, 245, 82).rgb, 0.75f, event.partialTicks)
            RenderSystem.disableDepthTest()
            RenderUtil.renderWaypointText(
                "Orb",
                orb.x,
                orb.y + 1.5f,
                orb.z,
                event.partialTicks,
                matrixStack
            )
            if (Skytils.config.shinyPigLocations) {
                if (pig != null) {
                    RenderUtil.renderWaypointText(
                        "Pig",
                        pig.x,
                        pig.y + 0.5,
                        pig.z,
                        event.partialTicks,
                        matrixStack
                    )
                    RenderUtil.draw3DLine(
                        Vec3d(pig.x, pig.y + 0.5, pig.z), Vec3d(orb.x, orb.y + 1.5, orb.z),
                        1, Color.RED, event.partialTicks, matrixStack
                    )
                    RenderUtil.drawOutlinedBoundingBox(pig.boundingBox, Color.RED, 1f, event.partialTicks)
                }
            }
        }
        GlState.popState()
    }

    fun onEntityInteract(event: EntityInteractEvent) {
        if (!Utils.inSkyblock) return
        checkPig(event.entity as? PigEntity ?: return)
    }

    fun onEntityAttack(event: EntityAttackEvent) {
        if (!Utils.inSkyblock) return
        checkPig(event.target as? PigEntity ?: return)
    }

    fun checkPig(entity: PigEntity) {
        if (mc.world.method_0_375(
            entity,
            Box(
                BlockPos(entity.x - 1, entity.y, entity.z - 1),
                BlockPos(entity.x + 1, entity.y + 2, entity.z + 1)
            )
        ).any { it.isValidPigLabel() }) {
            latestPig = entity
        }
    }

    fun onWorldChange(event: WorldUnloadEvent) {
        shinyPigs.clear()
        latestPig = null
    }

    private fun Entity.isValidPigLabel() = this is ArmorStandEntity && !isRemoved && customName == "§6§lSHINY PIG"
}