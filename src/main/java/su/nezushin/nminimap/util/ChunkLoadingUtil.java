package su.nezushin.nminimap.util;

import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.World;
import su.nezushin.nminimap.NMinimap;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

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


    /**
     * Loads a chunk and gives back a way to get its block snapshot. Call {@link Supplier#get()} on the render thread.
     * <p>
     * Paper allows reading chunk data from any thread, so the snapshot is taken when {@code get()} is called,
     * off the main thread. Spigot stops the server when chunk data is read off the main thread
     * ("Accessing PalettedContainer from multiple threads"), so there the snapshot is taken on the main
     * thread right after loading, and {@code get()} just returns it.
     */
    public static CompletableFuture<Supplier<ChunkSnapshot>> getChunkSnapshotAt(World w, int x, int z) {

        if (isPaper) {
            return w.getChunkAtAsync(x, z).thenApply(chunk -> () -> takeSnapshot(chunk));
        }

        CompletableFuture<Supplier<ChunkSnapshot>> future = new CompletableFuture<>();
        NMinimap.sync(() -> {
            try {
                var snapshot = takeSnapshot(w.getChunkAt(x, z));
                future.complete(() -> snapshot);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });

        return future;
    }

    private static ChunkSnapshot takeSnapshot(Chunk chunk) {
        return chunk.getChunkSnapshot(true, false, false);
    }
}
