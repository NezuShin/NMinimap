package su.nezushin.nminimap;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import su.nezushin.nminimap.chunks.BlockDataInfo;
import su.nezushin.nminimap.util.RenderUtil;
import su.nezushin.nminimap.util.ConnectedCaveCheck;
import su.nezushin.nminimap.util.config.SmartDescendSettings;
import su.nezushin.nminimap.util.config.UndergroundLayer;
import su.nezushin.nminimap.util.config.WaterRenderingSettings;

public class UndergroundRenderCheck {
    private static final WaterRenderingSettings WATER = new WaterRenderingSettings(
            WaterRenderingSettings.Mode.VANILLA, WaterRenderingSettings.Scope.REGION, 0.35f, 0.1f, 0.65f, 12,
            Color.BLUE, WaterRenderingSettings.ColorSource.WATER, 12, 0f);

    private static ChunkSnapshot snapshot(boolean cave) {
        return snapshot(cave, false);
    }

    private static ChunkSnapshot snapshot(boolean cave, boolean isolated) {
        return (ChunkSnapshot) Proxy.newProxyInstance(ChunkSnapshot.class.getClassLoader(),
                new Class<?>[]{ChunkSnapshot.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getHighestBlockYAt")) return 80;
                    int x = (int) args[0];
                    int y = (int) args[1];
                    int z = (int) args[2];
                    Material material = y == 80 ? Material.GRASS_BLOCK
                            : cave && (!isolated || x == 0 && z == 0) && (y == 25 || y == 24) ? Material.CAVE_AIR : Material.STONE;
                    if (method.getName().equals("getBlockType")) return material;
                    if (method.getName().equals("getBlockData")) return blockData(material);
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private static BlockData blockData(Material material) {
        return (BlockData) Proxy.newProxyInstance(BlockData.class.getClassLoader(),
                new Class<?>[]{BlockData.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getMaterial")) return material;
                    if (method.getName().equals("getMapColor")) return material == Material.GRASS_BLOCK ? Color.GREEN
                            : material == Material.WATER ? Color.BLUE : Color.GRAY;
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private static ChunkSnapshot waterAtRegionFloor() {
        return (ChunkSnapshot) Proxy.newProxyInstance(ChunkSnapshot.class.getClassLoader(),
                new Class<?>[]{ChunkSnapshot.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getHighestBlockYAt")) return 80;
                    int y = (int) args[1];
                    Material material = y == 80 ? Material.GRASS_BLOCK
                            : y == 25 || y == 24 ? Material.WATER : Material.STONE;
                    if (method.getName().equals("getBlockType")) return material;
                    if (method.getName().equals("getBlockData")) return blockData(material);
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private static ChunkSnapshot snapshotWithoutFloor() {
        return (ChunkSnapshot) Proxy.newProxyInstance(ChunkSnapshot.class.getClassLoader(),
                new Class<?>[]{ChunkSnapshot.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getHighestBlockYAt")) return 80;
                    int y = (int) args[1];
                    Material material = y == 80 ? Material.GRASS_BLOCK
                            : y >= 10 && y <= 25 ? Material.CAVE_AIR : Material.STONE;
                    if (method.getName().equals("getBlockType")) return material;
                    if (method.getName().equals("getBlockData")) return blockData(material);
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private static BlockDataInfo render(boolean cave, int regionFloor) {
        var smart = new SmartDescendSettings(Integer.MIN_VALUE, true,
                SmartDescendSettings.NoOpeningMode.DESCEND, Set.of(Material.CAVE_AIR), 2, 1);
        var layer = new UndergroundLayer("test", List.of("region"), 50, 1, 0.5f, smart, WATER);
        return RenderUtil.getUndergroundBlockDataAt(snapshot(cave), 0, 0, -64, 100,
                false, false, Set.of(), layer, regionFloor);
    }

    public static void main(String[] args) {
        var cave = render(true, 10);
        if (cave.yLevel() != 23 || !cave.color().equals(Color.GRAY) || cave.missingCave())
            throw new AssertionError("Cave floor was not selected: " + cave);
        var waterSmart = new SmartDescendSettings(Integer.MIN_VALUE, true,
                SmartDescendSettings.NoOpeningMode.DESCEND, Set.of(Material.WATER), 2, 1);
        var waterLayer = new UndergroundLayer("water", List.of("region"), 25, 1, 0.5f, waterSmart, WATER);
        var waterFloor = RenderUtil.getUndergroundBlockDataAt(waterAtRegionFloor(), 0, 0, -64, 100,
                false, false, Set.of(), waterLayer, 24);
        if (waterFloor.missingCave() || waterFloor.yLevel() != 23 || waterFloor.waterDepth() != 2
                || !waterFloor.bottomColor().equals(Color.GRAY))
            throw new AssertionError("Solid bottom just below the region floor was rejected: " + waterFloor);
        var empty = render(false, 10);
        if (empty.yLevel() != 80 || !empty.color().equals(Color.GREEN) || !empty.missingCave())
            throw new AssertionError("Missing cave did not fall back to surface: " + empty);
        var bounded = render(true, 30);
        if (bounded.yLevel() != 80 || !bounded.color().equals(Color.GREEN) || !bounded.missingCave())
            throw new AssertionError("Region floor was not respected: " + bounded);
        var floorlessSettings = new SmartDescendSettings(10, false,
                SmartDescendSettings.NoOpeningMode.DESCEND, Set.of(Material.CAVE_AIR), 2, 1);
        var floorlessLayer = new UndergroundLayer("floorless", List.of("region"), 25, 1, 0.5f, floorlessSettings, WATER);
        var floorlessResult = RenderUtil.getUndergroundBlockDataAt(snapshotWithoutFloor(), 0, 0, -64, 100,
                false, false, Set.of(), floorlessLayer, null);
        if (!floorlessResult.missingCave() || floorlessResult.yLevel() != 80)
            throw new AssertionError("Transparent search boundary was rendered as a floor: " + floorlessResult);
        var fixedSettings = new SmartDescendSettings(10, false,
                SmartDescendSettings.NoOpeningMode.FIXED, Set.of(Material.CAVE_AIR), 2, 1);
        var fixedLayer = new UndergroundLayer("fixed", List.of("region"), 50, 1, 0.5f, fixedSettings, WATER);
        var fixedResult = RenderUtil.getUndergroundBlockDataAt(snapshot(true), 0, 0, -64, 100,
                false, false, Set.of(), fixedLayer, null);
        if (fixedResult.yLevel() != 50 || fixedResult.missingCave())
            throw new AssertionError("Fixed mode searched below its Y slice: " + fixedResult);
        var connected = new SmartDescendSettings(10, false,
                SmartDescendSettings.NoOpeningMode.DESCEND, Set.of(Material.CAVE_AIR), 2, 3);
        var connectedLayer = new UndergroundLayer("connected", List.of("region"), 50, 1, 0.5f, connected, WATER);
        var openSnapshot = snapshot(true);
        var openCheck = new ConnectedCaveCheck(Map.of(ConnectedCaveCheck.chunkKey(0, 0), openSnapshot));
        var connectedResult = RenderUtil.getUndergroundBlockDataAt(openSnapshot, 0, 0, -64, 100,
                false, false, Set.of(), connectedLayer, null, openCheck);
        if (connectedResult.missingCave() || connectedResult.yLevel() != 23)
            throw new AssertionError("Connected cave was rejected: " + connectedResult);
        var northCheck = new ConnectedCaveCheck(Map.of(ConnectedCaveCheck.chunkKey(0, 0), openSnapshot,
                ConnectedCaveCheck.chunkKey(0, -1), openSnapshot)).forChunk(0, -1);
        if (!northCheck.hasConnectedColumns(0, 25, 15, 10, 50, connected))
            throw new AssertionError("North neighbor was checked with the wrong chunk origin");
        var isolatedSnapshot = snapshot(true, true);
        var isolatedCheck = new ConnectedCaveCheck(Map.of(ConnectedCaveCheck.chunkKey(0, 0), isolatedSnapshot));
        var isolatedResult = RenderUtil.getUndergroundBlockDataAt(isolatedSnapshot, 0, 0, -64, 100,
                false, false, Set.of(), connectedLayer, null, isolatedCheck);
        if (!isolatedResult.missingCave() || isolatedResult.yLevel() != 80)
            throw new AssertionError("Isolated pocket was accepted: " + isolatedResult);
        System.out.println("Render checks passed");
    }
}
