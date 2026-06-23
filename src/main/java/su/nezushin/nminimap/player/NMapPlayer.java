package su.nezushin.nminimap.player;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.NumberConversions;
import su.nezushin.anvil.orm.SqlFlag;
import su.nezushin.anvil.orm.SqlType;
import su.nezushin.anvil.orm.table.AnvilORMSerializable;
import su.nezushin.anvil.orm.table.SqlColumn;
import su.nezushin.nminimap.NMinimap;
import su.nezushin.nminimap.api.events.AsyncMapRenderEvent;
import su.nezushin.nminimap.api.events.AsyncMarkerRenderEvent;
import su.nezushin.nminimap.chunks.ChunkEntry;
import su.nezushin.nminimap.compatibility.WorldGuardManager;
import su.nezushin.nminimap.markers.impl.LocationMarker;
import su.nezushin.nminimap.util.config.Config;
import su.nezushin.nminimap.util.config.UndergroundLayer;
import su.nezushin.nminimap.util.ColorUtil;

import java.util.Arrays;

public class NMapPlayer implements AnvilORMSerializable {


    private Player player;

    private transient UndergroundLayer activeLayer;

    @SqlColumn(type = SqlType.VARCHAR, flags = SqlFlag.PRIMARY_KEY)
    private String id;
    @SqlColumn(type = SqlType.VARCHAR)
    private String name;

    @SqlColumn(type = SqlType.INT)
    private int scale = 1;
    @SqlColumn(type = SqlType.BOOLEAN)
    private boolean enabled = false, isRight, isRound;


    private int lastSentMapHash;
    private int lastSentMarkers;


    public NMapPlayer(Player player, boolean enabled) {
        this.setPlayer(player);
        this.enabled = enabled;
        NMinimap.getInstance().getPacketManager().spawnEntities(this.player);
    }

    public NMapPlayer() {
    }

    public void onQuit() {
        NMinimap.getInstance().getPacketManager().removeEntities(this.player);
        if (NMinimap.getInstance().isEnabled())
            NMinimap.getInstance().getModCompatibilityManager().resetModMinimap(this.player);
    }

    public void sendMap() {
        if (!enabled || !player.isValid() || player.isDead())
            return;
        NMinimap.async(() -> {
            var mapData = prepareMap();
            var hashCode = Arrays.hashCode(mapData);
            if (hashCode != lastSentMapHash) {
                lastSentMapHash = hashCode;
                NMinimap.getInstance().getPacketManager().sendMapData(player, mapData);
            }


            var markers = prepareMarkers();
            hashCode = markers.hashCode();
            if (hashCode != lastSentMarkers) {
                lastSentMarkers = hashCode;
                NMinimap.getInstance().getPacketManager().sendMarkerData(player, markers);
            }

        });
    }

