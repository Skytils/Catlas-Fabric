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

import com.google.common.collect.EvictingQueue
import gg.essential.universal.UChat
import gg.essential.universal.UMatrixStack
import gg.skytils.event.EventPriority
import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.TickEvent
import gg.skytils.event.impl.play.ChatMessageReceivedEvent
import gg.skytils.event.impl.play.WorldUnloadEvent
import gg.skytils.event.impl.render.WorldDrawEvent
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod._event.MainThreadPacketReceiveEvent
import gg.skytils.skytilsmod._event.PacketSendEvent
import gg.skytils.skytilsmod.core.SoundQueue
import gg.skytils.skytilsmod.features.impl.events.GriffinBurrows.BurrowEstimation.lastParticleTrail
import gg.skytils.skytilsmod.features.impl.events.GriffinBurrows.BurrowEstimation.lastSoundTrail
import gg.skytils.skytilsmod.features.impl.events.GriffinBurrows.BurrowEstimation.otherGrassData
import gg.skytils.skytilsmod.utils.*
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.block.Blocks
import net.minecraft.item.Items
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.PlaySoundIdS2CPacket
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket
import net.minecraft.util.*
import java.awt.Color
import java.time.Duration
import java.time.Instant
import kotlin.math.*

object GriffinBurrows : EventSubscriber {
    val particleBurrows = hashMapOf<BlockPos, ParticleBurrow>()
    var lastDugParticleBurrow: BlockPos? = null
    val recentlyDugParticleBurrows = EvictingQueue.create<BlockPos>(5)

    var hasSpadeInHotbar = false
    var lastSpadeUse = -1L

    object BurrowEstimation {
        val arrows = mutableMapOf<Arrow, Instant>()
        val guesses = mutableMapOf<BurrowGuess, Instant>()
        val lastParticleTrail = mutableListOf<Vec3d>()
        val lastSoundTrail = linkedSetOf<Pair<Vec3d, Double>>()
        var lastTrailCreated = -1L

        fun getDistanceFromPitch(pitch: Double) =
            2805 * pitch - 1375

        val grassData by lazy {
            this::class.java.getResource("/assets/skytils/grassdata.txt")!!.readBytes()
        }

        val otherGrassData by lazy {
            this::class.java.getResource("/assets/skytils/hub_grass_heights.bin")!!.readBytes()
        }

        class Arrow(val directionVector: Vec3d, val pos: Vec3d)
    }

    override fun setup() {
        register(::onTick)
        register(::onSendPacket)
        register(::onReceivePacket)
        register(::onWorldChange)
        register(::onWorldRender)
        register(::onChat, EventPriority.Highest)
    }

