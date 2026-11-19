package su.nezushin.nminimap.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import su.nezushin.nminimap.NMinimap;
import su.nezushin.nminimap.chunks.ChunkEntry;
import su.nezushin.nminimap.util.PerWorldSettingsUtil;
import su.nezushin.nminimap.util.config.Config;

public class ChunkListener implements Listener {

    @EventHandler
    public void chunkGenerate(ChunkLoadEvent e) {
        if (!Config.renderNewChunks)
            return;

        var chunk = e.getChunk();
        var chunkManager = NMinimap.getInstance().getChunkManager();
        var entry = new ChunkEntry(chunk.getWorld().getName(), chunk.getX(), chunk.getZ(), null);

        // Check synchronously before spawning an async task so neighbor-only loads don't race past unmark.
        if (chunkManager.isNeighborChunkLoading(entry)
                || !PerWorldSettingsUtil.getAllowFileCache(entry.world())
                || chunkManager.getChunkCache().hasInCache(entry)
                || !entry.isInsideWorldBorder())
            return;

        NMinimap.async(() -> chunkManager.getOrRenderChunk(entry));
    }

    @EventHandler
    public void worldUnload(WorldUnloadEvent e) {
        NMinimap.getInstance().getChunkManager().clearWorldTiles(e.getWorld().getName());
    }
}
