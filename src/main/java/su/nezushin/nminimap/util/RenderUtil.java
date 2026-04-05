package su.nezushin.nminimap.util;

import com.destroystokyo.paper.MaterialSetTag;
import com.destroystokyo.paper.MaterialTags;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Color;
import org.bukkit.Material;
import su.nezushin.nminimap.chunks.BlockDataInfo;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class RenderUtil {

    public static boolean isTransparent(Material material) {
        if (MaterialTags.GLASS.isTagged(material) || MaterialTags.GLASS_PANES.isTagged(material) || MaterialSetTag.BARS.isTagged(material))
            return true;
        return switch (material) {
            //<editor-fold defaultstate="collapsed" desc="isTransparent">
            // Start generate - Material#isTransparent
            case Material.ACACIA_BUTTON, Material.ACACIA_SAPLING, Material.ACTIVATOR_RAIL, Material.AIR,
                 Material.ALLIUM, Material.ATTACHED_MELON_STEM, Material.ATTACHED_PUMPKIN_STEM, Material.AZURE_BLUET,
                 Material.BARRIER, Material.BEETROOTS, Material.BIRCH_BUTTON, Material.BIRCH_SAPLING,
                 Material.BLACK_CARPET, Material.BLUE_CARPET, Material.BLUE_ORCHID, Material.BROWN_CARPET,
                 Material.BROWN_MUSHROOM, Material.CARROTS, Material.CAVE_AIR, Material.CHORUS_FLOWER,
                 Material.CHORUS_PLANT, Material.COCOA, Material.COMPARATOR, Material.CREEPER_HEAD,
                 Material.CREEPER_WALL_HEAD, Material.CYAN_CARPET, Material.DANDELION, Material.DARK_OAK_BUTTON,
                 Material.DARK_OAK_SAPLING, Material.DEAD_BUSH, Material.DETECTOR_RAIL, Material.DRAGON_HEAD,
                 Material.DRAGON_WALL_HEAD, Material.END_GATEWAY, Material.END_PORTAL, Material.END_ROD, Material.FERN,
                 Material.FIRE, Material.FLOWER_POT, Material.GRAY_CARPET, Material.GREEN_CARPET,
                 Material.JUNGLE_BUTTON, Material.JUNGLE_SAPLING, Material.LADDER, Material.LARGE_FERN, Material.LEVER,
                 Material.LIGHT_BLUE_CARPET, Material.LIGHT_GRAY_CARPET, Material.LILAC, Material.LILY_PAD,
                 Material.LIME_CARPET, Material.MAGENTA_CARPET, Material.MELON_STEM, Material.NETHER_PORTAL,
                 Material.NETHER_WART, Material.OAK_BUTTON, Material.OAK_SAPLING, Material.ORANGE_CARPET,
                 Material.ORANGE_TULIP, Material.OXEYE_DAISY, Material.PEONY, Material.PINK_CARPET, Material.PINK_TULIP,
                 Material.PLAYER_HEAD, Material.PLAYER_WALL_HEAD, Material.POPPY, Material.POTATOES,
                 Material.POTTED_ACACIA_SAPLING, Material.POTTED_ALLIUM, Material.POTTED_AZALEA_BUSH,
                 Material.POTTED_AZURE_BLUET, Material.POTTED_BIRCH_SAPLING, Material.POTTED_BLUE_ORCHID,
                 Material.POTTED_BROWN_MUSHROOM, Material.POTTED_CACTUS, Material.POTTED_DANDELION,
                 Material.POTTED_DARK_OAK_SAPLING, Material.POTTED_DEAD_BUSH, Material.POTTED_FERN,
                 Material.POTTED_FLOWERING_AZALEA_BUSH, Material.POTTED_JUNGLE_SAPLING, Material.POTTED_OAK_SAPLING,
                 Material.POTTED_ORANGE_TULIP, Material.POTTED_OXEYE_DAISY, Material.POTTED_PINK_TULIP,
                 Material.POTTED_POPPY, Material.POTTED_RED_MUSHROOM, Material.POTTED_RED_TULIP,
                 Material.POTTED_SPRUCE_SAPLING, Material.POTTED_WHITE_TULIP, Material.POWERED_RAIL,
                 Material.PUMPKIN_STEM, Material.PURPLE_CARPET, Material.RAIL, Material.REDSTONE_TORCH,
                 Material.REDSTONE_WALL_TORCH, Material.REDSTONE_WIRE, Material.RED_CARPET, Material.RED_MUSHROOM,
                 Material.RED_TULIP, Material.REPEATER, Material.ROSE_BUSH, Material.SHORT_GRASS,
                 Material.SKELETON_SKULL, Material.SKELETON_WALL_SKULL, Material.SNOW, Material.SPRUCE_BUTTON,
                 Material.SPRUCE_SAPLING, Material.STONE_BUTTON, Material.STRUCTURE_VOID, Material.SUGAR_CANE,
                 Material.SUNFLOWER, Material.TALL_GRASS, Material.TORCH, Material.TRIPWIRE, Material.TRIPWIRE_HOOK,
                 Material.VINE, Material.VOID_AIR, Material.WALL_TORCH, Material.WHEAT, Material.WHITE_CARPET,
                 Material.WHITE_TULIP, Material.WITHER_SKELETON_SKULL, Material.WITHER_SKELETON_WALL_SKULL,
                 Material.YELLOW_CARPET, Material.ZOMBIE_HEAD, Material.ZOMBIE_WALL_HEAD ->
                //</editor-fold>
                    true;
            default -> false;
        };
    }


    public static int getHighestNonTransparentBlockAt(ChunkSnapshot c, int x, int z, int minY) {
        var y = c.getHighestBlockYAt(x, z);

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

    public static BlockDataInfo getHighestBlockDataAt(ChunkSnapshot c, int x, int z, int minY) {
        var y = Math.max(getHighestNonTransparentBlockAt(c, x, z, minY), minY);

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
