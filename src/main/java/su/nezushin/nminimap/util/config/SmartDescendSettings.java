package su.nezushin.nminimap.util.config;

import org.bukkit.Material;

import java.util.Set;

public record SmartDescendSettings(
        boolean enabled,
        int minY,
        boolean useRegionFloor,
        NoOpeningMode noOpeningMode,
        Set<Material> transparentBlocks,
        int minOpenHeight,
        int minConnectedColumns
) {
    public enum NoOpeningMode { FIXED, DESCEND }

    public SmartDescendSettings {
        transparentBlocks = Set.copyOf(transparentBlocks);
        minOpenHeight = Math.max(1, minOpenHeight);
        minConnectedColumns = Math.max(1, minConnectedColumns);
    }
}
