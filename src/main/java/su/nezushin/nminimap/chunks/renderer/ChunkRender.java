package su.nezushin.nminimap.chunks.renderer;

import org.bukkit.Chunk;
import org.bukkit.Material;
import su.nezushin.nminimap.NMinimap;
import su.nezushin.nminimap.chunks.BlockDataInfo;
import su.nezushin.nminimap.chunks.ChunkEntry;
import su.nezushin.nminimap.chunks.ChunkTile;
import su.nezushin.nminimap.chunks.LayerData;
import su.nezushin.nminimap.chunks.TileBakeSettings;
import su.nezushin.nminimap.util.ChunkLoadingUtil;
import su.nezushin.nminimap.util.ColorUtil;
import su.nezushin.nminimap.util.PerWorldSettingsUtil;
import su.nezushin.nminimap.util.RenderUtil;
import su.nezushin.nminimap.util.config.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class ChunkRender {

    private static final int[] SCALES = {1, 2, 4, 8};

    public void renderChunk(ChunkEntry chunk) {
        var chunkManager = NMinimap.getInstance().getChunkManager();
        CompletableFuture<Chunk> futureChunk = ChunkLoadingUtil.getChunkAt(chunk.getWorld(), chunk.x(), chunk.z());

        futureChunk.whenComplete((c, ex) -> {
            if (ex != null) {
                chunkManager.finishChunkJob(chunk);
                NMinimap.getInstance().getLogger().log(Level.SEVERE, "Failed to render chunk tile", ex);

                return;
            }

            try {
                NMinimap.async(() -> {
                    try {
                        var hasCeiling = c.getWorld().hasCeiling();
                        int minY = PerWorldSettingsUtil.getMinY(c.getWorld());
                        boolean skipCeiling = PerWorldSettingsUtil.getSkipCeiling(c.getWorld());
                        Set<Material> ceilingBlocks = PerWorldSettingsUtil.getCeilingBlocks(c.getWorld());
                        var chunkSnapshot = c.getChunkSnapshot(true, false, false);

                        int maxY = PerWorldSettingsUtil.getMaxY(c.getWorld());
                        if (chunk.layer() != null)
                            maxY = Math.min(maxY, chunk.layer().renderFromY());

                        int maxLayers = Config.multiLayerMaxLayers;
                        int minAirGap = Config.multiLayerMinAirGap;

                        @SuppressWarnings("unchecked")
                        List<BlockDataInfo>[] columns = new List[16 * 16];
                        for (var x = 0; x < 16; x++) {
                            for (var z = 0; z < 16; z++) {
                                columns[x + (z * 16)] = RenderUtil.getBlockLayersAt(chunkSnapshot, x, z, minY, maxY,
                                        hasCeiling, skipCeiling, ceilingBlocks, maxLayers, minAirGap);
                            }
                        }

                        var tile = bake(columns, TileBakeSettings.current(c.getWorld(), chunk.layer()));
                        chunkManager.getLoadedTiles().put(chunk, tile);
                        chunkManager.getLastChunkUse().put(chunk, System.currentTimeMillis());
                        chunkManager.getChunkCache().saveToCache(chunk, tile);
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

    /** Bake the open spaces of the 16×16 columns ({@code x + z * 16}) into per-scale layer grids. */
    private static ChunkTile bake(List<BlockDataInfo>[] columns, TileBakeSettings bakeSettings) {
        int layerCount = 1;
        for (var layers : columns)
            layerCount = Math.max(layerCount, layers.size());

        List<LayerData> baked = new ArrayList<>(layerCount);
        for (int layerIndex = 0; layerIndex < layerCount; layerIndex++)
            baked.add(new LayerData(new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>()));

        for (var scale : SCALES) {
            int size = 16 / scale;
            for (var layer : baked) {
                layer.scales().put(scale, new byte[size * size]);
                layer.draws().put(scale, emptyGrid(size));
                layer.floors().put(scale, emptyGrid(size));
                layer.tops().put(scale, emptyGrid(size));
            }

            for (var x = 0; x < size; x++) {
                for (var z = 0; z < size; z++) {
                    int idx = x + (z * size);
                    // One real column per cell keeps its layers physically consistent
                    var cell = scale == 1 ? columns[idx] : representative(columns, x, z, scale);
                    for (int layerIndex = 0; layerIndex < cell.size(); layerIndex++) {
                        var anchor = cell.get(layerIndex);
                        var info = scale == 1 ? anchor : RenderUtil.getMostCommonBlockData(columns, x, z, scale, anchor);
                        if (info.drawY() == LayerData.NO_LAYER)
                            continue;

                        var layer = baked.get(layerIndex);
                        layer.scales().get(scale)[idx] = bakeColor(info, x, z);
                        layer.draws().get(scale)[idx] = (short) info.drawY();
                        layer.floors().get(scale)[idx] = (short) info.floorY();
                        layer.tops().get(scale)[idx] = (short) info.topY();
                    }
                }
            }
        }
        return new ChunkTile(baked, bakeSettings);
    }

    /** Column of a {@code scale}² cell with the most layers, nearest to the cell center on ties. */
    private static List<BlockDataInfo> representative(List<BlockDataInfo>[] columns, int x, int z, int scale) {
        List<BlockDataInfo> best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (var dx = 0; dx < scale; dx++)
            for (var dz = 0; dz < scale; dz++) {
                var layers = columns[((x * scale) + dx) + (((z * scale) + dz) * 16)];
                int offsetX = 2 * dx - (scale - 1);
                int offsetZ = 2 * dz - (scale - 1);
                int distance = offsetX * offsetX + offsetZ * offsetZ;
                if (best == null || layers.size() > best.size()
                        || (layers.size() == best.size() && distance < bestDistance)) {
                    best = layers;
                    bestDistance = distance;
                }
            }
        return best;
    }

    /** Hillshade is applied when composing the map; only water gets its depth shade here. */
    private static byte bakeColor(BlockDataInfo info, int x, int z) {
        byte color = ColorUtil.exactColor(info.color());
        if (info.waterDepth() > 0)
            color = ColorUtil.waterShade(color, info.waterDepth(), (x + z) & 1);
        return color;
    }

    private static short[] emptyGrid(int size) {
        var grid = new short[size * size];
        Arrays.fill(grid, LayerData.NO_LAYER);
        return grid;
    }
}
