package su.nezushin.nminimap.chunks.cache;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import su.nezushin.nminimap.NMinimap;
import su.nezushin.nminimap.chunks.ChunkEntry;
import su.nezushin.nminimap.util.config.Config;

import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkCache {

    private Gson gson = new Gson();


    private Set<ChunkEntry> cachedFiles = ConcurrentHashMap.newKeySet();


    public ChunkCache() {
        if (!Config.allowFileCache)
            return;
        loadCachedFiles();
    }

    public void loadCachedFiles() {
        for (var file : Config.cacheFolder.listFiles()) {
            String[] name = file.getName().split("\\.");
            cachedFiles.add(new ChunkEntry(Bukkit.getWorld(name[0]), Integer.parseInt(name[1]), Integer.parseInt(name[2])));
        }
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

            try (var reader = new FileReader(file)) {
                var type = new TypeToken<Map<Integer, String>>() {
                }.getType();

                Map<Integer, String> map = gson.fromJson(reader, type);

                Map<Integer, byte[]> scales = new HashMap<>();
                map.forEach((a, b) -> {
                    scales.put(a, Base64.getDecoder().decode(b));
                });

                chunkManager.getLoadedTiles().put(chunk, scales);
                chunkManager.getLoadingChunks().remove(chunk);
                chunkManager.renderNextAwaitingChunk();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    public void saveToCache(ChunkEntry chunk, Map<Integer, byte[]> scales) {
        if (!Config.allowFileCache)
            return;
        Map<Integer, String> map = new HashMap<>();

        scales.forEach((a, b) -> {
            map.put(a, Base64.getEncoder().encodeToString(b));
        });


        try (var writer = new FileWriter(chunk.getAsFile())) {

            gson.toJson(map, writer);
            cachedFiles.add(chunk);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public Set<ChunkEntry> getCachedFiles() {
        return cachedFiles;
    }
}
