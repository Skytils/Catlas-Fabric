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

package gg.skytils.event.impl.render

import gg.skytils.event.CancellableEvent
import gg.skytils.event.Event
import net.minecraft.client.render.entity.LivingEntityRenderer
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity

//#if MC>=12000
import net.minecraft.client.render.entity.model.EntityModel
//#if MC>=12100
import net.minecraft.client.render.entity.state.LivingEntityRenderState
//#endif
//#endif

/**
 * [gg.skytils.event.mixins.render.MixinRendererLivingEntity.onRender]
 */
class LivingEntityPreRenderEvent
    //#if MC<12100
    //$$  <T : LivingEntity, M: EntityModel<T>>
    //#else
    <T : LivingEntity, S : LivingEntityRenderState, M : EntityModel<in S>>
    //#endif
    (
        val entity: T,
        //#if MC<12100
        //$$ val renderer: LivingEntityRenderer<T, M>,
        //#else
        val renderer: LivingEntityRenderer<T, S, M>,
        //#endif
        val x: Double, val y: Double, val z: Double, val partialTicks: Float
    ) : CancellableEvent()

/**
 * [gg.skytils.event.mixins.render.MixinRendererLivingEntity.onRenderPost]
 */
class LivingEntityPostRenderEvent(val entity: Entity) : Event()

/**
 * [gg.skytils.event.mixins.render.MixinRenderManager.shouldRender]
 *
 * TODO: find better inject mixin
 */
class CheckRenderEntityEvent<T : Entity>(val entity: T) : CancellableEvent()