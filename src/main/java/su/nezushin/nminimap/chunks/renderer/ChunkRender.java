package su.nezushin.nminimap.chunks.renderer;

import org.bukkit.Chunk;
import su.nezushin.nminimap.NMinimap;
import su.nezushin.nminimap.chunks.BlockDataInfo;
import su.nezushin.nminimap.chunks.ChunkEntry;
import su.nezushin.nminimap.util.ColorUtil;
import su.nezushin.nminimap.util.RenderUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ChunkRender {

    public void renderChunk(ChunkEntry chunk) {
        var chunkManager = NMinimap.getInstance().getChunkManager();
        chunkManager.getLoadingChunks().add(chunk);

        CompletableFuture<Chunk> futureFirstChunk = chunk.w().getChunkAtAsync(chunk.x(), chunk.z());
        CompletableFuture<Chunk> futureSecondChunk = chunk.w().getChunkAtAsync(chunk.x(), chunk.z() - 1);

        CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(futureFirstChunk, futureSecondChunk);


        combinedFuture.thenAccept(v -> {
            Chunk c = futureFirstChunk.join();
            Chunk cNorth = futureSecondChunk.join();

            var hasCeiling = c.getWorld().hasCeiling();
            int minY = c.getWorld().getMinHeight();
            var chunkSnapshot = c.getChunkSnapshot(true, false, false);
            var northChunkSnapshot = cNorth.getChunkSnapshot(true, false, false);


            NMinimap.async(() -> {
                var northChunk = new BlockDataInfo[(16) * (8)];
                var currentChunk = new BlockDataInfo[(16) * (16)];

                for (var x = 0; x < 16; x++) {
                    for (var z = 0; z < 16; z++) {
                        if (z < 8) {
                            northChunk[x + (z * 16)] = RenderUtil.getHighestBlockDataAt(northChunkSnapshot, x, 15 - z, minY, hasCeiling);
                        }
                        currentChunk[x + (z * 16)] = RenderUtil.getHighestBlockDataAt(chunkSnapshot, x, z, minY, hasCeiling);
                    }
                }

                var lastYLevel = 0;

                Map<Integer, byte[]> scales = new HashMap<>();

                for (var scale : new int[]{1, 2, 4, 8}) {
                    byte[] bytes = new byte[(16 / scale) * (16 / scale)];
                    for (var x = 0; x < 16 / scale; x++) {
                        lastYLevel = 0;
                        for (var z = 0; z < 16 / scale; z++) {
                            if (z == 0) {
                                lastYLevel = RenderUtil.getMostCommonOpaqueBlockBlockData(northChunk, x, 0, scale).yLevel();
                            }
                            var info = RenderUtil.getMostCommonOpaqueBlockBlockData(currentChunk, x, z, scale);
                            var color = ColorUtil.exactColor(info.color());
                            var waterDepth = info.waterDepth();

                            //https://mcsrc.dev/1/26.1.1/net/minecraft/world/item/MapItem
                            if (waterDepth != 0) {
                                double diff = waterDepth * 0.1 + (x + z & 1) * 0.2;
                                if (diff < 0.5) {
                                } else if (diff > 0.9) {
                                    color -= 2;
                                } else {
                                    color -= 1;
                                }
                            } else {
                                var y = info.yLevel();
                                double diff = (y - lastYLevel) * 4.0 / (scale + 4) + ((x + z & 1) - 0.5) * 0.4;
                                if (diff > 0.6) {
                                } else if (diff < -0.6) {
                                    color -= 2;
                                } else {
                                    color -= 1;
                                }
                            }

                            bytes[x + (z * (16 / scale))] = color;


                            lastYLevel = info.yLevel();
                        }
                    }
                    scales.put(scale, bytes);
                }


                chunkManager.getLoadedTiles().put(chunk, scales);
                chunkManager.getChunkCache().saveToCache(chunk, scales);
                chunkManager.getLoadingChunks().remove(chunk);

                chunkManager.renderNextAwaitingChunk();
            });
        });
    }
}
