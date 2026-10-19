package su.nezushin.nminimap.player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
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
import su.nezushin.nminimap.api.events.AsyncFrameRenderEvent;
import su.nezushin.nminimap.api.events.AsyncMapRenderEvent;
import su.nezushin.nminimap.api.events.AsyncMarkerRenderEvent;
import su.nezushin.nminimap.chunks.ChunkEntry;
import su.nezushin.nminimap.chunks.ChunkManager;
import su.nezushin.nminimap.chunks.LayerData;
import su.nezushin.nminimap.frames.FrameLayer;
import su.nezushin.nminimap.resourcepack.ResourcepackManager;
import su.nezushin.nminimap.util.ColorUtil;
import su.nezushin.nminimap.util.DisallowedWorldsUtil;
import su.nezushin.nminimap.util.config.Config;
import su.nezushin.nminimap.util.config.Permission;
import su.nezushin.nminimap.util.config.UndergroundLayer;

import java.util.ArrayList;
import java.util.Arrays;

public class NMapPlayer implements AnvilORMSerializable {

    private static final int[][] NEIGHBOURS = {{0, -1}, {0, 1}, {1, 0}, {-1, 0}};


    private Player player;

    private transient UndergroundLayer activeLayer;

    private transient volatile PlayerCavity lastCavity;

    @SqlColumn(type = SqlType.VARCHAR, flags = SqlFlag.PRIMARY_KEY)
    private String id;
    @SqlColumn(type = SqlType.VARCHAR)
    private String name;

    @SqlColumn(type = SqlType.INT)
    private int scale = 1;
    @SqlColumn(type = SqlType.BOOLEAN)
    private boolean enabled = false, isRight, isRound, radarEnabled = true;

    @SqlColumn(type = SqlType.VARCHAR)
    private String frame;


    private int lastSentMapHash;
    private int lastSentMarkersHash;
    private int lastWorldHash;

    private boolean worldAllowed, entitiesSpawned;
    private boolean isBedrockPlayer;


