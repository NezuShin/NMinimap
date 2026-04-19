package su.nezushin.nminimap.packets;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import su.nezushin.nminimap.NMinimap;
import su.nezushin.nminimap.packets.hooks.EntityHook;
import su.nezushin.nminimap.packets.hooks.PassengerHook;
import su.nezushin.nminimap.packets.hooks.impl.PacketEventsEntityHook;
import su.nezushin.nminimap.packets.hooks.impl.PacketEventsPassengerHook;
import su.nezushin.nminimap.packets.hooks.impl.PassengerAPIPassengerHook;
import su.nezushin.nminimap.util.SchedulerUtil;
import su.nezushin.nminimap.util.config.Config;
import su.nezushin.nminimap.util.VanillaMapUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PacketManager {

    private EntityHook entityHook;

    private PassengerHook passengerHook;


    private int markerEntityId, upItemFrameEntityId, downItemFrameEntityId, facingItemFrameEntityId;

    private int mapId;

    private ItemStack mapItem;

    private final Set<Player> trackedPlayers = ConcurrentHashMap.newKeySet();


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


        SchedulerUtil.getScheduler().async(this::tickTrackedPlayers, 1, 1);
        if (SchedulerUtil.getScheduler().isFolia())
            SchedulerUtil.getScheduler().async(this::foliaTickTrackedPlayers, 20, 20);

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

        trackedPlayers.add(p);
    }

    /**
     * Update map and markers for player
     * @param p
     * @param mapData
     * @param markers
     */
    public void updateMap(Player p, byte[] mapData, Component markers) {

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
        this.trackedPlayers.remove(p);
    }


    private final Map<Player, World> foliaWorldMap = new ConcurrentHashMap<>();

    /*
     PlayerTeleportEvent is broken on folia, this is ugly fix
     https://github.com/PaperMC/Folia/issues/330
      */
    private void foliaTickTrackedPlayers() {
        for (var p : trackedPlayers) {
            if (!p.getWorld().equals(foliaWorldMap.get(p))) {
                spawnEntities(p);
                foliaWorldMap.put(p, p.getWorld());
            }
        }
    }


    private void tickTrackedPlayers() {
        for (var p : trackedPlayers) {
            entityHook.teleportItemFrame(p, facingItemFrameEntityId);
        }
    }


}
