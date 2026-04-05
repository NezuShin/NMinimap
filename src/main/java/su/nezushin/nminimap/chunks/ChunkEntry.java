package su.nezushin.nminimap.chunks;

import org.bukkit.World;
import su.nezushin.nminimap.util.config.Config;

import java.io.File;

public record ChunkEntry(World w, int x, int z) {

    public File getAsFile() {
        return new File(Config.cacheFolder, w.getName() + "." + x + "." + z + ".json");
    }
}

