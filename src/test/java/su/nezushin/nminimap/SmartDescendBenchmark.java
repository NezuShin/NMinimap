package su.nezushin.nminimap;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import su.nezushin.nminimap.util.ConnectedCaveCheck;
import su.nezushin.nminimap.util.RenderUtil;
import su.nezushin.nminimap.util.config.SmartDescendSettings;
import su.nezushin.nminimap.util.config.UndergroundLayer;

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.atomic.LongAdder;

/** Synthetic CPU benchmark: does not measure server chunk loading, world generation or disk IO. */
public final class SmartDescendBenchmark {
    private static final Map<Material, BlockData> blocks = new EnumMap<>(Material.class);
    static {
        for (var material : List.of(Material.STONE, Material.GRASS_BLOCK, Material.AIR, Material.WATER)) {
            blocks.put(material, (BlockData) Proxy.newProxyInstance(BlockData.class.getClassLoader(),
                    new Class<?>[]{BlockData.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "getMaterial" -> material;
                        case "getMapColor" -> material == Material.WATER ? Color.BLUE
                                : material == Material.AIR ? Color.BLACK : Color.GRAY;
                        default -> throw new UnsupportedOperationException(method.getName());
                    }));
        }
    }

    private static ChunkSnapshot snapshot(int dx, int dz, String scenario, LongAdder reads) {
        return (ChunkSnapshot) Proxy.newProxyInstance(ChunkSnapshot.class.getClassLoader(),
                new Class<?>[]{ChunkSnapshot.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getHighestBlockYAt")) return 80;
                    reads.increment();
                    int x = dx * 16 + (int) args[0], y = (int) args[1], z = dz * 16 + (int) args[2];
                    Material type = Material.STONE;
                    if (y == 80) type = Material.GRASS_BLOCK;
                    else if (scenario.equals("cavern") && y >= 16 && y <= 31) type = Material.AIR;
                    else if (scenario.equals("flooded") && y >= 16 && y <= 31) type = Material.WATER;
                    else if (scenario.equals("isolated-shaft") && x == 4 && z == 4 && y >= -63 && y <= 50) type = Material.AIR;
                    else if (scenario.equals("floorless") && y <= 50 && y >= -64) type = Material.AIR;
                    return method.getName().equals("getBlockType") ? type : blocks.get(type);
                });
    }

    public static void main(String[] args) {
        RenderRegressionCheck.defaults();
        System.out.println("scenario,connected-columns,median-ms-per-tile,block-reads-per-tile");
        for (String scenario : List.of("stone", "cavern", "flooded", "isolated-shaft", "floorless")) {
            for (int connected : new int[]{1, 3, 64}) {
                var reads = new LongAdder();
                Map<Long, ChunkSnapshot> snapshots = new HashMap<>();
                for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
                    snapshots.put(ConnectedCaveCheck.chunkKey(dx, dz), snapshot(dx, dz, scenario, reads));
                var smart = new SmartDescendSettings(-64, false, SmartDescendSettings.NoOpeningMode.DESCEND,
                        Set.of(Material.AIR, Material.WATER), 2, connected);
                var layer = new UndergroundLayer("bench", List.of(), 50, 1, .5f, smart, RenderRegressionCheck.water(.15f));
                for (int i = 0; i < 20; i++) render(snapshots, layer);
                long[] times = new long[101];
                reads.reset();
                for (int i = 0; i < times.length; i++) {
                    long start = System.nanoTime();
                    render(snapshots, layer);
                    times[i] = System.nanoTime() - start;
                }
                Arrays.sort(times);
                System.out.printf(Locale.ROOT, "%s,%d,%.3f,%d%n", scenario, connected,
                        times[times.length / 2] / 1_000_000.0, reads.sum() / times.length);
            }
        }
    }

    private static void render(Map<Long, ChunkSnapshot> snapshots, UndergroundLayer layer) {
        var check = layer.smartDescend().minConnectedColumns() > 1 ? new ConnectedCaveCheck(snapshots) : null;
        var snapshot = snapshots.get(ConnectedCaveCheck.chunkKey(0, 0));
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            var result = RenderUtil.getUndergroundBlockDataAt(snapshot, x, z, -64, 100,
                    false, false, Set.of(), layer, null, check);
            RenderRegressionCheck.require(result.color().asRGB() != 0, "Transparent floor leaked into map");
        }
    }
}
