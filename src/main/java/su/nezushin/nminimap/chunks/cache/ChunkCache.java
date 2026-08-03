package su.nezushin.nminimap.chunks.cache;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import su.nezushin.nminimap.NMinimap;
import su.nezushin.nminimap.chunks.ChunkEntry;
import su.nezushin.nminimap.util.DiskCapacityUtil;
import su.nezushin.nminimap.util.MapDataUtil;
import su.nezushin.nminimap.util.SchedulerUtil;
import su.nezushin.nminimap.util.config.Config;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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
        if (Config.cacheLoadDelay <= 0)
            loadCachedFiles();
        else
            SchedulerUtil.getScheduler().async(this::loadCachedFiles, Config.cacheLoadDelay);
    }

    public void loadCachedFiles() {
        var deletedOld = 0;
        var deletedInvalidWorlds = 0;
        NMinimap.getInstance().getLogger().info("Loading cache...");
        var reportTask = SchedulerUtil.getScheduler().async(this::reportCacheLoadingStatus, 40, 40);
        this.cachedFiles.clear();
        try (var stream = Files.newDirectoryStream(Config.cacheFolder.toPath())) {
            for (var path : stream) {
                var file = path.toFile();
                String[] name = file.getName().split("\\.");
                if (file.getName().endsWith(".json")) {
                    file.delete();//old cache clear
                    deletedOld++;
                    continue;
                }
                if (file.getName().contains(".tmp.")) {
                    // Broken files
                    file.delete();
                    continue;
                }
                if (!file.getName().endsWith(".bin.gz"))
                    continue;

                int z;
                su.nezushin.nminimap.util.config.UndergroundLayer layer = null;
                int layerIndex = name[2].indexOf("_layer_");
                if (layerIndex != -1) {
                    z = Integer.parseInt(name[2].substring(0, layerIndex));
                    String layerId = name[2].substring(layerIndex + "_layer_".length());
                    layer = Config.undergroundLayers.stream()
                            .filter(i -> i.id().equalsIgnoreCase(layerId))
                            .findFirst()
                            .orElse(null);
                } else {
                    z = Integer.parseInt(name[2]);
                }
                if (Config.cacheValidateWorlds) {
                    if (Bukkit.getWorld(name[0]) == null) {
                        deletedInvalidWorlds++;
                        file.delete();
                        continue;
                    }
                }
                cachedFiles.add(new ChunkEntry(name[0], Integer.parseInt(name[1]), z, layer));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        reportTask.cancel();
        NMinimap.getInstance().getLogger().info("Cache init done! Loaded " + cachedFiles.size() + " tiles. Deleted " + deletedOld + " old cache files and " + deletedInvalidWorlds + " invalid world files.");
    }

    private void reportCacheLoadingStatus() {
        NMinimap.getInstance().getLogger().info("Loaded " + cachedFiles.size() + " tiles.");
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

        NMinimap.async(() -> {
            var file = chunk.getAsFile();
            try {
                if (!file.exists()) {
                    cachedFiles.remove(chunk);
                    return;
                }

                try (var is = new GZIPInputStream(new FileInputStream(file))) {
                    var scales = MapDataUtil.readMap(is);
                    chunkManager.getLoadedTiles().put(chunk, scales);
                    chunkManager.getLastChunkUse().put(chunk, System.currentTimeMillis());
                } catch (FileNotFoundException ex) {//?!
                    cachedFiles.remove(chunk);
                } catch (Exception ex) {
                    if (Config.cacheDeleteIfReadFailed) {
                        try {
                            if (file.exists())
                                file.delete();
                        } catch (Exception ex2) {
                            NMinimap.getInstance().getLogger().log(Level.SEVERE, "Failed to delete broken cache file" + file, ex2);
                        }
                    }
                    cachedFiles.remove(chunk);//prevent another failed load try
                    NMinimap.getInstance().getLogger().log(Level.SEVERE, "Failed to load chunk tile from cache", ex);
                }
            } finally {
                chunkManager.finishChunkJob(chunk);
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
        var file = chunk.getAsFile();
        var parent = file.getParentFile();
        if (parent != null)
            parent.mkdirs();

        var tmpDir = parent != null ? parent : Config.cacheFolder;
        var tmpFile = new File(tmpDir, file.getName() + ".tmp." + UUID.randomUUID());

        try {
            cachedFiles.remove(chunk);//prevent load from cache if file is being written right now
            try (var os = new GZIPOutputStream(new FileOutputStream(tmpFile))) {
                MapDataUtil.saveMap(scales, os);
            }

            try {
                Files.move(tmpFile.toPath(), file.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
                Files.move(tmpFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            cachedFiles.add(chunk);
        } catch (Exception ex) {
            cachedFiles.remove(chunk);
            NMinimap.getInstance().getLogger().log(Level.SEVERE, "Failed to save chunk tile to cache", ex);
        } finally {
            // If the move failed, remove the tmp file.
            try {
                if (tmpFile.exists())
                    tmpFile.delete();
            } catch (Exception ex) {
                NMinimap.getInstance().getLogger().log(Level.SEVERE, "Failed to delete tmp cache file" + tmpFile, ex);
            }
        }
    }

    public boolean cleanCache(String world) {
        return cleanCache0(new ArrayList<>(cachedFiles.stream().filter(i -> i.world().equalsIgnoreCase(world)).toList()));
    }


    public boolean cleanCache() {
        return cleanCache0(new ArrayList<>(cachedFiles));
    }

    private boolean cleanCache0(List<ChunkEntry> cache) {
        NMinimap.getInstance().getLogger().info("Cleaning cache...");


        var size = cachedFiles.size();


        var runnable = new BukkitRunnable() {

            public int cleaned = 0;

            @Override
            public void run() {
                NMinimap.getInstance().getLogger().info("Deleted " + cleaned + " of " + size + " tiles (" + (Math.floor(100 * ((double) cleaned) / ((double) size))) + "%)");
            }
        };

        runnable.runTaskTimerAsynchronously(NMinimap.getInstance(), 40, 40);
        var hasExceptions = false;
        for (var i : cache) {
            try {
                removeFromCache(i);
                runnable.cleaned++;
            } catch (Exception ex) {
                hasExceptions = true;
                NMinimap.getInstance().getLogger().log(Level.SEVERE, "Failed to delete chunk tile from cache", ex);
            }
        }
        runnable.cancel();
        NMinimap.getInstance().getLogger().info("Cache cleaned!");
        return hasExceptions;
    }


    public Set<ChunkEntry> getCachedFiles() {
        return cachedFiles;
    }

    public boolean isDiskFull() {
        return isDiskFull;
    }
}
