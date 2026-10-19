package su.nezushin.nminimap.chunks;

import java.util.Map;

/**
 * One layer of a chunk tile at scales 1/2/4/8
 * (same grid size per scale — 16² / 8² / 4² / 2²).
 * <p>
 * A layer is an open space: the player can occupy it from {@code floors} (the floor block itself, so slabs,
 * paths and soul sand count) up to {@code tops}. Liquids inside the space are part of it.
 * {@code scales} = map colors without hillshade (water keeps its depth shade), {@code draws} = Y of the drawn block.
 */
public record LayerData(Map<Integer, byte[]> scales, Map<Integer, short[]> draws,
                        Map<Integer, short[]> floors, Map<Integer, short[]> tops) {

    public static final short NO_LAYER = Short.MIN_VALUE;

    /** {@code tops} value of a space with nothing solid above it. */
    public static final short SKY = Short.MAX_VALUE;

    public byte colorAt(int scale, int indexXX, int indexZZ, int chunkSize) {
        if (scales == null)
            return 0;
        var grid = scales.get(scale);
        if (grid == null)
            return 0;
        int idx = indexXX + (indexZZ * chunkSize);
        if (idx < 0 || idx >= grid.length)
            return 0;
        return grid[idx];
    }

    public short drawAt(int scale, int indexXX, int indexZZ, int chunkSize) {
        return at(draws, scale, indexXX, indexZZ, chunkSize);
    }

    public short floorAt(int scale, int indexXX, int indexZZ, int chunkSize) {
        return at(floors, scale, indexXX, indexZZ, chunkSize);
    }

    public short topAt(int scale, int indexXX, int indexZZ, int chunkSize) {
        return at(tops, scale, indexXX, indexZZ, chunkSize);
    }

    private static short at(Map<Integer, short[]> grids, int scale, int indexXX, int indexZZ, int chunkSize) {
        if (grids == null)
            return NO_LAYER;
        var grid = grids.get(scale);
        if (grid == null)
            return NO_LAYER;
        int idx = indexXX + (indexZZ * chunkSize);
        if (idx < 0 || idx >= grid.length)
            return NO_LAYER;
        return grid[idx];
    }
}
