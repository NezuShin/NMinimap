package su.nezushin.nminimap.chunks.cache;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import su.nezushin.nminimap.NMinimap;
import su.nezushin.nminimap.chunks.ChunkEntry;
import su.nezushin.nminimap.util.DiskCapacityUtil;
import su.nezushin.nminimap.util.PerWorldSettingsUtil;
import su.nezushin.nminimap.util.SchedulerUtil;
import su.nezushin.nminimap.util.config.Config;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class ChunkCache {

    private final Set<ChunkEntry> cachedFiles = ConcurrentHashMap.newKeySet();
    private final TileFiles tileFiles = new TileFiles();
    private static final Pattern TILE_NAME = Pattern.compile("^(.+)\\.(-?\\d+)\\.(-?\\d+)(?:(_surface_layer_|_layer_)(.+))?\\.bin\\.gz$");


    private boolean isDiskFull;

    public ChunkCache() {
        if (!Config.allowFileCache && Config.perWorldSettings.stream().noneMatch(settings -> Boolean.TRUE.equals(settings.allowFileCache()))) {
            return;
        }
        if (Config.cacheLoadDelay <= 0)
            loadCachedFiles();
        else
            SchedulerUtil.getScheduler().async(this::loadCachedFiles, Config.cacheLoadDelay);
    }

    public void loadCachedFiles() {
        var deletedInvalidWorlds = 0;
        NMinimap.getInstance().getLogger().info("Loading cache...");
        this.cachedFiles.clear();
        Set<Path> namespaces = new HashSet<>();
        Set<String> worlds = new HashSet<>();
        Bukkit.getWorlds().forEach(world -> worlds.add(world.getName()));
        Config.perWorldSettings.forEach(settings -> worlds.addAll(settings.worlds()));
        for (var world : worlds) {
            if (!PerWorldSettingsUtil.getAllowFileCache(world)) continue;
            namespaces.add(new ChunkEntry(world, 0, 0, null).getAsFile().getParentFile().toPath());
            for (var layer : PerWorldSettingsUtil.getUndergroundLayers(world)) {
                namespaces.add(new ChunkEntry(world, 0, 0, layer).getAsFile().getParentFile().toPath());
                if (layer.waterRendering().scope() == su.nezushin.nminimap.util.config.WaterRenderingSettings.Scope.MAP)
                    namespaces.add(new ChunkEntry(world, 0, 0, layer, true).getAsFile().getParentFile().toPath());
            }
        }
        try {
            for (var namespace : namespaces) {
                if (!Files.isDirectory(namespace))
                    continue;
                try (var stream = Files.list(namespace)) {
                    for (var path : (Iterable<Path>) stream::iterator) {
                        var file = path.toFile();
                        if (!file.isFile())
                            continue;
                        var name = TILE_NAME.matcher(file.getName());
                        if (!name.matches())
                            continue;

                        String world = name.group(1);
                        su.nezushin.nminimap.util.config.UndergroundLayer layer = null;
                        boolean surface = "_surface_layer_".equals(name.group(4));
                        if (name.group(5) != null) {
                            String layerId = name.group(5);
                            layer = PerWorldSettingsUtil.getUndergroundLayers(world).stream()
                                    .filter(i -> i.id().equalsIgnoreCase(layerId))
                                    .findFirst()
                                    .orElse(null);
                            if (layer == null)
                                continue;
                        }
                        int x;
                        int z;
                        try {
                            x = Integer.parseInt(name.group(2));
                            z = Integer.parseInt(name.group(3));
                        } catch (NumberFormatException ex) {
                            continue;
                        }
                        if (Config.cacheValidateWorlds && Bukkit.getWorld(world) == null) {
                            deletedInvalidWorlds++;
                            tileFiles.delete(path);
                            continue;
                        }
                        if (!PerWorldSettingsUtil.getAllowFileCache(world))
                            continue;
                        var entry = new ChunkEntry(world, x, z, layer, surface);
                        if (!path.normalize().equals(entry.getAsFile().toPath().normalize()))
                            continue;
                        cachedFiles.add(entry);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        NMinimap.getInstance().getLogger().info("Cache index ready: " + cachedFiles.size()
                + " disk tiles (not loaded into RAM). Deleted " + deletedInvalidWorlds + " invalid world files.");
    }

    public void removeFromCache(ChunkEntry chunk) {
        cachedFiles.remove(chunk);
        var file = chunk.getAsFile();

        try {
            tileFiles.delete(file.toPath());
        } catch (IOException ex) {
            NMinimap.getInstance().getLogger().log(Level.SEVERE, "Failed to invalidate cache tile " + file, ex);
        }
    }

    public boolean hasInCache(ChunkEntry chunk) {
        // Also supports worlds loaded after the initial index and concurrent index rebuilds.
        return cachedFiles.contains(chunk) || chunk.getAsFile().isFile();
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

                try {
                    var scales = tileFiles.read(file.toPath());
                    chunkManager.getLoadedTiles().put(chunk, scales);
                    chunkManager.getLastChunkUse().put(chunk, System.currentTimeMillis());
                } catch (java.nio.file.NoSuchFileException ex) {
                    cachedFiles.remove(chunk);
                } catch (Exception ex) {
                    if (Config.cacheDeleteIfReadFailed) {
                        try {
                            tileFiles.delete(file.toPath());
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
        if (!PerWorldSettingsUtil.getAllowFileCache(chunk.world()))
            return;
        if (DiskCapacityUtil.getUsableSpace() < Config.availableDiskSpaceThreshold) {
            isDiskFull = true;
            return;
        }
        isDiskFull = false;
        var file = chunk.getAsFile();
        try {
            tileFiles.write(file.toPath(), scales);
            cachedFiles.add(chunk);
        } catch (Exception ex) {
            if (!file.isFile()) cachedFiles.remove(chunk);
            NMinimap.getInstance().getLogger().log(Level.SEVERE, "Failed to save chunk tile to cache", ex);
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
