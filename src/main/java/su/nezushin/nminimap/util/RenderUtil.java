package su.nezushin.nminimap.util;

import com.google.common.collect.Sets;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Color;
import org.bukkit.Material;
import su.nezushin.nminimap.chunks.BlockDataInfo;
import su.nezushin.nminimap.util.config.SmartDescendSettings;
import su.nezushin.nminimap.util.config.UndergroundLayer;

import java.util.*;

public class RenderUtil {

    private static final Set<Material> transparent;


    static {
        transparent = Sets.newHashSet();


        //Version-safe transparent material list.
        Sets.newHashSet("BLUE_STAINED_GLASS_PANE", "OXIDIZED_COPPER_BARS",
                "LIME_STAINED_GLASS", "WAXED_WEATHERED_COPPER_BARS", "ORANGE_STAINED_GLASS_PANE",
                "ORANGE_STAINED_GLASS", "WHITE_STAINED_GLASS_PANE", "COPPER_BARS", "RED_STAINED_GLASS_PANE",
                "CYAN_STAINED_GLASS", "LIGHT_BLUE_STAINED_GLASS", "CYAN_STAINED_GLASS_PANE",
                "WEATHERED_COPPER_BARS", "MAGENTA_STAINED_GLASS_PANE", "WAXED_EXPOSED_COPPER_BARS",
                "WHITE_STAINED_GLASS", "PURPLE_STAINED_GLASS_PANE", "BLACK_STAINED_GLASS",
                "BROWN_STAINED_GLASS", "YELLOW_STAINED_GLASS", "BLUE_STAINED_GLASS", "BROWN_STAINED_GLASS_PANE",
                "LIGHT_BLUE_STAINED_GLASS_PANE", "WAXED_OXIDIZED_COPPER_BARS", "IRON_BARS",
                "PURPLE_STAINED_GLASS", "GLASS", "GREEN_STAINED_GLASS_PANE", "BLACK_STAINED_GLASS_PANE",
                "LIGHT_GRAY_STAINED_GLASS", "WAXED_COPPER_BARS", "LIME_STAINED_GLASS_PANE", "GRAY_STAINED_GLASS",
                "GLASS_PANE", "EXPOSED_COPPER_BARS", "PINK_STAINED_GLASS", "GREEN_STAINED_GLASS",
                "GRAY_STAINED_GLASS_PANE", "TINTED_GLASS", "LIGHT_GRAY_STAINED_GLASS_PANE", "RED_STAINED_GLASS",
                "PINK_STAINED_GLASS_PANE", "MAGENTA_STAINED_GLASS", "YELLOW_STAINED_GLASS_PANE",
                "ACACIA_BUTTON", "ACACIA_SAPLING", "ACTIVATOR_RAIL", "AIR",
                "ALLIUM", "ATTACHED_MELON_STEM", "ATTACHED_PUMPKIN_STEM", "AZURE_BLUET",
                "BARRIER", "BEETROOTS", "BIRCH_BUTTON", "BIRCH_SAPLING",
                "BLACK_CARPET", "BLUE_CARPET", "BLUE_ORCHID", "BROWN_CARPET",
                "BROWN_MUSHROOM", "CARROTS", "CAVE_AIR", "CHORUS_FLOWER",
                "CHORUS_PLANT", "COCOA", "COMPARATOR", "CREEPER_HEAD",
                "CREEPER_WALL_HEAD", "CYAN_CARPET", "DANDELION", "DARK_OAK_BUTTON",
                "DARK_OAK_SAPLING", "DEAD_BUSH", "DETECTOR_RAIL", "DRAGON_HEAD",
                "DRAGON_WALL_HEAD", "END_GATEWAY", "END_PORTAL", "END_ROD", "FERN",
                "FIRE", "FLOWER_POT", "GRAY_CARPET", "GREEN_CARPET",
                "JUNGLE_BUTTON", "JUNGLE_SAPLING", "LADDER", "LARGE_FERN", "LEVER",
                "LIGHT_BLUE_CARPET", "LIGHT_GRAY_CARPET", "LILAC", "LILY_PAD",
                "LIME_CARPET", "MAGENTA_CARPET", "MELON_STEM", "NETHER_PORTAL",
                "NETHER_WART", "OAK_BUTTON", "OAK_SAPLING", "ORANGE_CARPET",
                "ORANGE_TULIP", "OXEYE_DAISY", "PEONY", "PINK_CARPET", "PINK_TULIP",
                "PLAYER_HEAD", "PLAYER_WALL_HEAD", "POPPY", "POTATOES",
                "POTTED_ACACIA_SAPLING", "POTTED_ALLIUM", "POTTED_AZALEA_BUSH",
                "POTTED_AZURE_BLUET", "POTTED_BIRCH_SAPLING", "POTTED_BLUE_ORCHID",
                "POTTED_BROWN_MUSHROOM", "POTTED_CACTUS", "POTTED_DANDELION",
                "POTTED_DARK_OAK_SAPLING", "POTTED_DEAD_BUSH", "POTTED_FERN",
                "POTTED_FLOWERING_AZALEA_BUSH", "POTTED_JUNGLE_SAPLING", "POTTED_OAK_SAPLING",
                "POTTED_ORANGE_TULIP", "POTTED_OXEYE_DAISY", "POTTED_PINK_TULIP",
                "POTTED_POPPY", "POTTED_RED_MUSHROOM", "POTTED_RED_TULIP",
                "POTTED_SPRUCE_SAPLING", "POTTED_WHITE_TULIP", "POWERED_RAIL",
                "PUMPKIN_STEM", "PURPLE_CARPET", "RAIL", "REDSTONE_TORCH",
                "REDSTONE_WALL_TORCH", "REDSTONE_WIRE", "RED_CARPET", "RED_MUSHROOM",
                "RED_TULIP", "REPEATER", "ROSE_BUSH", "SHORT_GRASS",
                "SKELETON_SKULL", "SKELETON_WALL_SKULL", "SNOW", "SPRUCE_BUTTON",
                "SPRUCE_SAPLING", "STONE_BUTTON", "STRUCTURE_VOID", "SUGAR_CANE",
                "SUNFLOWER", "TALL_GRASS", "TORCH", "TRIPWIRE", "TRIPWIRE_HOOK",
                "VINE", "VOID_AIR", "WALL_TORCH", "WHEAT", "WHITE_CARPET",
                "WHITE_TULIP", "WITHER_SKELETON_SKULL", "WITHER_SKELETON_WALL_SKULL",
                "YELLOW_CARPET", "ZOMBIE_HEAD", "ZOMBIE_WALL_HEAD").forEach(i -> {
            try {
                transparent.add(Material.valueOf(i));
            } catch (Exception ex) {
                //:(
            }
        });
    }

