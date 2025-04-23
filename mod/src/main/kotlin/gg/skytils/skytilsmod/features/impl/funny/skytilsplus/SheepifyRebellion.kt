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

package gg.skytils.skytilsmod.features.impl.funny.skytilsplus

import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.entity.EntityJoinWorldEvent
import gg.skytils.event.impl.entity.LivingEntityDeathEvent
import gg.skytils.event.impl.play.WorldUnloadEvent
import gg.skytils.event.impl.render.LivingEntityPreRenderEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod.core.tickTimer
import gg.skytils.skytilsmod.mixins.transformers.accessors.AccessorEntity
import gg.skytils.skytilsmod.mixins.transformers.accessors.AccessorEntitySlime
import gg.skytils.skytilsmod.mixins.transformers.accessors.AccessorEnumChatFormatting
import gg.skytils.skytilsmod.mixins.transformers.accessors.AccessorEnumDyeColor
import gg.skytils.skytilsmod.utils.ReflectionHelper.getClassHelper
import gg.skytils.skytilsmod.utils.ReflectionHelper.getFieldHelper
import gg.skytils.skytilsmod.utils.ReflectionHelper.getMethodHelper
import gg.skytils.skytilsmod.utils.SuperSecretSettings
import gg.skytils.skytilsmod.utils.Utils
import gg.skytils.skytilsmod.utils.hasMoved
import kotlinx.serialization.Serializable
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.world.ClientWorld
import net.minecraft.client.render.entity.LivingEntityRenderer
import net.minecraft.entity.passive.PassiveEntity
import net.minecraft.entity.EntityType
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.mob.AbstractSkeletonEntity
import net.minecraft.entity.mob.SlimeEntity
import net.minecraft.entity.passive.*
import net.minecraft.util.DyeColor
import kotlin.math.abs

object SheepifyRebellion : EventSubscriber {
    val fakeWorld by lazy {
        getClassHelper("gg.essential.gui.common.EmulatedUI3DPlayer\$FakeWorld")?.let { clazz ->
            val instance = clazz.getFieldHelper("INSTANCE")?.get(null)
            clazz.getMethodHelper("getFakeWorld")?.invoke(instance) as? ClientWorld
        } ?: error("Failed to get FakeWorld")
    }

    val dummyModelMap = hashMapOf<AbstractClientPlayerEntity, LivingEntity>()

    val skytilsPlusUsernames = mutableSetOf<String>()
    val skytilsPlusColors = mutableMapOf<String, DyeColor>()

    val lookup = DyeColor.entries.associateBy { ((it as AccessorEnumDyeColor).chatColor as AccessorEnumChatFormatting).formattingCode }

    val isSkytilsPlus by lazy {
        Utils.isBSMod || SuperSecretSettings.sheepifyRebellion
    }

    val palEntities: List<Int> by lazy {
        mutableListOf<Int>().apply {
            addAll(50..68)
            addAll(90..101)
            remove(63) // vault the ender dragon cause the model is different
            if (isSkytilsPlus) {
                remove(91)
            }
        }
    }

    fun playerSpawn(event: EntityJoinWorldEvent) {
        if (event.entity !is AbstractClientPlayerEntity || event.entity.uuid.version() == 2) return

        if (Utils.inSkyblock) {
            checkForFakeModel(event.entity as AbstractClientPlayerEntity)
        } else if (event.entity is ClientPlayerEntity) {
            val world = event.entity.world
            tickTimer(5) {
                if (Utils.inSkyblock && mc.world == world) {
                    world.players.forEach {
                        if (it is AbstractClientPlayerEntity && it.uuid.version() != 2 && it !in dummyModelMap) {
                            checkForFakeModel(it)
                        }
                    }
                }
            }
        }
    }

    fun playerLeave(event: LivingEntityDeathEvent) {
        dummyModelMap.remove(event.entity)?.remove()
    }

    fun onWorldUnload(event: WorldUnloadEvent) {
        dummyModelMap.clear()
    }

    fun onRender(event: LivingEntityPreRenderEvent<*>) {
        val fakeEntity = dummyModelMap[event.entity] ?: return
        event.cancelled = true
        val renderer = event.renderer.renderManager.getRenderer<LivingEntity>(fakeEntity)
        copyProperties(fakeEntity, event.entity)
        renderer.render(fakeEntity, event.x, event.y, event.z, event.entity.yaw, 1f)
        (event.renderer as LivingEntityRenderer<LivingEntity>).renderLabelIfPresent(event.entity, event.x, event.y, event.z)
    }

