package su.nezushin.nminimap.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import su.nezushin.nminimap.NMinimap;
import su.nezushin.nminimap.chunks.ChunkEntry;
import su.nezushin.nminimap.util.config.Config;

public class ChunkListener implements Listener {

    @EventHandler
    public void chunkGenerate(ChunkLoadEvent e) {
        if (!Config.renderNewChunks)
            return;

        var chunk = e.getChunk();
        NMinimap.async(() -> {
            var chunkManager = NMinimap.getInstance().getChunkManager();
            var entry = new ChunkEntry(chunk.getWorld(), chunk.getX(), chunk.getZ());

            if (!chunkManager.getChunkCache().hasInCache(entry))
                NMinimap.getInstance().getChunkManager().getOrRenderChunk(entry);
        });
    }
}