    fun onTick(event: TickEvent) {
        hasSpadeInHotbar = mc.player != null && Utils.inSkyblock && (0..7).any {
            mc.player.inventory.getStack(it).isSpade
        }
        if (!Skytils.config.burrowEstimation) return
        BurrowEstimation.guesses.entries.removeIf { (_, instant) ->
            Duration.between(instant, Instant.now()).toMinutes() > 2
        }
        BurrowEstimation.arrows.entries.removeIf { (_, instant) ->
            Duration.between(instant, Instant.now()).toMillis() > 30_000L
        }
        if (!Skytils.config.experimentBurrowEstimation) return
        if (lastSoundTrail.size >= 2 && lastParticleTrail.size >= 2 && System.currentTimeMillis() - BurrowEstimation.lastTrailCreated > 1000) {
            printDevMessage({ "Trail found $lastParticleTrail" }, "griffinguess")
            printDevMessage({ "Sound trail $lastSoundTrail" }, "griffinguess")

            // chat did I get a 5 on the exam?
            // https://apcentral.collegeboard.org/media/pdf/statistics-formula-sheet-and-tables-2020.pdf
            val pitches = lastSoundTrail.map { it.second }
            val xMean = (lastSoundTrail.size - 1) / 2.0
            val xStd = sqrt(lastSoundTrail.indices.sumOf {
                (it - xMean) * (it - xMean)
            }) / (lastSoundTrail.size - 1)

            val yMean = pitches.average()
            val yStd = sqrt(pitches.sumOf {
                (it - yMean) * (it - yMean)
            }) / (pitches.size - 1)

            val numerator = lastSoundTrail.withIndex().sumOf { (i, pair) ->
                (i - xMean) * (pair.second - yMean)
            }

            val denominatorX = sqrt(lastSoundTrail.indices.sumOf { (it - xMean) * (it - xMean) })
            val denominatorY = sqrt(pitches.sumOf { (it - yMean) * (it - yMean) })

            val r = numerator / (denominatorX * denominatorY)

            val slope = r * yStd / xStd

            if (r < 0.95) UChat.chat("${Skytils.failPrefix} §cWarning: low correlation, r = $r. Burrow guess may be incorrect.")

            printDevMessage({ "Slope $slope, xbar $xMean, sx $xStd, ybar $yMean, sy $yStd, r $r" }, "griffinguess")

            val trail = lastParticleTrail.asReversed()

            // formula for distance guess comes from soopyboo32
            val distanceGuess = E / slope
            printDevMessage({ "Distance guess $distanceGuess" }, "griffinguess")

            val directionVector = trail[0].subtract(trail[1]).normalize()
            printDevMessage({ "Direction vector $directionVector" }, "griffinguess")

            val guessPos = trail.last().add(
                directionVector * distanceGuess
            )
            printDevMessage({ "Guess pos $guessPos" }, "griffinguess")

            // offset of 300 blocks for both x and z
            // x ranges from 195 to -281
            // z ranges from 207 to -233

            fun getIndex(x: Int, z: Int) = (x - -281) + (z - -233) * (195 - -281 + 1)

            val guess = BurrowGuess(guessPos.x.toInt(), otherGrassData.getOrNull(getIndex(guessPos.x.toInt(), guessPos.z.toInt()))?.toInt() ?: 0, guessPos.z.toInt())
            BurrowEstimation.guesses[guess] = Instant.now()

            lastParticleTrail.clear()
            BurrowEstimation.lastTrailCreated = -1
            lastSoundTrail.clear()
        }
    }

    fun onChat(event: ChatMessageReceivedEvent) {
        val unformatted = event.message.string.stripControlCodes()
        if (Skytils.config.showGriffinBurrows &&
            (unformatted.startsWith("You died") || unformatted.startsWith("☠ You were killed") ||
                    unformatted.startsWith("You dug out a Griffin Burrow! (") ||
                    unformatted == "You finished the Griffin burrow chain! (4/4)")
        ) {
            if (lastDugParticleBurrow != null) {
                val particleBurrow =
                    particleBurrows[lastDugParticleBurrow] ?: return
                recentlyDugParticleBurrows.add(lastDugParticleBurrow)
                particleBurrows.remove(particleBurrow.blockPos)
                printDevMessage({ "Removed $particleBurrow" }, "griffin")
                lastDugParticleBurrow = null
            }
        }
        if (event.message.method_10865() == "§r§6Poof! §r§eYou have cleared your griffin burrows!§r") {
            particleBurrows.clear()
            recentlyDugParticleBurrows.clear()
            lastDugParticleBurrow = null
            BurrowEstimation.guesses.clear()
            BurrowEstimation.arrows.clear()
            lastParticleTrail.clear()
            BurrowEstimation.lastTrailCreated = -1
            lastSpadeUse = -1
            lastSoundTrail.clear()
        }
    }

