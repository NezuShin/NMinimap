package su.nezushin.nminimap.util;

import org.bukkit.ChunkSnapshot;
import su.nezushin.nminimap.util.config.SmartDescendSettings;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Checks a six-directionally connected transparent space in a 3-by-3 chunk window. */
public final class ConnectedCaveCheck {
    private static final int WIDTH = 48;
    private static final int PLANE = WIDTH * WIDTH;
    private final Map<Long, ChunkSnapshot> snapshots;

    public ConnectedCaveCheck(Map<Long, ChunkSnapshot> snapshots) {
        this.snapshots = snapshots;
    }

    public static long chunkKey(int dx, int dz) {
        return ((long) dx << 32) | (dz & 0xffffffffL);
    }

    public boolean hasConnectedColumns(int x, int y, int z, int minY, int maxY, SmartDescendSettings settings) {
        if (settings.minConnectedColumns() <= 1)
            return true;

        var pending = new ArrayDeque<Long>();
        Set<Long> visited = new HashSet<>();
        Set<Integer> columns = new HashSet<>();
        pending.add(encode(x, y, z, minY));

        while (!pending.isEmpty()) {
            long point = pending.removeFirst();
            if (!visited.add(point))
                continue;
            int py = (int) (point / PLANE) + minY;
            int column = (int) (point % PLANE);
            int px = column % WIDTH - 16;
            int pz = column / WIDTH - 16;
            if (px < -16 || px >= 32 || pz < -16 || pz >= 32 || py < minY || py > maxY)
                continue;

            var snapshot = snapshots.get(chunkKey(Math.floorDiv(px, 16), Math.floorDiv(pz, 16)));
            if (snapshot == null || !settings.transparentBlocks().contains(
                    snapshot.getBlockType(Math.floorMod(px, 16), py, Math.floorMod(pz, 16))))
                continue;

            columns.add(column);
            if (columns.size() >= settings.minConnectedColumns())
                return true;

            if (px > -16) pending.add(encode(px - 1, py, pz, minY));
            if (px < 31) pending.add(encode(px + 1, py, pz, minY));
            if (pz > -16) pending.add(encode(px, py, pz - 1, minY));
            if (pz < 31) pending.add(encode(px, py, pz + 1, minY));
            if (py > minY) pending.add(encode(px, py - 1, pz, minY));
            if (py < maxY) pending.add(encode(px, py + 1, pz, minY));
        }
        return false;
    }

    private static long encode(int x, int y, int z, int minY) {
        return (long) (y - minY) * PLANE + (z + 16L) * WIDTH + x + 16;
    }
}
