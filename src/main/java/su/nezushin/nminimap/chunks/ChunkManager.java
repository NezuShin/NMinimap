package su.nezushin.nminimap.chunks;

import org.bukkit.Bukkit;
import su.nezushin.nminimap.NMinimap;
import su.nezushin.nminimap.chunks.cache.ChunkCache;
import su.nezushin.nminimap.chunks.renderer.ChunkRender;
import su.nezushin.nminimap.util.MapDataUtil;
import su.nezushin.nminimap.util.config.Config;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ChunkManager {


    private final Map<ChunkEntry, Map<Integer, byte[]>> loadedTiles = new ConcurrentHashMap<>();
    private final Map<ChunkEntry, Long> lastChunkUse = new ConcurrentHashMap<>();

    private final Set<ChunkEntry> loadingChunks = ConcurrentHashMap.newKeySet();
    private final Queue<ChunkEntry> awaitingChunks = new ConcurrentLinkedQueue<>();

    private final Map<Integer, byte[]> emptyMap = MapDataUtil.prepareEmptyMap();


    private ChunkCache chunkCache;
    private ChunkRender chunkRender;


    public ChunkManager() {
        this.chunkRender = new ChunkRender();
        this.chunkCache = new ChunkCache();

        Bukkit.getScheduler().runTaskTimerAsynchronously(NMinimap.getInstance(), () -> {
            removeOldChunks();
        }, 40, 40);
    }

    private void removeOldChunks() {
        var chunkCountToRemove = loadedTiles.size() - Config.maxTilesInRam;

        if (chunkCountToRemove < 1)
            return;

        var sortedEntries = new HashSet<>(lastChunkUse.entrySet()).stream()
                .filter(i -> System.currentTimeMillis() - i.getValue() > 15000)
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).toList();

        for (var i = 0; i < chunkCountToRemove && sortedEntries.size() > i; i++) {
            var entry = sortedEntries.get(i);

            loadedTiles.remove(entry.getKey());
            lastChunkUse.remove(entry.getKey());
            System.out.println("REMOVED CHUNK " + entry.getKey().x() +  " " + entry.getKey().z());
        }
    }

    public void renderNextAwaitingChunk() {
        while (loadingChunks.size() < Config.maxRenderThreads) {
            var chunk = awaitingChunks.poll();
            if (chunk == null)
                return;

            if (Config.allowFileCache && chunkCache.hasInCache(chunk))
                chunkCache.loadFromCache(chunk);
            else
                chunkRender.renderChunk(chunk);

        }
    }

    public Map<Integer, byte[]> getOrRenderChunk(ChunkEntry chunk) {
        if (loadedTiles.containsKey(chunk))
            return loadedTiles.get(chunk);
        if (!awaitingChunks.contains(chunk) && !loadingChunks.contains(chunk)) {
            awaitingChunks.add(chunk);
            loadedTiles.put(chunk, emptyMap);
            renderNextAwaitingChunk();
        }

        return emptyMap;
    }

    public void reRenderChunk(ChunkEntry chunk) {
        /*chunkCache.removeFromCache(chunk);
        getOrRenderChunk(chunk);*/
        NMinimap.async(() -> {
            chunkCache.removeFromCache(chunk);
            //loadedTiles.remove(chunk);

            lastChunkUse.remove(chunk);
            awaitingChunks.add(chunk);
            renderNextAwaitingChunk();
        });
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


}
