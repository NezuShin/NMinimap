package su.nezushin.nminimap.util;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Color;
import org.bukkit.Material;
import su.nezushin.nminimap.chunks.BlockDataInfo;
import su.nezushin.nminimap.chunks.LayerData;

import java.util.*;

public class RenderUtil {

    private static final Set<Material> transparent;

    /**
     * Drawn with water depth shading, like vanilla maps do.
     */
    private static final Set<Material> waterLike = EnumSet.of(Material.WATER, Material.BUBBLE_COLUMN,
            Material.KELP, Material.KELP_PLANT, Material.SEAGRASS, Material.TALL_SEAGRASS);

    /**
     * Indexed by {@link Material#ordinal()}, built by {@link #reloadPassable(Set)}.
     */
    private static volatile boolean[] passable = new boolean[0];

    /**
     * Placeholder when a column has no block for a given layer index.
     */
    public static final BlockDataInfo EMPTY_BLOCK = new BlockDataInfo(Color.fromRGB(0), LayerData.NO_LAYER, 0,
            LayerData.NO_LAYER, LayerData.NO_LAYER);

    static {
        transparent = EnumSet.noneOf(Material.class);

        // Map color NONE: look through these when picking the surface color (glass, air, rails, ...).
        // Snow/carpets/flowers have real map colors and must stay drawable.
        for (var material : Material.values()) {
            if (material.isLegacy() || !material.isBlock())
                continue;
            try {
                if (material.createBlockData().getMapColor().asRGB() == 0)
                    transparent.add(material);
            } catch (Exception ignored) {
                // Some materials cannot create block data on this server build
            }
        }
    }

    public static boolean isTransparent(Material material) {
        return transparent.contains(material);
    }

    private static boolean isLiquid(Material material) {
        return material == Material.LAVA || waterLike.contains(material);
    }

    /**
     * Rebuild the passable set: transparent blocks, liquids, {@code layerSkipBlocks} and every block without
     * a solid collision box (lichen, vines, roots, sculk veins, moss carpet, cobwebs, signs, ...).
     */
    public static void reloadPassable(Set<Material> layerSkipBlocks) {
        var materials = Material.values();
        var table = new boolean[materials.length];
        for (var material : materials) {
            if (material.isLegacy() || !material.isBlock())
                continue;
            table[material.ordinal()] = isTransparent(material) || isLiquid(material)
                    || (layerSkipBlocks != null && layerSkipBlocks.contains(material)) || !material.isSolid();
        }
        passable = table;
    }

    /**
     * Blocks a player can occupy: they never split a column into separate layers.
     */
    public static boolean isPassable(Material material) {
        var table = passable;
        int ordinal = material.ordinal();
        if (ordinal < table.length)
            return table[ordinal];
        return isTransparent(material) || isLiquid(material) || !material.isSolid();
    }

    public static int getWaterDepth(ChunkSnapshot c, int x, int y, int z, int min, int minWorldY) {
        int level = y - 1;
        while (y > min && y > minWorldY && waterLike.contains(c.getBlockType(x, y--, z))) {
        }
        return level - y;
    }

    /**
     * Highest Y to start scanning from. {@link ChunkSnapshot#getHighestBlockYAt} uses the
     * MOTION_BLOCKING heightmap (Paper/Spigot), which omits non-solid cover like snow layers,
     * carpets, and flowers — walk up through non-air so those still get drawn.
     */
    private static int topSurfaceY(ChunkSnapshot c, int x, int z, int maxY) {
        int y = Math.min(c.getHighestBlockYAt(x, z), maxY);
        while (y < maxY) {
            var above = c.getBlockType(x, y + 1, z);
            if (above.isAir())
                break;
            y++;
        }
        return y;
    }

