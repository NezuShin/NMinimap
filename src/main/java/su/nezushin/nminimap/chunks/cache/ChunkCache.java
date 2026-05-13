package su.nezushin.nminimap.chunks.cache;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import su.nezushin.nminimap.NMinimap;
import su.nezushin.nminimap.chunks.ChunkEntry;
import su.nezushin.nminimap.util.DiskCapacityUtil;
import su.nezushin.nminimap.util.MapDataUtil;
import su.nezushin.nminimap.util.SchedulerUtil;
import su.nezushin.nminimap.util.config.Config;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ChunkCache {

    private Set<ChunkEntry> cachedFiles = ConcurrentHashMap.newKeySet();


    private boolean isDiskFull;

    public ChunkCache() {
        if (!Config.allowFileCache)
            return;
        loadCachedFiles();
    }

    public void loadCachedFiles() {
        var deleted = 0;
        NMinimap.getInstance().getLogger().info("Loading cache...");
        var reportTask = SchedulerUtil.getScheduler().async(() -> reportCacheLoadingStatus(), 40, 40);
        for (var file : Config.cacheFolder.listFiles()) {
            String[] name = file.getName().split("\\.");
            if (name[3].equalsIgnoreCase("json")) {
                file.delete();//old cache clear
                deleted++;
                continue;
            }
            if (!file.getName().endsWith(".bin.gz"))
                continue;
            cachedFiles.add(new ChunkEntry(Bukkit.getWorld(name[0]), Integer.parseInt(name[1]), Integer.parseInt(name[2])));
        }
        reportTask.cancel();
        NMinimap.getInstance().getLogger().info("Cache init done! Loaded " + cachedFiles.size() + " tiles. Deleted " + deleted + " old cache files.");
    }

    private void reportCacheLoadingStatus() {
        NMinimap.getInstance().getLogger().info("Loaded " + cachedFiles.size() + " tiles....");

    }

    public void removeFromCache(ChunkEntry chunk) {
        cachedFiles.remove(chunk);
        var file = chunk.getAsFile();

        if (file.exists())
            file.delete();
    }

    public boolean hasInCache(ChunkEntry chunk) {
        return cachedFiles.contains(chunk);
    }

    public void loadFromCache(ChunkEntry chunk) {
        var chunkManager = NMinimap.getInstance().getChunkManager();
        chunkManager.getLoadingChunks().add(chunk);

        NMinimap.async(() -> {
            var file = chunk.getAsFile();
            if (!file.exists()) {
                cachedFiles.remove(chunk);
                NMinimap.getInstance().getChunkManager().getLoadingChunks().remove(chunk);
                return;
            }

            try (var is = new GZIPInputStream(new FileInputStream(file))) {
                var scales = MapDataUtil.readMap(is);

                chunkManager.getLoadedTiles().put(chunk, scales);
                chunkManager.getLoadingChunks().remove(chunk);
                chunkManager.renderNextAwaitingChunk();
            } catch (Exception ex) {
                cachedFiles.remove(chunk);
                NMinimap.getInstance().getChunkManager().getLoadingChunks().remove(chunk);
                NMinimap.getInstance().getLogger().log(Level.SEVERE, "Failed to load chunk tile from cache", ex);
            }
        });
    }

    public void saveToCache(ChunkEntry chunk, Map<Integer, byte[]> scales) {
        if (!Config.allowFileCache)
            return;
        if (DiskCapacityUtil.getUsableSpace() < Config.availableDiskSpaceThreshold) {
            isDiskFull = true;
            return;
        }
        isDiskFull = false;

        try (var os = new GZIPOutputStream(new FileOutputStream(chunk.getAsFile()))) {
            MapDataUtil.saveMap(scales, os);

            cachedFiles.add(chunk);
        } catch (Exception ex) {
            cachedFiles.remove(chunk);
            NMinimap.getInstance().getLogger().log(Level.SEVERE, "Failed to save chunk tile to cache", ex);
        }
    }

    public Set<ChunkEntry> getCachedFiles() {
        return cachedFiles;
    }

    public boolean isDiskFull() {
        return isDiskFull;
    }
}
