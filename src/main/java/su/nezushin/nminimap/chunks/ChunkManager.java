package su.nezushin.nminimap.chunks;

import su.nezushin.nminimap.NMinimap;
import su.nezushin.nminimap.chunks.cache.ChunkCache;
import su.nezushin.nminimap.chunks.renderer.ChunkRender;
import su.nezushin.nminimap.util.MapDataUtil;
import su.nezushin.nminimap.util.PerWorldSettingsUtil;
import su.nezushin.nminimap.util.RamCapacityUtil;
import su.nezushin.nminimap.util.SchedulerUtil;
import su.nezushin.nminimap.util.config.Config;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ChunkManager {


    private final Map<ChunkEntry, Map<Integer, byte[]>> loadedTiles = new ConcurrentHashMap<>();
    private final Map<ChunkEntry, Long> lastChunkUse = new ConcurrentHashMap<>();

    private final Set<ChunkEntry> loadingChunks = ConcurrentHashMap.newKeySet();
    /** Chunks currently sitting in {@link #awaitingChunks} (dedupe for the queue). */
    private final Set<ChunkEntry> pendingChunks = ConcurrentHashMap.newKeySet();
    /**
     * Needs another pass. Set before scheduling; cleared when a job starts so
     * dirties that arrive mid-job are preserved for {@link #finishChunkJob}.
     */
    private final Set<ChunkEntry> dirtyChunks = ConcurrentHashMap.newKeySet();
    private final Queue<ChunkEntry> awaitingChunks = new ConcurrentLinkedQueue<>();

    private final Map<Integer, byte[]> emptyMap = MapDataUtil.prepareEmptyMap();

    private boolean ramLow;


    private ChunkCache chunkCache;
    private ChunkRender chunkRender;


    public ChunkManager() {
        this.chunkRender = new ChunkRender();
        this.chunkCache = new ChunkCache();

        SchedulerUtil.getScheduler().async(this::removeOldChunks, 40, 40);
    }

    private void removeOldChunks() {
        long now = System.currentTimeMillis();
        var sortedEntries = new HashSet<>(loadedTiles.keySet()).stream()
                .filter(chunk -> now - lastChunkUse.getOrDefault(chunk, 0L) > 15000)
                .toList();

        for (var i = 0; sortedEntries.size() > i; i++) {
            var chunk = sortedEntries.get(i);

            loadedTiles.remove(chunk);
            lastChunkUse.remove(chunk);
        }

        renderNextAwaitingChunk();
    }

    public void clearWorldTiles(String world) {
        for (var i : loadedTiles.keySet().stream().filter(i -> i.world().equalsIgnoreCase(world)).toList()) {
            loadedTiles.remove(i);
            lastChunkUse.remove(i);
        }

        pendingChunks.removeIf(i -> i.world().equalsIgnoreCase(world));
        dirtyChunks.removeIf(i -> i.world().equalsIgnoreCase(world));
        awaitingChunks.removeIf(i -> i.world().equalsIgnoreCase(world));
    }

    /**
     * Enqueue a chunk at most once. Returns true if it was newly queued.
     */
    public boolean enqueueChunk(ChunkEntry chunk) {
        if (!pendingChunks.add(chunk))
            return false;//already in render queue.
        awaitingChunks.add(chunk);
        return true;
    }

    /**
     * Called when load/render finishes. If the chunk was dirtied meanwhile, re-queue it once.
     */
    public void finishChunkJob(ChunkEntry chunk) {
        loadingChunks.remove(chunk);
        if (dirtyChunks.contains(chunk))
            enqueueChunk(chunk);
        renderNextAwaitingChunk();
    }

    public void markDirty(ChunkEntry chunk) {
        dirtyChunks.add(chunk);
    }

    public void renderNextAwaitingChunk() {
        if (RamCapacityUtil.getAvailableMemory() < Config.availableRamThreshold) {
            ramLow = true;
            return;
        }
        ramLow = false;

        while (loadingChunks.size() < Config.maxRenderThreads) {
            var chunk = awaitingChunks.poll();
            if (chunk == null)
                return;

            pendingChunks.remove(chunk);

            // Duplicate queue entries / race: never start a second job for the same chunk.
            if (!loadingChunks.add(chunk))
                continue;

            // Accept this pass; any dirty set after this point triggers another via finishChunkJob.
            dirtyChunks.remove(chunk);

            if (PerWorldSettingsUtil.getAllowFileCache(chunk.world()) && chunkCache.hasInCache(chunk))
                chunkCache.loadFromCache(chunk);
            else
                chunkRender.renderChunk(chunk);

        }
    }

    public Map<Integer, byte[]> getOrRenderChunk(ChunkEntry chunk) {
        if (!chunk.isInsideWorldBorder())
            return emptyMap;

        Map<Integer, byte[]> tiles = loadedTiles.get(chunk);
        if (tiles != null)
            return tiles;

        if (loadingChunks.contains(chunk) || pendingChunks.contains(chunk))
            return emptyMap;

        if (enqueueChunk(chunk))
            renderNextAwaitingChunk();

        return emptyMap;
    }

    public void reRenderChunk(ChunkEntry chunk) {
        NMinimap.async(() -> {
            chunkCache.removeFromCache(chunk);
            // Always mark dirty + try enqueue. If a job is in flight, finishChunkJob
            // will re-enqueue via dirty; pendingChunks dedupes double enqueue.
            dirtyChunks.add(chunk);
            enqueueChunk(chunk);
            renderNextAwaitingChunk();
        });
    }

    public int getAwaitingChunksSize() {
        return awaitingChunks.size();
    }

    public Set<ChunkEntry> getLoadingChunks() {
        return loadingChunks;
    }

    public ChunkCache getChunkCache() {
        return chunkCache;
    }

    public Map<ChunkEntry, Map<Integer, byte[]>> getLoadedTiles() {
        return loadedTiles;
    }

    public Map<ChunkEntry, Long> getLastChunkUse() {
        return lastChunkUse;
    }

    public Map<Integer, byte[]> getEmptyMap() {
        return emptyMap;
    }

    public boolean isRamLow() {
        return ramLow;
    }


}