    public static boolean isTransparent(Material material) {
        return transparent.contains(material);
    }


    public static int getHighestNonTransparentBlockAt(ChunkSnapshot c, int x, int z, int minY, int maxY, boolean hasCeiling, boolean skipCeiling, Set<Material> ceilingBlocks) {
        var y = c.getHighestBlockYAt(x, z);

        y = Math.min(y, maxY);

        if (skipCeiling && hasCeiling && ceilingBlocks.contains(c.getBlockType(x, y, z)))//skip ceiling if needed
            while (y > minY && !isTransparent(c.getBlockType(x, y, z))) {
                y--;
            }

        while (y > minY && isTransparent(c.getBlockType(x, y, z))) {
            y--;
        }
        return y;
    }

    public static int getWaterDepth(ChunkSnapshot c, int x, int y, int z, int min, int minWorldY) {
        int level = y - 1;
        while (y > min && y > minWorldY && c.getBlockType(x, y--, z) == Material.WATER) {
        }
        return level - y;
    }

    public static BlockDataInfo getHighestBlockDataAt(ChunkSnapshot c, int x, int z, int minY, int maxY, boolean hasCeiling,
                                                       boolean skipCeiling, Set<Material> ceilingBlocks, int maxWaterDepth) {
        var y = Math.max(getHighestNonTransparentBlockAt(c, x, z, minY, maxY, hasCeiling, skipCeiling, ceilingBlocks), minY);

        var blockData = c.getBlockData(x, y, z);
        var waterDepth = 0;

        if (blockData.getMaterial() == Material.WATER) {
            waterDepth = getWaterDepth(c, x, y, z, y - Math.max(1, maxWaterDepth), minY);
        }

        var bottomColor = blockData.getMapColor();
        if (waterDepth > 0) {
            int bottomY = Math.max(minY, y - waterDepth);
            bottomColor = c.getBlockData(x, bottomY, z).getMapColor();
        }
        return new BlockDataInfo(blockData.getMapColor(), bottomColor, y, waterDepth);
    }

    public static BlockDataInfo getUndergroundBlockDataAt(ChunkSnapshot c, int x, int z, int minY, int worldMaxY,
                                                           boolean hasCeiling, boolean skipCeiling,
                                                           Set<Material> ceilingBlocks, UndergroundLayer layer, Integer regionFloorY) {
        return getUndergroundBlockDataAt(c, x, z, minY, worldMaxY, hasCeiling, skipCeiling,
                ceilingBlocks, layer, regionFloorY, null);
    }

