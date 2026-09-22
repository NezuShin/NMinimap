package su.nezushin.nminimap.chunks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import su.nezushin.nminimap.util.config.Config;
import su.nezushin.nminimap.util.config.UndergroundLayer;

import java.io.File;

public record ChunkEntry(String world, int x, int z, UndergroundLayer layer) {

    public File getAsFile() {
        String layerSuffix = layer != null ? "_layer_" + layer.id() : "";
        int settingsHash = layer != null ? layer.hashCode() : Config.waterRendering.hashCode();
        var namespace = new File(Config.cacheFolder, "render-v6-" + Integer.toUnsignedString(settingsHash, 16));
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
