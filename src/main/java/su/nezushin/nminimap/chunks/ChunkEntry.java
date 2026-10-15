package su.nezushin.nminimap.chunks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import su.nezushin.nminimap.util.config.Config;
import su.nezushin.nminimap.util.config.UndergroundLayer;
import su.nezushin.nminimap.chunks.cache.CacheFingerprint;

import java.io.File;

public record ChunkEntry(String world, int x, int z, UndergroundLayer layer, boolean surface) {

    public ChunkEntry(String world, int x, int z, UndergroundLayer layer) {
        this(world, x, z, layer, false);
    }

    public ChunkEntry {
        if (surface && layer == null)
            throw new IllegalArgumentException("A surface tile override requires an underground layer");
    }

    public File getAsFile() {
        String layerSuffix = layer == null ? "" : surface ? "_surface_layer_" + layer.id() : "_layer_" + layer.id();
        var namespace = new File(Config.cacheFolder, CacheFingerprint.namespace(this));
        return new File(namespace, world + "." + x + "." + z + layerSuffix + ".bin.gz");
    }

    public World getWorld() {//fix for cases where world is not loaded when cache is already there
        return Bukkit.getWorld(world);
    }

    public boolean isInsideWorldBorder() {
        var world = getWorld();
        return world != null && world.getWorldBorder().isInside(new Location(world, x * 16, 0, z * 16));
    }
}
