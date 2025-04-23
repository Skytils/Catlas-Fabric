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

package gg.skytils.skytilsmod.listeners

import gg.skytils.event.EventSubscriber
import gg.skytils.event.impl.entity.EntityJoinWorldEvent
import gg.skytils.event.impl.play.WorldUnloadEvent
import gg.skytils.event.postSync
import gg.skytils.event.register
import gg.skytils.skytilsmod.Skytils
import gg.skytils.skytilsmod.Skytils.IO
import gg.skytils.skytilsmod.Skytils.mc
import gg.skytils.skytilsmod._event.DungeonPuzzleCompletedEvent
import gg.skytils.skytilsmod._event.DungeonPuzzleDiscoveredEvent
import gg.skytils.skytilsmod._event.DungeonPuzzleResetEvent
import gg.skytils.skytilsmod._event.MainThreadPacketReceiveEvent
import gg.skytils.skytilsmod.core.tickTimer
import gg.skytils.skytilsmod.features.impl.dungeons.DungeonFeatures
import gg.skytils.skytilsmod.features.impl.dungeons.DungeonTimer
import gg.skytils.skytilsmod.features.impl.dungeons.catlas.core.DungeonMapPlayer
import gg.skytils.skytilsmod.features.impl.dungeons.catlas.core.map.Room
import gg.skytils.skytilsmod.features.impl.dungeons.catlas.core.map.RoomType
import gg.skytils.skytilsmod.features.impl.dungeons.catlas.handlers.DungeonInfo
import gg.skytils.skytilsmod.features.impl.dungeons.catlas.utils.ScanUtils
import gg.skytils.skytilsmod.listeners.ServerPayloadInterceptor.getResponse
import gg.skytils.skytilsmod.mixins.transformers.accessors.AccessorNetHandlerPlayClient
import gg.skytils.skytilsmod.utils.*
import gg.skytils.skytilsmod.utils.NumberUtil.romanToDecimal
import gg.skytils.skytilsws.client.WSClient
import gg.skytils.skytilsws.shared.packet.C2SPacketDungeonEnd
import gg.skytils.skytilsws.shared.packet.C2SPacketDungeonRoom
import gg.skytils.skytilsws.shared.packet.C2SPacketDungeonRoomSecret
import gg.skytils.skytilsws.shared.packet.C2SPacketDungeonStart
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPartyInfoPacket
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPartyInfoPacket
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.client.util.DefaultSkinHelper
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket
import net.minecraft.util.Identifier

object DungeonListener : EventSubscriber {
    val team = hashMapOf<String, DungeonTeammate>()
    val deads = hashSetOf<DungeonTeammate>()
    val disconnected = hashSetOf<String>()
    val incompletePuzzles = hashSetOf<String>()
    val terminalStatePuzzles = hashSetOf<String>()

    val partyCountPattern = Regex("§r {9}§r§b§lParty §r§f\\((?<count>[1-5])\\)§r")
    private val classPattern =
        Regex("§r(?:§.)+(?:\\[.+] )?(?<name>\\w+?)(?:§.)* (?:§r(?:§[\\da-fklmno]){1,2}.+ )?§r§f\\(§r§d(?:(?<class>Archer|Berserk|Healer|Mage|Tank) (?<lvl>\\w+)|§r§7EMPTY|§r§cDEAD)§r§f\\)§r")
    private val puzzleRegex = Regex("§r (?<puzzle>.+): §r§7\\[§r(?:§a§l(?<completed>✔)|§c§l(?<failed>✖)|§6§l(?<missing>✦))§r§7] ?§r")
    private val deathRegex = Regex("§r§c ☠ §r§7(?:You were |(?:§.)+(?<username>\\w+)§r)(?<reason>.*) and became a ghost§r§7\\.§r")
    private val reconnectedRegex = Regex("§r§c ☠ §r§7(?:§.)+(?<username>\\w+) §r§7reconnected§r§7.§r")
    private val reviveRegex = Regex("^§r§a ❣ §r§7(?:§.)+(?<username>\\w+)§r§a was revived")
    private val secretsRegex = Regex("\\s*§7(?<secrets>\\d+)\\/(?<maxSecrets>\\d+) Secrets")
    private val keyPickupRegex = Regex("§r§e§lRIGHT CLICK §r§7on §r§7.+?§r§7 to open it\\. This key can only be used to open §r§a(?<num>\\d+)§r§7 door!§r")
    private val witherDoorOpenedRegex = Regex("^(?:\\[.+?] )?(?<name>\\w+) opened a WITHER door!$")
    private const val bloodOpenedString = "§r§cThe §r§c§lBLOOD DOOR§r§c has been opened!§r"
    var outboundRoomQueue = Channel<C2SPacketDungeonRoom>(UNLIMITED)
    // 1 + i * 4
    private val playerEntryNames = mapOf("!A-b" to 1, "!A-f" to 5, "!A-j" to 9, "!A-n" to 13, "!A-r" to 17)

