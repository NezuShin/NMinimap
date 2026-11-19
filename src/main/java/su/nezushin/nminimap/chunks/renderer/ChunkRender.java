package su.nezushin.nminimap.chunks.renderer;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;
import su.nezushin.nminimap.NMinimap;
import su.nezushin.nminimap.chunks.BlockDataInfo;
import su.nezushin.nminimap.chunks.ChunkEntry;
import su.nezushin.nminimap.chunks.ChunkManager;
import su.nezushin.nminimap.util.ChunkLoadingUtil;
import su.nezushin.nminimap.util.ColorUtil;
import su.nezushin.nminimap.util.PerWorldSettingsUtil;
import su.nezushin.nminimap.util.RenderUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Level;

public class ChunkRender {

    public void renderChunk(ChunkEntry chunk) {
        var chunkManager = NMinimap.getInstance().getChunkManager();
        var world = chunk.getWorld();
        if (world == null) {
            chunkManager.finishChunkJob(chunk);
            return;
        }

        var northChunkEntry = new ChunkEntry(chunk.world(), chunk.x(), chunk.z() - 1, null);
        CompletableFuture<Supplier<ChunkSnapshot>> futureFirstChunk = ChunkLoadingUtil.getChunkSnapshotAt(world, chunk.x(), chunk.z());
        CompletableFuture<Supplier<ChunkSnapshot>> futureSecondChunk = loadNorthChunkSnapshot(chunkManager, world, northChunkEntry);

        CompletableFuture.allOf(futureFirstChunk, futureSecondChunk).whenComplete((v, ex) -> {
            if (ex != null) {
                chunkManager.finishChunkJob(chunk);
                NMinimap.getInstance().getLogger().log(Level.SEVERE, "Failed to render chunk tile", ex);

                return;
            }

            try {
                Supplier<ChunkSnapshot> c = futureFirstChunk.join();
                if (c == null) {
                    chunkManager.finishChunkJob(chunk);
                    return;
                }
                Supplier<ChunkSnapshot> cNorth = futureSecondChunk.join();

                NMinimap.async(() -> {
                    try {
                        var hasCeiling = world.hasCeiling();
                        int minY = PerWorldSettingsUtil.getMinY(world);
                        boolean skipCeiling = PerWorldSettingsUtil.getSkipCeiling(world);
                        Set<Material> ceilingBlocks = PerWorldSettingsUtil.getCeilingBlocks(world);
                        var chunkSnapshot = c.get();
                        var northChunkSnapshot = cNorth != null ? cNorth.get() : null;

                        var northChunk = northChunkSnapshot != null ? new BlockDataInfo[(16) * (8)] : null;
                        var currentChunk = new BlockDataInfo[(16) * (16)];

                        int maxY = PerWorldSettingsUtil.getMaxY(world);
                        if (chunk.layer() != null)
                            maxY = Math.min(maxY, chunk.layer().renderFromY());

                        for (var x = 0; x < 16; x++) {
                            for (var z = 0; z < 16; z++) {
                                if (z < 8 && northChunkSnapshot != null) {
                                    northChunk[x + (z * 16)] = RenderUtil.getHighestBlockDataAt(northChunkSnapshot, x, 15 - z, minY, maxY, hasCeiling, skipCeiling, ceilingBlocks);
                                }
                                currentChunk[x + (z * 16)] = RenderUtil.getHighestBlockDataAt(chunkSnapshot, x, z, minY, maxY, hasCeiling, skipCeiling, ceilingBlocks);
                            }
                        }

                        var lastYLevel = 0;

                        Map<Integer, byte[]> scales = new HashMap<>();

                        for (var scale : new int[]{1, 2, 4, 8}) {
                            byte[] bytes = new byte[(16 / scale) * (16 / scale)];
                            for (var x = 0; x < 16 / scale; x++) {
                                lastYLevel = 0;
                                for (var z = 0; z < 16 / scale; z++) {
                                    if (z == 0 && northChunk != null) {
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
                        chunkManager.getLastChunkUse().put(chunk, System.currentTimeMillis());
                        chunkManager.getChunkCache().saveToCache(chunk, scales);
                    } catch (Exception e) {
                        NMinimap.getInstance().getLogger().log(Level.SEVERE, "Failed to render chunk tile", e);
                    } finally {
                        chunkManager.finishChunkJob(chunk);
                    }
                });
            } catch (Exception e) {
                chunkManager.finishChunkJob(chunk);
                NMinimap.getInstance().getLogger().log(Level.SEVERE, "Failed to render chunk tile", e);
            }
        });
    }

    /**
     * Loads the northern neighbor snapshot only inside the world border without generating missing
     * chunks on Paper, and tracks the load so {@code ChunkListener} does not recurse on it.
     */
    private CompletableFuture<Supplier<ChunkSnapshot>> loadNorthChunkSnapshot(ChunkManager chunkManager, World world, ChunkEntry northChunkEntry) {
        if (!northChunkEntry.isInsideWorldBorder()) {
            return CompletableFuture.completedFuture(null);
        }

        chunkManager.markNeighborChunkLoading(northChunkEntry);
        try {
            return ChunkLoadingUtil.getChunkSnapshotAt(world, northChunkEntry.x(), northChunkEntry.z(), false)
                    .whenComplete((v, ex) -> chunkManager.unmarkNeighborChunkLoading(northChunkEntry));
        } catch (Throwable t) {
            chunkManager.unmarkNeighborChunkLoading(northChunkEntry);
            return CompletableFuture.failedFuture(t);
        }
    }
}
