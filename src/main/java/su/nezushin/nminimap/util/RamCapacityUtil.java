package su.nezushin.nminimap.util;

import su.nezushin.nminimap.chunks.ChunkManager;

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
        var emptyMap = chunkManager.getEmptyMap();
        for (var scales : chunkManager.getLoadedTiles().values()) {
            total += OVERHEAD_PER_TILE;
            if (scales == emptyMap)
                continue;
            for (var bytes : scales.values()) {
                if (bytes != null)
                    total += bytes.length;
            }
        }
        return total;
    }

    public static String formatGb(long bytes) {
        return String.format("%.1f", (double) bytes / GB);
    }
}
