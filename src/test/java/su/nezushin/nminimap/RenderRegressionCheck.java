package su.nezushin.nminimap;

import com.sun.nio.file.ExtendedOpenOption;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import su.nezushin.nminimap.chunks.ChunkEntry;
import su.nezushin.nminimap.chunks.cache.CacheFingerprint;
import su.nezushin.nminimap.chunks.cache.TileFiles;
import su.nezushin.nminimap.util.MapDataUtil;
import su.nezushin.nminimap.util.PerWorldSettingsUtil;
import su.nezushin.nminimap.util.config.*;

import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Standalone regression checks, also runnable without a Minecraft server. */
public final class RenderRegressionCheck {
    static WaterRenderingSettings water(float opacity) {
        return new WaterRenderingSettings(WaterRenderingSettings.Mode.FIXED, WaterRenderingSettings.Scope.REGION,
                opacity, .1f, .65f, 12, Color.fromRGB(0x3F9FD4), WaterRenderingSettings.ColorSource.BOTTOM, 8, .1f);
    }

    static void defaults() {
        Config.waterRendering = water(.35f);
        Config.smartDescendDefaults = new SmartDescendSettings(-64, false, SmartDescendSettings.NoOpeningMode.DESCEND,
                Set.of(Material.AIR, Material.CAVE_AIR, Material.WATER), 2, 3);
        Config.perWorldSettings = List.of();
        Config.undergroundLayers = List.of();
        PerWorldSettingsUtil.clearCache();
        CacheFingerprint.clear();
    }

    public static void main(String[] args) throws Exception {
        defaults();
        if (args.length > 0 && args[0].equals("fingerprint")) {
            System.out.println(CacheFingerprint.namespace(new ChunkEntry("world", 0, 0, null)));
            var layer = new UndergroundLayer("cave", List.of("region"), 40, 1, .5f,
                    Config.smartDescendDefaults, Config.waterRendering);
            System.out.println(CacheFingerprint.namespace(new ChunkEntry("world", 0, 0, layer)));
            return;
        }
        fingerprints();
        inheritance();
        tileFiles();
        UndergroundRenderCheck.main(new String[0]);
        System.out.println("Cache fingerprint, config inheritance and concurrent tile IO checks passed");
    }

    private static void fingerprints() {
        var smart = Config.smartDescendDefaults;
        var first = new UndergroundLayer("cave", List.of("b", "a"), 40, 1, .5f, smart, Config.waterRendering);
        var second = new UndergroundLayer("cave", List.of("a", "b"), 40, 99, .5f,
                new SmartDescendSettings(-64, false, smart.noOpeningMode(),
                        new LinkedHashSet<>(List.of(Material.WATER, Material.CAVE_AIR, Material.AIR)), 2, 3), water(.35f));
        String key = CacheFingerprint.namespace(new ChunkEntry("world", 1, 2, first));
        require(key.equals(CacheFingerprint.namespace(new ChunkEntry("world", 1, 2, second))), "Set/list order or priority changed disk identity");
        Config.waterRendering = water(.2f);
        CacheFingerprint.clear();
        String changed = CacheFingerprint.namespace(new ChunkEntry("world", 0, 0, null));
        Config.waterRendering = water(.35f);
        CacheFingerprint.clear();
        require(!changed.equals(CacheFingerprint.namespace(new ChunkEntry("world", 0, 0, null))), "Water opacity did not invalidate cache");
    }