    fun checkForFakeModel(entity: AbstractClientPlayerEntity) {
        var fakeEntity: LivingEntity? = null
        if ((SuperSecretSettings.catGaming || (Utils.isBSMod && SkytilsPlus.redeemed)) && entity is ClientPlayerEntity) {
            fakeEntity = CatEntity(fakeWorld)
            fakeEntity.catType = 3
        } else if (SuperSecretSettings.palworld) {
            val uuid = entity.uuid
            val most = abs(uuid.mostSignificantBits)
            val least = abs(uuid.leastSignificantBits)
            fakeEntity = EntityType.createInstanceFromId(if (SuperSecretSettings.cattiva) 98 else palEntities[(most % palEntities.size).toInt()], fakeWorld) as LivingEntity
            when (fakeEntity) {
                is CatEntity -> fakeEntity.catType = (least % 4).toInt()
                is SheepEntity -> fakeEntity.color = DyeColor.method_0_8193((least % 16).toInt())
                is WolfEntity -> {
                    fakeEntity.isTamed = true
                    fakeEntity.collarColor = DyeColor.method_0_8193((least % 16).toInt())
                }
                is AbstractSkeletonEntity -> fakeEntity.method_0_7863((least % 2).toInt())
                is AbstractHorseEntity -> {
                    fakeEntity.method_0_7566((least % 5).toInt())
                    fakeEntity.method_0_7568((most % 7).toInt() or ((least % 5).toInt() shl 8))
                    fakeEntity.method_0_7564((most % 2).toInt() == 1)
                }
                is RabbitEntity -> {
                    fakeEntity.rabbitType = (least % 7).toInt().takeIf { it != 6 } ?: 99
                }
                is SlimeEntity -> {
                    (fakeEntity as AccessorEntitySlime).invokeSetSlimeSize((least % 3).toInt())
                }
                is BatEntity -> {
                    fakeEntity.isRoosting = false
                }
            }
        } else if (isSkytilsPlus) {
            val color = skytilsPlusColors[entity.name] ?: return
            fakeEntity = SheepEntity(fakeWorld)
            fakeEntity.color = color

            if (skytilsPlusUsernames.contains(entity.name)) {
                fakeEntity.customName = "jeb_"
                fakeEntity.isCustomNameVisible = false
            }
        }

        if (fakeEntity != null) {
            dummyModelMap[entity] = fakeEntity
            fakeEntity.teleporting = true
            fakeEntity.velocityX = 0.0
            fakeEntity.velocityY = 0.0
            fakeEntity.velocityZ = 0.0
            fakeEntity.noClip = true
        }
    }

    fun copyProperties(entity: LivingEntity, originalEntity: LivingEntity) {
        entity as AccessorEntity
        originalEntity as AccessorEntity

        entity.copyPositionAndRotation(originalEntity)
        entity.headYaw = originalEntity.headYaw
        entity.lastLimbDistance = originalEntity.lastLimbDistance
        entity.limbDistance = originalEntity.limbDistance
        entity.limbAngle = originalEntity.limbAngle
        entity.lastHandSwingProgress = originalEntity.lastHandSwingProgress
        entity.handSwingProgress = originalEntity.handSwingProgress
        entity.handSwinging = originalEntity.handSwinging
        entity.setBodyYaw(originalEntity.bodyYaw)
        entity.hurtTime = originalEntity.hurtTime
        entity.fire = originalEntity.fire
        entity.deathTime = originalEntity.deathTime
        entity.stuckArrowCount = originalEntity.stuckArrowCount
        entity.isSprinting = originalEntity.isSprinting
        entity.isInvisible = originalEntity.isInvisible

        if (entity.armorItems.size >= originalEntity.armorItems.size) {
            for (i in originalEntity.armorItems.indices) {
                entity.armorItems[i] = originalEntity.armorItems[i]
            }
        }

        if (originalEntity.isBaby && entity is PassiveEntity) {
            entity.breedingAge = -1
        }

        if (entity is TameableEntity) {
            entity.isInSittingPose = originalEntity.isSneaking && !originalEntity.hasMoved
        } else {
            entity.isSneaking = originalEntity.isSneaking
        }

        entity.age = originalEntity.age
    }

    override fun setup() {
        register(::playerSpawn)
        register(::playerLeave)
        register(::onWorldUnload)
    }

    @Serializable
    data class SkytilsPlusData(val active: List<String>, val inactive: List<String>)
}