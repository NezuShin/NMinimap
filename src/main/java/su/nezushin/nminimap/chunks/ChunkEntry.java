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
        return new File(Config.cacheFolder, world + "." + x + "." + z + layerSuffix + ".bin.gz");
    }

    public World getWorld() {//fix for cases where world is not loaded when cache is already there
        return Bukkit.getWorld(world);
    }
    public boolean isInsideWorldBorder() {
        return getWorld().getWorldBorder().isInside(new Location(getWorld(), x * 16, 0, z * 16));
    }
}
