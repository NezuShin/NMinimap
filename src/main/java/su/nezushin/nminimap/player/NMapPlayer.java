package su.nezushin.nminimap.player;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import su.nezushin.anvil.orm.SqlFlag;
import su.nezushin.anvil.orm.SqlType;
import su.nezushin.anvil.orm.table.AnvilORMSerializable;
import su.nezushin.anvil.orm.table.SqlColumn;
import su.nezushin.nminimap.NMinimap;
import su.nezushin.nminimap.api.events.AsyncMapRenderEvent;
import su.nezushin.nminimap.api.events.AsyncMarkerRenderEvent;
import su.nezushin.nminimap.chunks.ChunkEntry;
import su.nezushin.nminimap.util.config.Config;

public class NMapPlayer implements AnvilORMSerializable {


    private Player player;

    @SqlColumn(type = SqlType.VARCHAR, flags = SqlFlag.PRIMARY_KEY)
    private String id;
    @SqlColumn(type = SqlType.VARCHAR)
    private String name;

    @SqlColumn(type = SqlType.INT)
    private int scale = 1;
    @SqlColumn(type = SqlType.BOOLEAN)
    private boolean enabled = false, isRight, isRound;


    public NMapPlayer(Player player, boolean enabled) {
        this.setPlayer(player);
        this.enabled = enabled;
        NMinimap.getInstance().getPacketManager().spawnEntities(this.player);
    }

    public NMapPlayer() {
    }

    public void onQuit() {
        NMinimap.getInstance().getPacketManager().removeEntities(this.player);
        NMinimap.getInstance().getModCompatibilityManager().resetModMinimap(this.player);
    }

    public void sendMap() {
        if (!enabled)
            return;
        NMinimap.async(() -> {
            NMinimap.getInstance().getPacketManager().updateMap(player, prepareMap(), prepareMarkers());
        });
    }

    private byte[] prepareMap() {
        var chunkManager = NMinimap.getInstance().getChunkManager();

        var px = player.getLocation().getBlockX();
        var pz = player.getLocation().getBlockZ();
        var mapSize = 128;
        var chunkSize = 16 / scale;
        var mapData = new byte[128 * 128];
        var world = player.getWorld();

        for (var x = 1; x < mapSize; x++) {
            for (var z = 1; z < mapSize; z++) {
                var wx = px + (x - mapSize / 2) * scale;
                var wz = pz + (z - mapSize / 2) * scale;

                var cx = Math.floorDiv(wx, 16);
                var cz = Math.floorDiv(wz, 16);

                var localX = Math.floorMod(wx, 16);
                var localZ = Math.floorMod(wz, 16);

                var chunk = new ChunkEntry(world, cx, cz);
                var bytes = chunkManager.getOrRenderChunk(chunk).get(scale);

                chunkManager.getLastChunkUse().put(new ChunkEntry(world, cx, cz), System.currentTimeMillis());

                var indexXX = Math.floorDiv(localX, scale);
                var indexZZ = Math.floorDiv(localZ, scale);

                mapData[x + (z * mapSize)] = bytes[indexXX + (indexZZ * chunkSize)];
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

            if (Math.abs(markerX) < 128 && Math.abs(markerZ) < 128) {
                builder.append(Component.text(NMinimap.getInstance().getMarkerImageManager().getMarkerIcon(marker.getIcon(), isRight, isRound)).font(Key.key("nminimap:default")).color(TextColor.color(markerX - 128, markerZ - 128, rotation)));
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


}