    @SuppressWarnings("unchecked")
    private static void inheritance() throws Exception {
        var yaml = new YamlConfiguration();
        yaml.loadFromString("""
                underground-layers:
                  inherited:
                    wg-regions: [region]
                  explicit:
                    wg-regions: [region]
                    smart-descend:
                      min-connected-columns: 7
                    water-rendering:
                      opacity: 0.8
                per-world-settings:
                  test:
                    worlds: [world]
                    max-y: 90
                    water-rendering:
                      opacity: 0.15
                    smart-descend-defaults:
                      min-open-height: 4
                      min-connected-columns: 5
                """);
        Method loader = Config.class.getDeclaredMethod("loadPerWorldSettings", org.bukkit.configuration.file.FileConfiguration.class);
        loader.setAccessible(true);
        Config.perWorldSettings = (List<PerWorldSettings>) loader.invoke(null, yaml);
        PerWorldSettingsUtil.clearCache();
        CacheFingerprint.clear();
        require(PerWorldSettingsUtil.getWaterRendering("world").opacity() == .15f, "World water override ignored");
        require(PerWorldSettingsUtil.getWaterRendering("other").opacity() == .35f, "World override leaked");
        var layers = PerWorldSettingsUtil.getUndergroundLayers("world");
        var inherited = layers.stream().filter(layer -> layer.id().equals("inherited")).findFirst().orElseThrow();
        var explicit = layers.stream().filter(layer -> layer.id().equals("explicit")).findFirst().orElseThrow();
        require(inherited.smartDescend().minOpenHeight() == 4 && inherited.smartDescend().minConnectedColumns() == 5,
                "World smart-descend defaults ignored");
        require(inherited.waterRendering().opacity() == .15f && explicit.waterRendering().opacity() == .8f,
                "Layer/world/global water precedence incorrect");
        require(explicit.smartDescend().minConnectedColumns() == 7 && explicit.smartDescend().minOpenHeight() == 4,
                "Layer/world/global smart-descend precedence incorrect");
        String bounded = CacheFingerprint.namespace(new ChunkEntry("world", 0, 0, null));
        defaults();
        require(!bounded.equals(CacheFingerprint.namespace(new ChunkEntry("world", 0, 0, null))), "World Y bounds omitted from fingerprint");
    }

    private static Map<Integer, byte[]> tile(byte value) {
        var tile = MapDataUtil.prepareEmptyMap();
        tile.values().forEach(bytes -> Arrays.fill(bytes, value));
        return tile;
    }

    private static void tileFiles() throws Exception {
        var root = Files.createTempDirectory("nminimap-cache-check-");
        var path = root.resolve("tile.bin.gz");
        var files = new TileFiles();
        var pool = Executors.newFixedThreadPool(6);
        try {
            files.write(path, tile((byte) 10));
            var jobs = new ArrayList<Future<?>>();
            for (int task = 0; task < 6; task++) {
                int id = task;
                jobs.add(pool.submit(() -> {
                    for (int i = 0; i < 150; i++) {
                        try {
                            if (id < 3) files.write(path, tile((byte) (10 + id)));
                            else {
                                var data = files.read(path);
                                byte expected = data.get(1)[0];
                                for (var bytes : data.values()) for (byte value : bytes)
                                    require(value == expected, "Read a partially replaced tile");
                            }
                        } catch (Exception ex) { throw new RuntimeException(ex); }
                    }
                }));
            }
            for (var job : jobs) job.get();
            if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows")) {
                Future<?> writer;
                try (var held = Files.newByteChannel(path, Set.of(StandardOpenOption.READ, ExtendedOpenOption.NOSHARE_DELETE))) {
                    writer = pool.submit(() -> {
                        try { files.write(path, tile((byte) 42)); }
                        catch (Exception ex) { throw new RuntimeException(ex); }
                    });
                    Thread.sleep(65);
                }
                writer.get();
                require(files.read(path).get(1)[0] == 42, "Windows replacement retry did not recover");
                try (var held = Files.newByteChannel(path, Set.of(StandardOpenOption.READ, ExtendedOpenOption.NOSHARE_DELETE))) {
                    try {
                        files.write(path, tile((byte) 99));
                        throw new AssertionError("Persistent external lock unexpectedly allowed replacement");
                    } catch (AccessDeniedException expected) { }
                }
                require(files.read(path).get(1)[0] == 42, "Failed replacement damaged the previous tile");
            }
            files.delete(path);
            require(!Files.exists(path), "Tile deletion failed");
            try (var entries = Files.list(root)) {
                require(entries.findAny().isEmpty(), "Temporary files leaked");
            }
        } finally {
            pool.shutdownNow();
            try (var entries = Files.list(root)) {
                for (var entry : entries.toList()) Files.deleteIfExists(entry);
            }
            Files.delete(root);
        }
    }

    static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