    fun onWorldLoad(event: WorldUnloadEvent) {
        if (event.world == mc.world) {
            team.clear()
            deads.clear()
            disconnected.clear()
            incompletePuzzles.clear()
            terminalStatePuzzles.clear()
        }
    }

    fun onPacket(event: MainThreadPacketReceiveEvent<*>) {
        if (!Utils.inDungeons) return
        if (event.packet is GameMessageS2CPacket) {
            val text = event.packet.content.formattedText
            val unformatted = text.stripControlCodes()
            if (event.packet.overlay) {
                secretsRegex.find(text)?.destructured?.also { (secrets, maxSecrets) ->
                    val sec = secrets.toInt()
                    val max = maxSecrets.toInt().coerceAtLeast(sec)

                    run setFoundSecrets@ {
                        val tile = ScanUtils.getRoomFromPos(mc.player!!.blockPos)
                        if (tile is Room && tile.data.name != "Unknown") {
                            val room = tile.uniqueRoom ?: return@setFoundSecrets
                            if (room.foundSecrets != sec) {
                                room.foundSecrets = sec
                                if (team.size > 1)
                                    WSClient.sendPacketAsync(C2SPacketDungeonRoomSecret(SBInfo.server ?: return@setFoundSecrets, room.mainRoom.data.name, sec))
                            }
                        }
                    }

                }
            } else {
                if (text.stripControlCodes()
                        .trim() == "> EXTRA STATS <"
                ) {
                    if (team.size > 1) {
                        SBInfo.server?.let {
                            WSClient.sendPacketAsync(C2SPacketDungeonEnd(it))
                        }
                    }
                } else if (text.startsWith("§r§c ☠ ")) {
                    if (text.endsWith(" §r§7reconnected§r§7.§r")) {
                        val match = reconnectedRegex.find(text) ?: return
                        val username = match.groups["username"]?.value ?: return
                        disconnected.remove(username)
                    } else if (text.endsWith(" and became a ghost§r§7.§r")) {
                        val match = deathRegex.find(text) ?: return
                        val username = match.groups["username"]?.value ?: mc.player!!.gameProfile.name
                        val teammate = team[username] ?: return
                        markDead(teammate)

                        if (match.groups["reason"]?.value?.contains("disconnected") == true) {
                            disconnected.add(username)
                        }
                    }
                } else if (text.startsWith("§r§a ❣ ")) {
                    val match = reviveRegex.find(text) ?: return
                    val username = match.groups["username"]!!.value
                    val teammate = team[username] ?: return
                    markRevived(teammate)
                } else if (text == bloodOpenedString) {
                    DungeonInfo.keys--
                } else if (text == "§r§aStarting in 1 second.§r") {
                    Skytils.launch {
                        delay(2000)
                        if (DungeonTimer.dungeonStartTime != -1L && team.size > 1) {
                            val party = async {
                                ServerboundPartyInfoPacket().getResponse<ClientboundPartyInfoPacket>()
                            }
                            val partyMembers = party.await().members.ifEmpty { setOf(mc.player!!.uuid) }.mapTo(hashSetOf()) { it.toString() }
                            val entrance = DungeonInfo.uniqueRooms["Entrance"] ?: error("Entrance not found")
                            assert(entrance.mainRoom.data.type == RoomType.ENTRANCE)
                            printDevMessage("hi", "dungeonws")
                            launch(IO.coroutineContext) {
                                WSClient.sendPacketAsync(C2SPacketDungeonStart(
                                    serverId = SBInfo.server ?: return@launch,
                                    floor = DungeonFeatures.dungeonFloor!!,
                                    members = partyMembers,
                                    startTime = DungeonTimer.dungeonStartTime,
                                    entranceLoc = entrance.mainRoom.z * entrance.mainRoom.x
                                )).join()
                                while (DungeonTimer.dungeonStartTime != -1L) {
                                    for (packet in outboundRoomQueue) {
                                        WSClient.sendPacketAsync(packet)
                                        printDevMessage({ packet.toString() }, "dungeonws")
                                    }
                                    printDevMessage("escaped loop", "dungeonws")
                                }
                            }.also {
                                it.invokeOnCompletion {
                                    printDevMessage({ "loop exit $it" }, "dungeonws")
                                }
                            }
                        }
                    }
                } else {
                    witherDoorOpenedRegex.find(unformatted)?.destructured?.let { (name) ->
                        DungeonInfo.keys--
                    }

                    keyPickupRegex.find(text)?.destructured?.let { (num) ->
                        DungeonInfo.keys += num.toInt()
                    }
                }
            }
        }
        else if (event.packet is PlayerListS2CPacket) {
            val actions = event.packet.actions
            val entries = event.packet.entries

            if (PlayerListS2CPacket.Action.ADD_PLAYER !in actions && PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME !in actions) return

            for (entry in entries) {
                val text = entry.text
                if ('✦' in text || '✔' in text || '✖' in text) {
                    puzzleRegex.find(text)?.let { match ->
                        val puzzleName = match.groups["puzzle"]!!.value
                        if (puzzleName != "???") {
                            when {
                                match.groups["missing"] != null -> {
                                    printDevMessage({ "found missing puzzle $puzzleName" }, "dungeonlistener")
                                    if (puzzleName in terminalStatePuzzles) {
                                        postSync(DungeonPuzzleResetEvent(puzzleName))
                                        terminalStatePuzzles.remove(puzzleName)
                                        incompletePuzzles.add(puzzleName)
                                    }
                                    if (puzzleName !in incompletePuzzles) {
                                        postSync(DungeonPuzzleDiscoveredEvent(puzzleName))
                                        incompletePuzzles.add(puzzleName)
                                    }
                                }

                                match.groups["completed"] != null || match.groups["failed"] != null -> {
                                    printDevMessage({ "found completed/failed puzzle $puzzleName" }, "dungeonlistener")
                                    if (puzzleName in incompletePuzzles) {
                                        postSync(DungeonPuzzleCompletedEvent(puzzleName))
                                        terminalStatePuzzles.add(puzzleName)
                                        incompletePuzzles.remove(puzzleName)
                                    }
                                }
                            }
                        }
                    }
                    continue
                }

                val old = (mc.networkHandler!! as AccessorNetHandlerPlayClient).uuidToPlayerInfo[entry.profileId]

                val pos = playerEntryNames[old?.profile?.name ?: entry.profile!!.name]
                if (pos != null) {
                    val matcher = classPattern.find(text)
                    if (matcher == null) {
                        println("$text didn't match!")
                        // TODO: monitor how leaving the dungeon works
                        team.values.find { it.tabEntryIndex == pos }?.let {
                            println("Removing ${it.playerName} from team due to not matching ${DungeonTimer.dungeonStartTime}")
                            team.remove(it.playerName)
                        }
                        continue
                    }
                    val name = matcher.groups["name"]!!.value
                    if (name in disconnected) {
                        println("Skipping over entry $name due to player being disconnected")
                        continue
                    }

                    val dungeonClass = matcher.groups["class"]?.value?.let { DungeonClass.getClassFromName(it) }
                    val classLevel = matcher.groups["lvl"]?.value?.romanToDecimal()
                    val teammate = team.computeIfAbsent(name) {
                        DungeonTeammate(
                            name,
                            DungeonClass.EMPTY,
                            0,
                            pos,
                            old?.skinTextures?.texture ?: DefaultSkinHelper.getTexture()
                        ).also {
                            if (old == null) {
                                printDevMessage({ "could not get network player info for $name $actions" }, "dungeonlistener")
                                tickTimer(1) {
                                    printDevMessage({ "setting skin for ${name}" }, "dungeonlistener")
                                    it.skin = (mc.networkHandler!! as AccessorNetHandlerPlayClient).uuidToPlayerInfo[entry.profileId]?.skinTextures?.texture ?: DefaultSkinHelper.getTexture()
                                }
                            }
                            println("Added $it to list")
                        }
                    }
                    println("Processing update for $name")

                    if (dungeonClass != null && classLevel != null) {
                        teammate.dungeonClass = dungeonClass
                        teammate.classLevel = classLevel
                    }

                    if (teammate.tabEntryIndex != pos) {
                        println("Updating ${teammate.playerName} tab entry index from ${teammate.tabEntryIndex} to $pos")
                        team.values.find { it.tabEntryIndex == pos }?.let {
                            println("Removing $it from team due to tab entry index collision")
                            team.remove(it.playerName)
                        }
                        teammate.tabEntryIndex = pos
                    }

                    teammate.player = mc.world!!.players.find {
                        it.name.string == teammate.playerName && it.uuid.version() == 4
                    }

                    old?.skinTextures?.texture?.let { teammate.skin = it }

                    if ("§r§cDEAD§r§f)§r" in text) {
                        markDead(teammate)
                    } else {
                        markRevived(teammate)
                    }

                    tickTimer(1) {
                        val self = team[mc.player!!.name.string]
                        val alives = team.values.filterNot {
                            it.dead || it == self || it in deads
                        }.sortedBy {
                            it.tabEntryIndex
                        }

                        alives.forEachIndexed { i, teammate ->
                            teammate.mapPlayer.icon = "icon-$i"
                            printDevMessage({ "Setting icon for ${teammate.playerName} to icon-$i" }, "dungeonlistener")
                        }
                        self?.mapPlayer?.icon = "icon-${alives.size}"
                    }
                }
            }
        } else if (event.packet is EntitiesDestroyS2CPacket) {
            event.packet.entityIds.mapNotNull(mc.world!!::getEntityById).filter { it is PlayerEntity }.forEach { entity ->
                team[entity.name.string]?.player = null
            }
        }
    }