    fun onSendPacket(event: PacketSendEvent<*>) {
        if (!Utils.inSkyblock || !Skytils.config.showGriffinBurrows || mc.player == null || SBInfo.mode != SkyblockIsland.Hub.mode) return
        if (mc.player.method_0_7087()?.isSpade != true) return

        if (event.packet is PlayerInteractBlockC2SPacket && event.packet.method_12548().y == -1) {
            lastSpadeUse = System.currentTimeMillis()
            lastParticleTrail.clear()
            BurrowEstimation.lastTrailCreated = -1
            lastSoundTrail.clear()
            printDevMessage("Spade used", "griffinguess")
        } else {
            val pos =
                when {
                    event.packet is PlayerActionC2SPacket && event.packet.action == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK -> {
                        event.packet.pos
                    }
                    event.packet is PlayerInteractBlockC2SPacket && event.packet.method_0_5804() != null -> event.packet.method_12548()
                    else -> return
                }
            if (mc.world.getBlockState(pos).block !== Blocks.field_0_767) return
            particleBurrows[pos]?.blockPos?.let {
                printDevMessage({ "Clicked on $it" }, "griffin")
                lastDugParticleBurrow = it
            }
        }
    }

    fun onWorldRender(event: WorldDrawEvent) {
        if (Skytils.config.showGriffinBurrows) {
            val matrixStack = UMatrixStack()
            for (pb in particleBurrows.values) {
                if (pb.hasEnchant && pb.hasFootstep && pb.type != -1) {
                    pb.drawWaypoint(event.partialTicks, matrixStack)
                }
            }
            if (Skytils.config.burrowEstimation) {
                for (bg in BurrowEstimation.guesses.keys) {
                    bg.drawWaypoint(event.partialTicks, matrixStack)
                }
                for (arrow in BurrowEstimation.arrows.keys) {
                    RenderUtil.drawCircle(
                        matrixStack,
                        arrow.pos.x,
                        arrow.pos.y + 0.2,
                        arrow.pos.z,
                        event.partialTicks,
                        5.0,
                        100,
                        255,
                        128,
                        0,
                    )
                }
            }
        }
    }

    fun onWorldChange(event: WorldUnloadEvent) {
        particleBurrows.clear()
        recentlyDugParticleBurrows.clear()
    }

