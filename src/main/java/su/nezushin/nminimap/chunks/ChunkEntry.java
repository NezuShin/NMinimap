package su.nezushin.nminimap.chunks;

import org.bukkit.Location;
import org.bukkit.World;
import su.nezushin.nminimap.util.config.Config;

import java.io.File;

public record ChunkEntry(World w, int x, int z) {

    public File getAsFile() {
        return new File(Config.cacheFolder, w.getName() + "." + x + "." + z + ".bin.gz");
    }

    public boolean isInsideWorldBorder() {
        return w.getWorldBorder().isInside(new Location(w, x * 16, 0, z * 16));
    }
}

