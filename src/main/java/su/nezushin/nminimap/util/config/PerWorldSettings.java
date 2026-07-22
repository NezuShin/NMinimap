package su.nezushin.nminimap.util.config;

import org.bukkit.Material;

import java.util.Set;
import java.util.regex.Pattern;

public record PerWorldSettings(
        String id,
        Integer maxY,
        Integer minY,
        Boolean skipCeiling,
        Set<Material> ceilingBlocks,
        Set<String> worlds,
        Pattern regex
) {

    public boolean matches(String worldName) {
        if (worlds.contains(worldName))
            return true;

        return regex != null && regex.matcher(worldName).matches();
    }
}
