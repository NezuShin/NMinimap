package su.nezushin.nminimap.chunks;

import org.bukkit.Location;
import org.bukkit.World;
import su.nezushin.nminimap.util.config.Config;
import su.nezushin.nminimap.util.config.UndergroundLayer;

import java.io.File;

public record ChunkEntry(World w, int x, int z, UndergroundLayer layer) {

    public ChunkEntry(World w, int x, int z) {
        this(w, x, z, null);
    }

    public File getAsFile() {
        String layerSuffix = layer != null ? "_layer_" + layer.id() : "";
        return new File(Config.cacheFolder, w.getName() + "." + x + "." + z + layerSuffix + ".bin.gz");
    }

    public boolean isInsideWorldBorder() {
        return w.getWorldBorder().isInside(new Location(w, x * 16, 0, z * 16));
    }
}
