package su.nezushin.nminimap.chunks;

import org.bukkit.Material;
import org.bukkit.World;
import su.nezushin.nminimap.util.PerWorldSettingsUtil;
import su.nezushin.nminimap.util.config.Config;
import su.nezushin.nminimap.util.config.UndergroundLayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * The scan inputs that produced a tile. Stored on the tile and in the disk cache so a config
 * change (max-layers, min-air-gap, layer-skip, Y range, ceiling skip) forces a re-render.
 */
public record TileBakeSettings(int maxLayers, int minAirGap, int minY, int maxY,
                               boolean skipCeiling, boolean hasCeiling,
                               List<String> skipBlocks, List<String> ceilingBlocks) {

    public TileBakeSettings {
        skipBlocks = List.copyOf(skipBlocks);
        ceilingBlocks = List.copyOf(ceilingBlocks);
    }

    public static TileBakeSettings current(World world, UndergroundLayer layer) {
        int maxY = PerWorldSettingsUtil.getMaxY(world);
        if (layer != null)
            maxY = Math.min(maxY, layer.renderFromY());
        return new TileBakeSettings(
                Config.multiLayerMaxLayers,
                Config.multiLayerMinAirGap,
                PerWorldSettingsUtil.getMinY(world),
                maxY,
                PerWorldSettingsUtil.getSkipCeiling(world),
                world.hasCeiling(),
                names(Config.multiLayerSkipBlocks),
                names(PerWorldSettingsUtil.getCeilingBlocks(world)));
    }

    private static List<String> names(Set<Material> materials) {
        if (materials == null || materials.isEmpty())
            return List.of();
        var names = new ArrayList<String>(materials.size());
        for (var material : materials)
            names.add(material.name());
        Collections.sort(names);
        return names;
    }
}