    public static BlockDataInfo getUndergroundBlockDataAt(ChunkSnapshot c, int x, int z, int minY, int worldMaxY,
                                                           boolean hasCeiling, boolean skipCeiling,
                                                           Set<Material> ceilingBlocks, UndergroundLayer layer, Integer regionFloorY,
                                                           ConnectedCaveCheck connectedCaveCheck) {
        int startY = Math.min(worldMaxY, layer.renderFromY());
        var smart = layer.smartDescend();
        if (smart == null || !smart.enabled()) {
            return getHighestBlockDataAt(c, x, z, minY, startY, hasCeiling, skipCeiling, ceilingBlocks,
                    layer.waterRendering().maxSampledDepth());
        }

        if (smart.useRegionFloor() && regionFloorY == null)
            return missingCaveSurface(c, x, z, minY, worldMaxY, hasCeiling, skipCeiling, ceilingBlocks, layer);

        int searchMinY = Math.max(minY, smart.useRegionFloor() ? regionFloorY : smart.minY());
        int openingY = isOpening(c, x, startY, z, searchMinY, startY, smart, connectedCaveCheck)
                ? startY : Integer.MIN_VALUE;
        if (openingY == Integer.MIN_VALUE && smart.noOpeningMode() == SmartDescendSettings.NoOpeningMode.DESCEND) {
            for (int y = startY - 1; y >= searchMinY; y--) {
                if (isOpening(c, x, y, z, searchMinY, startY, smart, connectedCaveCheck)) {
                    openingY = y;
                    break;
                }
            }
        }

        if (openingY == Integer.MIN_VALUE) {
            if (smart.noOpeningMode() == SmartDescendSettings.NoOpeningMode.DESCEND)
                return missingCaveSurface(c, x, z, minY, worldMaxY, hasCeiling, skipCeiling, ceilingBlocks, layer);
            return getHighestBlockDataAt(c, x, z, minY, startY, hasCeiling, skipCeiling, ceilingBlocks,
                    layer.waterRendering().maxSampledDepth());
        }

        Material fluid = c.getBlockType(x, openingY, z);
        int fluidDepth = 0;
        if (fluid == Material.WATER) {
            int fluidY = openingY;
            while (fluidY >= searchMinY && c.getBlockType(x, fluidY, z) == Material.WATER) {
                fluidDepth++;
                fluidY--;
            }
        }
        int floorY = openingY;
        while (floorY >= searchMinY && smart.transparentBlocks().contains(c.getBlockType(x, floorY, z)))
            floorY--;
        floorY = Math.max(floorY, searchMinY);
        var floor = c.getBlockData(x, floorY, z);
        int waterDepth = Math.min(fluidDepth, layer.waterRendering().maxSampledDepth());
        Color visible = fluid == Material.WATER ? c.getBlockData(x, openingY, z).getMapColor() : floor.getMapColor();
        return new BlockDataInfo(visible, floor.getMapColor(), floorY, waterDepth);
    }

    private static boolean isOpening(ChunkSnapshot c, int x, int y, int z, int minY, int maxY,
                                     SmartDescendSettings smart, ConnectedCaveCheck connectedCaveCheck) {
        if (!smart.transparentBlocks().contains(c.getBlockType(x, y, z)))
            return false;
        for (int i = 1; i < smart.minOpenHeight(); i++) {
            int checkY = y - i;
            if (checkY < minY || !smart.transparentBlocks().contains(c.getBlockType(x, checkY, z)))
                return false;
        }
        return smart.minConnectedColumns() <= 1 || connectedCaveCheck != null
                && connectedCaveCheck.hasConnectedColumns(x, y, z, minY, maxY, smart);
    }

    private static BlockDataInfo missingCaveSurface(ChunkSnapshot c, int x, int z, int minY, int worldMaxY,
                                                     boolean hasCeiling, boolean skipCeiling, Set<Material> ceilingBlocks,
                                                     UndergroundLayer layer) {
        var surface = getHighestBlockDataAt(c, x, z, minY, worldMaxY, hasCeiling, skipCeiling, ceilingBlocks,
                layer.waterRendering().maxSampledDepth());
        return new BlockDataInfo(surface.color(), surface.bottomColor(), surface.yLevel(), surface.waterDepth(), true);
    }

    public static BlockDataInfo getMostCommonOpaqueBlockBlockData(BlockDataInfo[] infoArray, int x, int z, int scale) {
        if (scale == 1)
            return infoArray[x + (z * 16)];


        Map<SampleKey, Integer> map = new HashMap<>();
        Map<SampleKey, Integer> yLevel = new HashMap<>();
        Map<SampleKey, Integer> waterDepth = new HashMap<>();
        for (var dx = 0; dx < scale; dx++)
                for (var dz = 0; dz < scale; dz++) {
                    var info = infoArray[((x * scale) + dx) + (((z * scale) + dz) * 16)];
                    var key = new SampleKey(info.color(), info.bottomColor(), info.waterDepth() > 0, info.missingCave());

                    map.put(key, map.getOrDefault(key, 0) + 1);
                    yLevel.put(key, info.yLevel());
                    waterDepth.put(key, Math.max(info.waterDepth(), waterDepth.getOrDefault(key, 0)));
                }
        var data = map.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).map(Map.Entry::getKey).findFirst().orElse(null);

        return new BlockDataInfo(data.color(), data.bottomColor(), yLevel.get(data), waterDepth.get(data), data.missingCave());
    }

    private record SampleKey(Color color, Color bottomColor, boolean water, boolean missingCave) {
    }

}