    private byte[] prepareMap() {
        var chunkManager = NMinimap.getInstance().getChunkManager();

        var px = player.getLocation().getBlockX();
        var pz = player.getLocation().getBlockZ();
        var fullMapSize = 128;

        var offsetX = isRight ? 128 - Config.mapPixelSize : 0;
        var offsetZ = 0;

        var mapSizeX = isRight ? 128 : Config.mapPixelSize + 1;
        var mapSizeZ = Config.mapPixelSize + 1;

        var chunkSize = 16 / scale;
        var mapData = new byte[128 * 128];
        var world = player.getWorld();
        var worldName = world.getName();

        for (var x = offsetX + 1; x < mapSizeX; x++) {
            for (var z = offsetZ + 1; z < mapSizeZ; z++) {
                var wx = px + (x - mapSizeX / 2) * scale;
                var wz = pz + (z - mapSizeZ / 2) * scale;

                var cx = Math.floorDiv(wx, 16);
                var cz = Math.floorDiv(wz, 16);

                var localX = Math.floorMod(wx, 16);
                var localZ = Math.floorMod(wz, 16);

                var chunk = new ChunkEntry(worldName, cx, cz, this.activeLayer);
                var bytes = chunkManager.getOrRenderChunk(chunk).get(scale);

                chunkManager.getLastChunkUse().put(chunk, System.currentTimeMillis());

                var indexXX = Math.floorDiv(localX, scale);
                var indexZZ = Math.floorDiv(localZ, scale);

                var color = bytes != null ? bytes[indexXX + (indexZZ * chunkSize)] : 0;
                if (this.activeLayer != null) {
                    // Check if block outside WG layer region
                    if (!NMinimap.getInstance().getWorldGuardManager().isInsideLayer(new Location(world, wx, this.activeLayer.renderFromY(), wz), this.activeLayer)) {
                        // Load normal surface chunk for outside region
                        var normalChunk = new ChunkEntry(worldName, cx, cz, null);
                        var normalBytes = chunkManager.getOrRenderChunk(normalChunk).get(scale);
                        chunkManager.getLastChunkUse().put(normalChunk, System.currentTimeMillis());

                        var normalColor = normalBytes != null ? normalBytes[indexXX + (indexZZ * chunkSize)] : 0;
                        color = normalColor != 0 ? su.nezushin.nminimap.util.ColorUtil.darken(normalColor, this.activeLayer.darken()) : 0;
                    }
                }

                mapData[x + (z * fullMapSize)] = color;
            }
        }
        var event = new AsyncMapRenderEvent(this, mapData);

        Bukkit.getPluginManager().callEvent(event);

        mapData = event.getMapData();

        mapData[0] = 18;
        mapData[1] = 4;
        mapData[2] = 49;
        mapData[3] = (byte) (isRight ? (isRound ? 29 : -127) : (isRound ? 67 : 17));

        return mapData;
    }

    private Component prepareMarkers() {

        var builder = Component.text();
        var event = new AsyncMarkerRenderEvent(this);
        Bukkit.getPluginManager().callEvent(event);

        for (var marker : event.getMarkers()) {

            var pos = marker.getPositionOnMap(this);
            var markerX = pos[0];
            var markerZ = pos[1];
            var rotation = pos[2];

            if (
                    (isRound && NumberConversions.square(markerX) + NumberConversions.square(markerZ) < NumberConversions.square(Config.mapPixelSize))
                            ||
                            (!isRound && Math.abs(markerX) < Config.mapPixelSize && Math.abs(markerZ) < Config.mapPixelSize)) {
                builder.append(Component.text(NMinimap.getInstance().getMarkerImageManager().getMarkerIcon(marker.getIcon(), isRight, isRound)).font(Key.key("nminimap:default"))
                        .color(
                                isRound ?
                                        TextColor.color(markerX - 128, markerZ - 128, rotation)//
                                        :
                                        TextColor.color(markerX + (isRight ? -Config.mapPixelSize : Config.mapPixelSize + 2), markerZ + Config.mapPixelSize + 2, rotation)));
            }
        }

        return builder.asComponent();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (this.enabled) {
            NMinimap.getInstance().getPacketManager().spawnEntities(player);
        } else {
            NMinimap.getInstance().getPacketManager().removeEntities(player);
        }

        handleModMinimap();
        saveAsync();
    }

    public void handleModMinimap() {
        if (Config.disableModMapActivated || Config.disableModMapAlways) {
            var mods = NMinimap.getInstance().getModCompatibilityManager();
            if (enabled || Config.disableModMapAlways)
                mods.disableModMinimap(player);
            else
                mods.resetModMinimap(player);
        }
    }

    public void saveAsync() {
        NMinimap.getInstance().getDatabaseManager().getPlayersTable().update().replace(this);
    }

    public Player getPlayer() {
        return player;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setPlayer(Player player) {
        this.player = player;
        this.id = player.getUniqueId().toString();
        this.name = player.getName();
    }

    public int getScale() {
        return scale;
    }

    public void setScale(int scale) {
        this.scale = scale;
        saveAsync();
    }

    public boolean isRight() {
        return isRight;
    }

    public void setRight(boolean right) {
        isRight = right;
        saveAsync();
    }

    public boolean isRound() {
        return isRound;
    }

    public void setRound(boolean round) {
        isRound = round;
        saveAsync();
    }

    public void setActiveLayer(su.nezushin.nminimap.util.config.UndergroundLayer layer) {
        this.activeLayer = layer;
    }

    public su.nezushin.nminimap.util.config.UndergroundLayer getActiveLayer() {
        return this.activeLayer;
    }


}
