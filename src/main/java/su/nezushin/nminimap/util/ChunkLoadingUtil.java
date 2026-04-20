package su.nezushin.nminimap.util;

import org.bukkit.Chunk;
import org.bukkit.World;
import su.nezushin.nminimap.NMinimap;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class ChunkLoadingUtil {


    private static boolean isPaper;


    public static boolean isPaper() {
        return isPaper;
    }

    static {
        try {
            Class.forName("com.destroystokyo.paper.utils.PaperPluginLogger");
            isPaper = true;
        } catch (Exception ex) {
            isPaper = false;
        }
    }


    public static CompletableFuture<Chunk> getChunkAt(World w, int x, int z) {

        if (isPaper) {
            return w.getChunkAtAsync(x, z);
        }

        CompletableFuture<Chunk> future = new CompletableFuture<>();
        NMinimap.sync(() -> {
            future.complete(w.getChunkAt(x, z));
        });

        return future;
    }
}
