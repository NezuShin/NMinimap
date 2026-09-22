package su.nezushin.nminimap.chunks;

import org.bukkit.Color;

public record BlockDataInfo(Color color, Color bottomColor, int yLevel, int waterDepth, boolean missingCave) {

    public BlockDataInfo(Color color, int yLevel, int waterDepth) {
        this(color, color, yLevel, waterDepth, false);
    }

    public BlockDataInfo(Color color, Color bottomColor, int yLevel, int waterDepth) {
        this(color, bottomColor, yLevel, waterDepth, false);
    }

}
