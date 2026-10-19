package su.nezushin.nminimap.util;

import su.nezushin.nminimap.chunks.ChunkManager;
import su.nezushin.nminimap.chunks.ChunkTile;

import java.util.Map;

public class RamCapacityUtil {

    /** Estimated object overhead per loadedTiles entry (CHM node, ChunkEntry, inner map, lastChunkUse). */
    public static final long OVERHEAD_PER_TILE = 256L;

    private static final long GB = 1024L * 1024L * 1024L;

    public static long getUsedMemory() {
        var runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    public static long getMaxMemory() {
        return Runtime.getRuntime().maxMemory();
    }

    /** Headroom until -Xmx (same role as disk usable space for the RAM threshold). */
    public static long getAvailableMemory() {
        return getMaxMemory() - getUsedMemory();
    }

    public static long estimatePluginMemory(ChunkManager chunkManager) {
        long total = 0;
        var emptyTile = chunkManager.getEmptyTile();
        for (var tile : chunkManager.getLoadedTiles().values()) {
            total += OVERHEAD_PER_TILE;
            if (tile == emptyTile)
                continue;
            total += estimateTileBytes(tile);
        }
        return total;
    }

    private static long estimateTileBytes(ChunkTile tile) {
        long total = 0;
        for (var layer : tile.layers()) {
            if (layer.scales() != null) {
                for (var bytes : layer.scales().values()) {
                    if (bytes != null)
                        total += bytes.length;
                }
            }
            total += estimateGridBytes(layer.draws());
            total += estimateGridBytes(layer.floors());
            total += estimateGridBytes(layer.tops());
        }
        return total;
    }

    private static long estimateGridBytes(Map<Integer, short[]> grids) {
        long total = 0;
        if (grids != null) {
            for (var grid : grids.values()) {
                if (grid != null)
                    total += grid.length * 2L;
            }
        }
        return total;
    }

    public static String formatGb(long bytes) {
        return String.format("%.1f", (double) bytes / GB);
    }
}