    public NMapPlayer(Player player, boolean enabled) {
        this.setPlayer(player);
        this.enabled = enabled;
        checkWorldAllowed();
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

        if (!checkWorldAllowed())
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
            if (hashCode != lastSentMarkersHash) {
                lastSentMarkersHash = hashCode;
                NMinimap.getInstance().getPacketManager().sendMarkerData(player, markers);
            }

        });
    }

    //Spawn/remove item frame and text display for player
    public void respawnEntities(boolean force) {
        if (entitiesSpawned && (!enabled || !worldAllowed)) {
            NMinimap.getInstance().getPacketManager().removeEntities(player);
            entitiesSpawned = false;
        } else if ((!entitiesSpawned || force) && enabled && worldAllowed) {
            NMinimap.getInstance().getPacketManager().spawnEntities(player);
            entitiesSpawned = true;
        }
    }

    private boolean checkWorldAllowed() {
        if (player.getWorld().getName().hashCode() == lastWorldHash)
            return worldAllowed;

        worldAllowed = DisallowedWorldsUtil.isAllowed(player.getWorld());
        lastWorldHash = player.getWorld().getName().hashCode();
        respawnEntities(false);
        return worldAllowed;
    }

    private byte[] prepareMap() {
        var chunkManager = NMinimap.getInstance().getChunkManager();

        var px = player.getLocation().getBlockX();
        var pz = player.getLocation().getBlockZ();
        var fullMapSize = 128;

        var mapSize = Config.mapPixelSize + 1;

        // Capture once — this.scale can change concurrently via setScale()
        final int scale = normalizeScale(this.scale);
        var chunkSize = 16 / scale;
        var mapData = new byte[128 * 128];
        var world = player.getWorld();
        var worldName = world.getName();
        final int playerY = player.getLocation().getBlockY();
        final var layer = this.activeLayer;

        final var cavity = findPlayerCavity(chunkManager, worldName, px, playerY, pz, layer);
        final boolean underground = !cavity.sky();

        for (var x = 1; x < mapSize; x++) {
            int northY = LayerData.NO_LAYER;
            // Row 0 is not drawn, it only seeds the hillshade of row 1
            for (var z = 0; z < mapSize; z++) {
                var wx = px + (x - mapSize / 2) * scale;
                var wz = pz + (z - mapSize / 2) * scale;

                var cx = Math.floorDiv(wx, 16);
                var cz = Math.floorDiv(wz, 16);

                var localX = Math.floorMod(wx, 16);
                var localZ = Math.floorMod(wz, 16);

                // Blocks outside the WG layer region are drawn from the normal surface chunk
                boolean outsideLayer = layer != null && !NMinimap.getInstance().getWorldGuardManager()
                        .isInsideLayer(new Location(world, wx, layer.renderFromY(), wz), layer);

                var chunk = new ChunkEntry(worldName, cx, cz, outsideLayer ? null : layer);
                var tile = chunkManager.getOrRenderChunk(chunk);

                chunkManager.getLastChunkUse().put(chunk, System.currentTimeMillis());

                var indexXX = Math.floorDiv(localX, scale);
                var indexZZ = Math.floorDiv(localZ, scale);

                int layerIndex = 0;
                boolean dim = false;
                if (underground && !outsideLayer) {
                    layerIndex = tile.containingIndex(scale, indexXX, indexZZ, chunkSize, playerY);
                    if (layerIndex < 0) {
                        // Inside solid blocks: show the floor above, dimmed if it's hidden above the player's ceiling
                        layerIndex = tile.aboveIndex(scale, indexXX, indexZZ, chunkSize, playerY);
                        if (layerIndex >= 0)
                            dim = tile.floorY(layerIndex, scale, indexXX, indexZZ, chunkSize) > cavity.topY();
                        else
                            layerIndex = 0;
                    }
                }

                int drawY = tile.drawY(layerIndex, scale, indexXX, indexZZ, chunkSize);
                if (z > 0) {
                    var color = tile.colorAt(layerIndex, scale, indexXX, indexZZ, chunkSize);
                    color = ColorUtil.hillshade(color, drawY, northY, scale, (indexXX + indexZZ) & 1);
                    if (dim)
                        color = ColorUtil.darken(color, Config.multiLayerDarken);
                    if (outsideLayer)
                        color = ColorUtil.darken(color, layer.darken());

                    mapData[x + (z * fullMapSize)] = color;
                }
                northY = drawY;
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
        var markerManager = NMinimap.getInstance().getMarkerManager();
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
                var icon = markerManager.getMarkerIcon(marker.getIcon(), isRight, isRound);
                if (icon == null)
                    continue;
                builder.append(Component.text(icon).font(ResourcepackManager.FONT)
                        .color(
                                isRound ?
                                        TextColor.color(markerX - 128, markerZ - 128, rotation)//
                                        :
                                        TextColor.color(markerX + Config.mapPixelSize + 2, markerZ + Config.mapPixelSize + 2, rotation)));
            }
        }

        appendFrame(builder);

        return builder.asComponent();
    }

    private void appendFrame(TextComponent.Builder builder) {
        if (frame == null)
            return;

        var definition = NMinimap.getInstance().getFrameManager().getFrame(frame);
        if (definition == null)
            return;

        var layers = definition.layers(isRound);

        //Layers are shared between players, so they are only handed to listeners as copies
        if (AsyncFrameRenderEvent.getHandlerList().getRegisteredListeners().length == 0) {
            for (var i = 0; i < layers.size(); i++)
                appendLayer(builder, layers.get(i));
            return;
        }

        var copies = new ArrayList<FrameLayer>(layers.size());
        for (var i = 0; i < layers.size(); i++)
            copies.add(layers.get(i).copy());

        var event = new AsyncFrameRenderEvent(this, copies);
        Bukkit.getPluginManager().callEvent(event);

        for (var layer : event.getLayers())
            if (layer != null)
                appendLayer(builder, layer);
    }

    private void appendLayer(TextComponent.Builder builder, FrameLayer layer) {
        builder.append(Component.text(layer.symbol(isRight)).font(ResourcepackManager.FONT)
                .color(TextColor.color(layer.getZIndex(), 0, layer.rotationChannel())));
    }

    public void setEnabled(boolean enabled) {
        this.isBedrockPlayer = NMinimap.getInstance().getGeyserManager().isBedrockPlayer(player);
        if (this.isBedrockPlayer && !Permission.bedrock_bypass.has(player)) {
            enabled = false;
        }
        this.enabled = enabled;

        respawnEntities(false);
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

    public boolean isBedrockPlayer() {
        return isBedrockPlayer;
    }

    public void setPlayer(Player player) {
        this.player = player;
        this.id = player.getUniqueId().toString();
        this.name = player.getName();
    }

    public int getScale() {
        return normalizeScale(scale);
    }

    public void setScale(int scale) {
        this.scale = normalizeScale(scale);
        saveAsync();
    }

    /** Supported zoom levels only (must divide 16). Invalid DB/config values fall back to 1. */
    private static int normalizeScale(int scale) {
        return scale == 2 || scale == 4 || scale == 8 ? scale : 1;
    }

    /**
     * Open space the player is in, from their own column at scale 1. Falls back to the neighbouring columns, then to
     * the last frame (also while the player's tile isn't rendered yet), then to an enclosed space at the player's Y.
     */
    private PlayerCavity findPlayerCavity(ChunkManager chunkManager, String worldName, int px, int py, int pz,
                                          UndergroundLayer layer) {
        PlayerCavity cavity = null;
        var tile = chunkManager.getOrRenderChunk(new ChunkEntry(worldName, Math.floorDiv(px, 16), Math.floorDiv(pz, 16), layer));
        if (tile != chunkManager.getEmptyTile()) {
            cavity = cavityAt(chunkManager, worldName, px, py, pz, layer);
            for (var i = 0; cavity == null && i < NEIGHBOURS.length; i++)
                cavity = cavityAt(chunkManager, worldName, px + NEIGHBOURS[i][0], py, pz + NEIGHBOURS[i][1], layer);
        }

        var last = lastCavity;
        if (cavity == null && last != null && last.world().equals(worldName))
            return last;
        if (cavity == null)
            return new PlayerCavity(worldName, py, py + 1);

        lastCavity = cavity;
        return cavity;
    }

    private static PlayerCavity cavityAt(ChunkManager chunkManager, String worldName, int x, int y, int z,
                                         UndergroundLayer layer) {
        var tile = chunkManager.getOrRenderChunk(new ChunkEntry(worldName, Math.floorDiv(x, 16), Math.floorDiv(z, 16), layer));
        var localX = Math.floorMod(x, 16);
        var localZ = Math.floorMod(z, 16);
        int layerIndex = tile.containingIndex(1, localX, localZ, 16, y);
        if (layerIndex < 0)
            return null;
        return new PlayerCavity(worldName, tile.floorY(layerIndex, 1, localX, localZ, 16),
                tile.topY(layerIndex, 1, localX, localZ, 16));
    }

    /** {@code topY} is {@link LayerData#SKY} when nothing solid is above the player. */
    private record PlayerCavity(String world, int floorY, int topY) {

        boolean sky() {
            return topY == LayerData.SKY;
        }
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

    public boolean isRadarEnabled() {
        return radarEnabled;
    }

    public void setRadarEnabled(boolean radarEnabled) {
        this.radarEnabled = radarEnabled;
        saveAsync();
    }

    public String getFrame() {
        return frame;
    }

    public void setFrame(String frame) {
        this.frame = frame;
        saveAsync();
    }

    public void setActiveLayer(su.nezushin.nminimap.util.config.UndergroundLayer layer) {
        this.activeLayer = layer;
    }

    public su.nezushin.nminimap.util.config.UndergroundLayer getActiveLayer() {
        return this.activeLayer;
    }


}
