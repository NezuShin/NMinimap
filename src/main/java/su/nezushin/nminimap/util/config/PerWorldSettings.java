package su.nezushin.nminimap.util.config;

import org.bukkit.Material;

import java.util.Set;
import java.util.List;
import java.util.regex.Pattern;

public record PerWorldSettings(
        String id,
        Integer maxY,
        Integer minY,
        Boolean skipCeiling,
        Boolean allowFileCache,
        Set<Material> ceilingBlocks,
        Set<String> worlds,
        Pattern regex,
        WaterRenderingSettings waterRendering,
        SmartDescendSettings smartDescendDefaults,
        List<UndergroundLayer> undergroundLayers
) {

    public boolean matches(String worldName) {
        if (worlds.contains(worldName))
            return true;

        return regex != null && regex.matcher(worldName).matches();
    }
}
