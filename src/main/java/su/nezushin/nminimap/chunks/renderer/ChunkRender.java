package su.nezushin.nminimap.chunks.renderer;

import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import su.nezushin.nminimap.NMinimap;
import su.nezushin.nminimap.chunks.BlockDataInfo;
import su.nezushin.nminimap.chunks.ChunkEntry;
import su.nezushin.nminimap.util.ChunkLoadingUtil;
import su.nezushin.nminimap.util.ConnectedCaveCheck;
import su.nezushin.nminimap.util.ColorUtil;
import su.nezushin.nminimap.util.PerWorldSettingsUtil;
import su.nezushin.nminimap.util.RenderUtil;
import su.nezushin.nminimap.util.config.Config;
import su.nezushin.nminimap.util.config.WaterRenderingSettings;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class ChunkRender {

    public void renderChunk(ChunkEntry chunk) {
        var chunkManager = NMinimap.getInstance().getChunkManager();
        CompletableFuture<Chunk> futureFirstChunk = ChunkLoadingUtil.getChunkAt(chunk.getWorld(), chunk.x(), chunk.z());
        CompletableFuture<Chunk> futureSecondChunk = ChunkLoadingUtil.getChunkAt(chunk.getWorld(), chunk.x(), chunk.z() - 1);
        Map<Long, CompletableFuture<Chunk>> nearby = new HashMap<>();
        nearby.put(ConnectedCaveCheck.chunkKey(0, 0), futureFirstChunk);
        nearby.put(ConnectedCaveCheck.chunkKey(0, -1), futureSecondChunk);
        if (chunk.layer() != null && chunk.layer().smartDescend().minConnectedColumns() > 1) {
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++) {
                    long key = ConnectedCaveCheck.chunkKey(dx, dz);
                    if (!nearby.containsKey(key))
                        nearby.put(key, ChunkLoadingUtil.getChunkAt(chunk.getWorld(), chunk.x() + dx, chunk.z() + dz));
                }
        }

        CompletableFuture.allOf(nearby.values().toArray(CompletableFuture[]::new)).whenComplete((v, ex) -> {
            if (ex != null) {
                chunkManager.finishChunkJob(chunk);
                NMinimap.getInstance().getLogger().log(Level.SEVERE, "Failed to render chunk tile", ex);

                return;
            }

            try {
                Chunk c = futureFirstChunk.join();
                Chunk cNorth = futureSecondChunk.join();

                NMinimap.async(() -> {
                    try {
                        var hasCeiling = c.getWorld().hasCeiling();
                        int minY = PerWorldSettingsUtil.getMinY(c.getWorld());
                        boolean skipCeiling = PerWorldSettingsUtil.getSkipCeiling(c.getWorld());
                        Set<Material> ceilingBlocks = PerWorldSettingsUtil.getCeilingBlocks(c.getWorld());
                        var chunkSnapshot = c.getChunkSnapshot(true, false, false);
                        var northChunkSnapshot = cNorth.getChunkSnapshot(true, false, false);
                        ConnectedCaveCheck connectedCaveCheck = null;
                        if (nearby.size() > 2) {
                            Map<Long, ChunkSnapshot> snapshots = new HashMap<>();
                            for (var entry : nearby.entrySet())
                                snapshots.put(entry.getKey(), entry.getValue().join().getChunkSnapshot(true, false, false));
                            connectedCaveCheck = new ConnectedCaveCheck(snapshots);
                        }

                        var northChunk = new BlockDataInfo[(16) * (8)];
                        var currentChunk = new BlockDataInfo[(16) * (16)];

                        int maxY = PerWorldSettingsUtil.getMaxY(c.getWorld());

                        for (var x = 0; x < 16; x++) {
                            for (var z = 0; z < 16; z++) {
                                if (z < 8) {
                                    northChunk[x + (z * 16)] = renderColumn(northChunkSnapshot, x, 15 - z, minY, maxY,
                                            hasCeiling, skipCeiling, ceilingBlocks, chunk, cNorth, null);
                                }
                                currentChunk[x + (z * 16)] = renderColumn(chunkSnapshot, x, z, minY, maxY,
                                        hasCeiling, skipCeiling, ceilingBlocks, chunk, c, connectedCaveCheck);
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
                                    var waterSettings = chunk.layer() == null ? Config.waterRendering : chunk.layer().waterRendering();
                                    var color = waterColor(info, waterSettings);
                                    var waterDepth = info.waterDepth();

                                    //https://mcsrc.dev/1/26.1.1/net/minecraft/world/item/MapItem
                                    if (waterDepth != 0 && waterSettings.mode() == WaterRenderingSettings.Mode.VANILLA) {
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

                                    if (info.missingCave() && chunk.layer() != null)
                                        color = ColorUtil.darken(color, chunk.layer().darken());

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

    private static BlockDataInfo renderColumn(org.bukkit.ChunkSnapshot snapshot, int x, int z, int minY, int maxY,
                                               boolean hasCeiling, boolean skipCeiling, Set<Material> ceilingBlocks,
                                               ChunkEntry chunk, Chunk sourceChunk, ConnectedCaveCheck connectedCaveCheck) {
        if (chunk.layer() == null)
            return RenderUtil.getHighestBlockDataAt(snapshot, x, z, minY, maxY, hasCeiling, skipCeiling, ceilingBlocks,
                    Config.waterRendering.maxSampledDepth());
        Integer regionFloor = null;
        if (chunk.layer().smartDescend().useRegionFloor())
            regionFloor = NMinimap.getInstance().getWorldGuardManager().getLayerFloorAt(sourceChunk.getWorld(),
                    sourceChunk.getX() * 16 + x, sourceChunk.getZ() * 16 + z, chunk.layer());
        return RenderUtil.getUndergroundBlockDataAt(snapshot, x, z, minY, maxY, hasCeiling, skipCeiling,
                ceilingBlocks, chunk.layer(), regionFloor, connectedCaveCheck);
    }

    private static byte waterColor(BlockDataInfo info, WaterRenderingSettings settings) {
        if (info.waterDepth() == 0)
            return ColorUtil.exactColor(info.color());
        if (settings.mode() == WaterRenderingSettings.Mode.VANILLA)
            return ColorUtil.exactColor(info.color());
        if (settings.mode() == WaterRenderingSettings.Mode.DISABLED)
            return ColorUtil.exactColor(info.bottomColor());

        var base = settings.colorSource() == WaterRenderingSettings.ColorSource.BOTTOM
                ? info.bottomColor() : info.color();
        byte color = ColorUtil.blend(base, settings.tint(), settings.opacityForDepth(info.waterDepth()));
        return ColorUtil.darken(color, settings.underwaterDarken());
    }
}
