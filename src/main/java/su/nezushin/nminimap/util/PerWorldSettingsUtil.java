package su.nezushin.nminimap.util;

import org.bukkit.Material;
import org.bukkit.World;
import su.nezushin.nminimap.util.config.Config;
import su.nezushin.nminimap.util.config.PerWorldSettings;

import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PerWorldSettingsUtil {

    private static final Set<Material> DEFAULT_CEILING_BLOCKS = EnumSet.of(Material.BEDROCK);

    private static final Map<String, Optional<PerWorldSettings>> cache = new ConcurrentHashMap<>();

    public static PerWorldSettings find(World world) {
        return find(world.getName());
    }

    public static PerWorldSettings find(String worldName) {
        return cache.computeIfAbsent(worldName, name -> {
            for (var settings : Config.perWorldSettings) {
                if (settings.matches(name))
                    return Optional.of(settings);
            }
            return Optional.empty();
        }).orElse(null);
    }

    public static void clearCache() {
        cache.clear();
    }

    public static int getMinY(World world) {
        var settings = find(world);
        if (settings != null && settings.minY() != null)
            return settings.minY();
        return world.getMinHeight();
    }

    public static int getMaxY(World world) {
        var settings = find(world);
        if (settings != null && settings.maxY() != null)
            return settings.maxY();
        return Integer.MAX_VALUE;
    }

    public static boolean getSkipCeiling(World world) {
        var settings = find(world);
        if (settings != null && settings.skipCeiling() != null)
            return settings.skipCeiling();
        return Config.skipCeiling;
    }

    public static Set<Material> getCeilingBlocks(World world) {
        var settings = find(world);
        if (settings != null && settings.ceilingBlocks() != null)
            return settings.ceilingBlocks();
        return DEFAULT_CEILING_BLOCKS;
    }
}