    fun onJoinWorld(event: EntityJoinWorldEvent) {
        // S0CPacketSpawnPlayer
        if (event.entity is AbstractClientPlayerEntity && event.entity.uuid.version() == 4) {
            team[event.entity.name.string]?.player = event.entity as PlayerEntity
        }
    }

    fun markDead(teammate: DungeonTeammate) {
        if (deads.add(teammate)) {
            val time = System.currentTimeMillis()
            val lastDeath = teammate.lastLivingStateChange
            if (lastDeath != null && time - lastDeath <= 1000) return
            teammate.lastLivingStateChange = time
            teammate.dead = true
            teammate.deaths++
        }
    }

    fun markRevived(teammate: DungeonTeammate) {
        if (deads.remove(teammate)) {
            teammate.dead = false
            teammate.lastLivingStateChange = System.currentTimeMillis()
        }
    }

    fun markAllRevived() {
        printDevMessage({ "${Skytils.prefix} §fdebug: marking all teammates as revived" }, "scorecalc")
        deads.clear()
        team.values.forEach {
            it.dead = false
        }
    }

    data class DungeonTeammate(
        val playerName: String,
        var dungeonClass: DungeonClass,
        var classLevel: Int,
        var tabEntryIndex: Int,
        var skin: Identifier
    ) {
        var player: PlayerEntity? = null
            set(value) {
                field = value
                if (value != null) {
                    mapPlayer.setData(value)
                }
            }
        var dead = false
        var deaths = 0
        var lastLivingStateChange: Long? = null

        val mapPlayer = DungeonMapPlayer(this)

        fun canRender() = player != null && player!!.health > 0 && !dead
    }

    override fun setup() {
        register(::onPacket)
        register(::onWorldLoad)
        register(::onJoinWorld)
    }
}
