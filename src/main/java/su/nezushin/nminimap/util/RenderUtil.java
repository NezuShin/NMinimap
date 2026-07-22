package su.nezushin.nminimap.util;

import com.google.common.collect.Sets;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Color;
import org.bukkit.Material;
import su.nezushin.nminimap.chunks.BlockDataInfo;

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

    public static int getWaterDepth(ChunkSnapshot c, int x, int y, int z, int min) {
        int level = y - 1;
        while (y > min && c.getBlockType(x, y--, z) == Material.WATER) {
        }
        return level - y;
    }

    public static BlockDataInfo getHighestBlockDataAt(ChunkSnapshot c, int x, int z, int minY, int maxY, boolean hasCeiling, boolean skipCeiling, Set<Material> ceilingBlocks) {
        var y = Math.max(getHighestNonTransparentBlockAt(c, x, z, minY, maxY, hasCeiling, skipCeiling, ceilingBlocks), minY);

        var blockData = c.getBlockData(x, y, z);
        var waterDepth = 0;

        if (blockData.getMaterial() == Material.WATER) {
            waterDepth = getWaterDepth(c, x, y, z, y - 12);
        }

        return new BlockDataInfo(blockData.getMapColor(), y, waterDepth);
    }

    public static BlockDataInfo getMostCommonOpaqueBlockBlockData(BlockDataInfo[] infoArray, int x, int z, int scale) {
        if (scale == 1)
            return infoArray[x + (z * 16)];


        Map<Color, Integer> map = new HashMap<>();
        Map<Color, Integer> yLevel = new HashMap<>();
        Map<Color, Integer> waterDepth = new HashMap<>();
        for (var i = 0; i < scale; i++)
            for (var dx = 0; dx <= i; dx++)
                for (var dz = 0; dz <= i; dz++) {
                    var info = infoArray[((x * scale) + dx) + (((z * scale) + dz) * 16)];
                    var color = info.color();

                    map.put(color, map.getOrDefault(color, 0) + 1);
                    yLevel.put(color, info.yLevel());
                    waterDepth.put(color, Math.max(info.waterDepth(), waterDepth.getOrDefault(color, 0)));
                }
        var data = map.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).map(Map.Entry::getKey).findFirst().orElse(null);

        return new BlockDataInfo(data, yLevel.get(data), waterDepth.get(data));
    }

}
