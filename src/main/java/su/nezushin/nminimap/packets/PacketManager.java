package su.nezushin.nminimap.packets;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import su.nezushin.nminimap.NMinimap;
import su.nezushin.nminimap.packets.hooks.EntityHook;
import su.nezushin.nminimap.packets.hooks.PassengerHook;
import su.nezushin.nminimap.packets.hooks.impl.PacketEventsEntityHook;
import su.nezushin.nminimap.packets.hooks.impl.PacketEventsPassengerHook;
import su.nezushin.nminimap.packets.hooks.impl.PassengerAPIPassengerHook;
import su.nezushin.nminimap.util.config.Config;
import su.nezushin.nminimap.util.VanillaMapUtil;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PacketManager {

    private EntityHook entityHook;

    private PassengerHook passengerHook;


    private int markerEntityId, upItemFrameEntityId, downItemFrameEntityId, facingItemFrameEntityId;

    private int mapId;

    private ItemStack mapItem;

    private Set<Player> trackedPlayers = Collections.synchronizedSet(new HashSet<>());
    private final Object trackedPlayersSync = new Object();

    private BukkitTask tracker;

    public PacketManager() {


        if (Bukkit.getPluginManager().getPlugin("packetevents") != null) {
            this.entityHook = new PacketEventsEntityHook();
            this.passengerHook = new PacketEventsPassengerHook();
        }

        if (Bukkit.getPluginManager().getPlugin("PassengerAPI") != null) {
            this.passengerHook = new PassengerAPIPassengerHook();
        }

        if (!isReady())
            return;


        this.mapId = Config.mapId;

        this.markerEntityId = Bukkit.getUnsafe().nextEntityId();
        this.upItemFrameEntityId = Bukkit.getUnsafe().nextEntityId();
        this.downItemFrameEntityId = Bukkit.getUnsafe().nextEntityId();
        this.facingItemFrameEntityId = Bukkit.getUnsafe().nextEntityId();

        mapItem = VanillaMapUtil.createItem(mapId);


        tracker = Bukkit.getScheduler().runTaskTimerAsynchronously(NMinimap.getInstance(), this::tickTrackedPlayers, 1, 1);
    }

    public boolean isReady() {
        return this.entityHook != null && this.passengerHook != null;
    }


    /**
     * Spawn item frames and marker for player
     *
     * @param p Player
     */
    public void spawnEntities(Player p) {
        entityHook.spawnItemFrame(p, upItemFrameEntityId, mapItem, false);
        entityHook.spawnItemFrame(p, downItemFrameEntityId, mapItem, true);
        entityHook.spawnItemFrame(p, facingItemFrameEntityId, mapItem, true);

        entityHook.spawnMarker(p, markerEntityId);

        passengerHook.updatePassengers(p, upItemFrameEntityId, downItemFrameEntityId, markerEntityId);


        synchronized (trackedPlayersSync) {
            trackedPlayers.add(p);
        }
    }

    /**
     * Update map and markers for player
     * @param p
     * @param mapData
     * @param markers
     */
    public void updateMap(Player p, byte[] mapData, Component markers){
        entityHook.sendMapData(p, mapId, 0, mapData);
        entityHook.sendMarkerData(p, markerEntityId, markers);
    }

    /**
     * Remove item frames and marker for player
     *
     * @param p Player
     */
    public void removeEntities(Player p) {
        entityHook.removeEntities(p, upItemFrameEntityId, downItemFrameEntityId, facingItemFrameEntityId, markerEntityId);
        synchronized (trackedPlayersSync) {
            this.trackedPlayers.remove(p);
        }
    }

    private void tickTrackedPlayers() {
        synchronized (trackedPlayersSync) {
            for (var p : trackedPlayers) {
                entityHook.teleportItemFrame(p, facingItemFrameEntityId);
            }
        }
    }

}