    /**
     * Open spaces of a column from the top down. A space is a run of passable blocks (air, liquids, foliage, ...)
     * resting on a solid floor; runs thinner than {@code minAirGap} without liquid in them merge into the solid around them.
     */
    public static List<BlockDataInfo> getBlockLayersAt(ChunkSnapshot c, int x, int z, int minY, int maxY,
                                                       boolean hasCeiling, boolean skipCeiling, Set<Material> ceilingBlocks,
                                                       int maxLayers, int minAirGap) {
        List<BlockDataInfo> layers = new ArrayList<>();
        int y = topSurfaceY(c, x, z, maxY);
        if (y < minY)
            return layers;

        boolean sky = true;
        if (skipCeiling && hasCeiling && ceilingBlocks != null && ceilingBlocks.contains(c.getBlockType(x, y, z))) {
            while (y >= minY && !isPassable(c.getBlockType(x, y, z))) {
                y--;
            }
            sky = false;
        }

        int topY = sky ? LayerData.SKY : y;
        boolean first = true;
        while (layers.size() < maxLayers && y >= minY) {
            int firstDrawnY = Integer.MIN_VALUE;
            boolean hasLiquid = false;
            for (; y >= minY; y--) {
                var material = c.getBlockType(x, y, z);
                if (!isPassable(material))
                    break;
                if (sky && firstDrawnY == Integer.MIN_VALUE && !isTransparent(material))
                    firstDrawnY = y;
                hasLiquid |= isLiquid(material);
            }

            // floorY = minY - 1 when the space reaches the bottom of the render range; it's kept if something in it is drawn
            int floorY = y;
            if (first || hasLiquid || topY - floorY >= minAirGap) {
                int drawY = sky
                        ? (firstDrawnY != Integer.MIN_VALUE ? firstDrawnY : floorY)
                        : liquidSurfaceY(c, x, z, floorY, topY);
                if (drawY >= minY)
                    layers.add(blockDataAt(c, x, z, drawY, minY, floorY, topY));
            }

            while (y >= minY && !isPassable(c.getBlockType(x, y, z))) {
                y--;
            }
            topY = y;
            sky = false;
            first = false;
        }

        return layers;
    }

    /**
     * Top of the liquid resting on the floor (lichen, carpets, ... under it are skipped), or the floor itself.
     * Vines and roots hanging into a dry cave are never drawn.
     */
    private static int liquidSurfaceY(ChunkSnapshot c, int x, int z, int floorY, int topY) {
        int surfaceY = floorY;
        for (int y = floorY + 1; y <= topY; y++) {
            var material = c.getBlockType(x, y, z);
            if (isLiquid(material))
                surfaceY = y;
            else if (surfaceY != floorY || isTransparent(material))
                break;
        }
        return surfaceY;
    }

    private static BlockDataInfo blockDataAt(ChunkSnapshot c, int x, int z, int drawY, int minY, int floorY, int topY) {
        var blockData = c.getBlockData(x, drawY, z);
        var waterDepth = 0;
        if (waterLike.contains(blockData.getMaterial())) {
            waterDepth = getWaterDepth(c, x, drawY, z, drawY - 12, minY);
        }
        return new BlockDataInfo(blockData.getMapColor(), drawY, waterDepth, floorY, topY);
    }

    /**
     * Most common color of a {@code scale}² cell for the space {@code anchor}. Every column votes with its own space
     * that overlaps {@code anchor} the most vertically; columns solid at that height don't vote.
     * Floor and top are taken from {@code anchor}.
     */
    public static BlockDataInfo getMostCommonBlockData(List<BlockDataInfo>[] columns, int x, int z, int scale, BlockDataInfo anchor) {
        Map<Color, Integer> map = new HashMap<>();
        Map<Color, Integer> drawY = new HashMap<>();
        Map<Color, Integer> waterDepth = new HashMap<>();
        for (var i = 0; i < scale; i++)
            for (var dx = 0; dx <= i; dx++)
                for (var dz = 0; dz <= i; dz++) {
                    var info = overlapping(columns[((x * scale) + dx) + (((z * scale) + dz) * 16)], anchor);
                    if (info == null)
                        continue;
                    var color = info.color();

                    map.put(color, map.getOrDefault(color, 0) + 1);
                    drawY.put(color, info.drawY());
                    waterDepth.put(color, Math.max(info.waterDepth(), waterDepth.getOrDefault(color, 0)));
                }
        var data = map.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).map(Map.Entry::getKey).findFirst().orElse(null);
        if (data == null)
            return EMPTY_BLOCK;

        return new BlockDataInfo(data, drawY.get(data), waterDepth.get(data), anchor.floorY(), anchor.topY());
    }

    private static BlockDataInfo overlapping(List<BlockDataInfo> layers, BlockDataInfo anchor) {
        if (layers == null)
            return null;
        BlockDataInfo best = null;
        int bestOverlap = 0;
        for (var layer : layers) {
            int overlap = Math.min(layer.topY(), anchor.topY()) - Math.max(layer.floorY(), anchor.floorY()) + 1;
            if (overlap > bestOverlap) {
                bestOverlap = overlap;
                best = layer;
            }
        }
        return best;
    }

}