    fun onReceivePacket(event: MainThreadPacketReceiveEvent<*>) {
        if (!Utils.inSkyblock) return
        when (event.packet) {
            is ParticleS2CPacket -> {
                if (Skytils.config.showGriffinBurrows && hasSpadeInHotbar) {
                    if (SBInfo.mode != SkyblockIsland.Hub.mode) return
                    event.packet.apply {
                        if (type == ParticleType.DRIP_LAVA && count == 2 && speed == -.5f && offsetX == 0f && offsetY == 0f && offsetZ == 0f && isLongDistance) {
                            lastParticleTrail.add(vec3)
                            BurrowEstimation.lastTrailCreated = System.currentTimeMillis()
                            printDevMessage({ "Found trail point $x $y $z" }, "griffinguess")
                        } else {
                            val type = ParticleType.getParticleType(this) ?: return
                            val pos = BlockPos(x, y, z).method_10074()
                            if (recentlyDugParticleBurrows.contains(pos)) return
                            BurrowEstimation.guesses.keys.associateWith { guess ->
                                (pos.x - guess.x) * (pos.x - guess.x) + (pos.z - guess.z) * (pos.z - guess.z)
                            }.minByOrNull { it.value }?.let { (guess, distance) ->
                                // printDevMessage({ "Nearest guess is $distance blocks^2 away" }, "griffin", "griffinguess")
                                if (distance <= 25 * 25) {
                                    BurrowEstimation.guesses.remove(guess)
                                }
                            }
                            val burrow = particleBurrows.getOrPut(pos) {
                                ParticleBurrow(pos, hasFootstep = false, hasEnchant = false)
                            }
                            if (burrow.type == -1 && type.isBurrowType) {
                                if (Skytils.config.pingNearbyBurrow) {
                                    SoundQueue.addToQueue("random.orb", 0.8f, 1f, 0, true)
                                }
                            }
                            when (type) {
                                ParticleType.FOOTSTEP -> burrow.hasFootstep = true
                                ParticleType.ENCHANT -> burrow.hasEnchant = true
                                ParticleType.EMPTY -> burrow.type = 0
                                ParticleType.MOB -> burrow.type = 1
                                ParticleType.TREASURE -> burrow.type = 2
                            }
                        }
                    }
                }
            }
            is EntityEquipmentUpdateS2CPacket -> {
                if (!Skytils.config.burrowEstimation || SBInfo.mode != SkyblockIsland.Hub.mode || Skytils.config.experimentBurrowEstimation) return
                val entity = mc.world?.getEntityById(event.packet.id)
                (entity as? ArmorStandEntity)?.let { armorStand ->
                    if (event.packet.stack?.item != Items.ARROW) return
                    if (armorStand.method_5831(mc.player?.blockPos) >= 27) return
                    printDevMessage({ "Found armor stand with arrow" }, "griffin", "griffinguess")
                    val yaw = Math.toRadians(armorStand.yaw.toDouble())
                    val lookVec = Vec3d(
                        -sin(yaw),
                        0.0,
                        cos(yaw)
                    )
                    val offset = Vec3d(-sin(yaw + PI/2), 0.0, cos(yaw + PI/2)) * 0.9
                    val origin = armorStand.pos.add(offset)
                    BurrowEstimation.arrows.put(BurrowEstimation.Arrow(lookVec, origin), Instant.now())
                }
            }
            is PlaySoundIdS2CPacket -> {
                if (!Skytils.config.burrowEstimation || SBInfo.mode != SkyblockIsland.Hub.mode) return
                if (event.packet.method_11460() != "note.harp" || event.packet.volume != 1f) return
                printDevMessage({ "Found note harp sound ${event.packet.pitch} ${event.packet.volume} ${event.packet.x} ${event.packet.y} ${event.packet.z}" }, "griffinguess")
                if (lastSpadeUse != -1L && System.currentTimeMillis() - lastSpadeUse < 1000) {
                    lastSoundTrail.add(Vec3d(event.packet.x, event.packet.y, event.packet.z) to event.packet.pitch.toDouble())
                }
                if (Skytils.config.experimentBurrowEstimation) return
                val (arrow, distance) = BurrowEstimation.arrows.keys
                    .associateWith { arrow ->
                        arrow.pos.squareDistanceTo(event.packet.x, event.packet.y, event.packet.z)
                    }.minByOrNull { it.value } ?: return
                printDevMessage({ "Nearest arrow is $distance blocks away ${arrow.pos}" }, "griffin", "griffinguess")
                if (distance > 25) return
                val guessPos = arrow.pos.add(
                    arrow.directionVector * BurrowEstimation.getDistanceFromPitch(event.packet.pitch.toDouble())
                )

                var y: Int
                var x = guessPos.x.toInt()
                var z = guessPos.z.toInt()
                // offset of 300 blocks for both x and z
                // x ranges from 195 to -281
                // z ranges from 207 to -233
                do {
                    y = BurrowEstimation.grassData.getOrNull((x++ % 507) * 507 + (z++ % 495))?.toInt() ?: 0
                } while (y < 2)
                val guess = BurrowGuess(guessPos.x.toInt(), y, guessPos.z.toInt())
                BurrowEstimation.arrows.remove(arrow)
                BurrowEstimation.guesses[guess] = Instant.now()
            }
        }
    }

    abstract class Diggable {
        abstract val x: Int
        abstract val y: Int
        abstract val z: Int
        val blockPos: BlockPos by lazy {
            BlockPos(x, y, z)
        }

        abstract val waypointText: String
        abstract val color: Color
        fun drawWaypoint(partialTicks: Float, matrixStack: UMatrixStack) {
            val (viewerX, viewerY, viewerZ) = RenderUtil.getViewerPos(partialTicks)
            val renderX = this.x - viewerX
            val renderY = this.y - viewerY
            val renderZ = this.z - viewerZ
            val distSq = renderX * renderX + renderY * renderY + renderZ * renderZ
            RenderSystem.disableDepthTest()
            RenderSystem.disableCull()
            RenderUtil.drawFilledBoundingBox(
                matrixStack,
                Box(renderX, renderY, renderZ, renderX + 1, renderY + 1, renderZ + 1).expandBlock(),
                this.color,
                (0.1f + 0.005f * distSq.toFloat()).coerceAtLeast(0.2f)
            )
            RenderSystem.method_4407()
            if (distSq > 5 * 5) RenderUtil.renderBeaconBeam(renderX, renderY + 1, renderZ, this.color.rgb, 1.0f, partialTicks)
            RenderUtil.renderWaypointText(
                waypointText,
                x + 0.5,
                y + 5.0,
                z + 0.5,
                partialTicks,
                matrixStack
            )
            RenderSystem.method_4406()
            RenderSystem.method_4397()
            RenderSystem.enableDepthTest()
            RenderSystem.enableCull()
        }
    }

    data class BurrowGuess(
        override val x: Int,
        override val y: Int,
        override val z: Int
    ) : Diggable() {
        override val waypointText = "§aBurrow §6(Guess)"
        override val color = Color.ORANGE
    }

    data class ParticleBurrow(
        override val x: Int,
        override val y: Int,
        override val z: Int,
        var hasFootstep: Boolean,
        var hasEnchant: Boolean
    ) : Diggable() {
        constructor(vec3: Vec3i, hasFootstep: Boolean, hasEnchant: Boolean) : this(
            vec3.x,
            vec3.y,
            vec3.z,
            hasFootstep,
            hasEnchant
        )

        var type = -1
            set(value) {
                field = value
                when (value) {
                    0 -> {
                        waypointText = "§aStart §a(Particle)"
                        color = Skytils.config.emptyBurrowColor
                    }
                    1 -> {
                        waypointText = "§cMob §a(Particle)"
                        color = Skytils.config.mobBurrowColor
                    }
                    2 -> {
                        waypointText = "§6Treasure §a(Particle)"
                        color = Skytils.config.treasureBurrowColor
                    }
                }
            }

        override var waypointText = "§7Unknown §a(Particle)"
            private set

        override var color = Color.WHITE
            private set
    }

    private val ItemStack?.isSpade
        get() = ItemUtil.getSkyBlockItemID(this) == "ANCESTRAL_SPADE"

    private enum class ParticleType(val check: ParticleS2CPacket.() -> Boolean, val isBurrowType: Boolean = true) {
        EMPTY({
            type == ParticleType.CRIT_MAGIC && count == 4 && speed == 0.01f && offsetX == 0.5f && offsetY == 0.1f && offsetZ == 0.5f
        }),
        MOB({
            type == ParticleType.CRIT && count == 3 && speed == 0.01f && offsetX == 0.5f && offsetY == 0.1f && offsetZ == 0.5f

        }),
        TREASURE({
            type == ParticleType.DRIP_LAVA && count == 2 && speed == 0.01f && offsetX == 0.35f && offsetY == 0.1f && offsetZ == 0.35f
        }),
        FOOTSTEP({
            type == ParticleType.FOOTSTEP && count == 1 && speed == 0.0f && offsetX == 0.05f && offsetY == 0.0f && offsetZ == 0.05f
        }, false),
        ENCHANT({
            type == ParticleType.ENCHANTMENT_TABLE && count == 5 && speed == 0.05f && offsetX == 0.5f && offsetY == 0.4f && offsetZ == 0.5f
        }, false);

        companion object {
            fun getParticleType(packet: ParticleS2CPacket): ParticleType? {
                if (!packet.isLongDistance) return null
                return entries.find {
                    it.check(packet)
                }
            }
        }
    }
}