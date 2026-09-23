package su.nezushin.nminimap.util.config;

import org.bukkit.Color;

public record WaterRenderingSettings(
        Mode mode,
        float opacity,
        float minOpacity,
        float maxOpacity,
        int depthForMaxOpacity,
        Color tint,
        ColorSource colorSource,
        int maxSampledDepth,
        float underwaterDarken
) {
    public enum Mode { VANILLA, FIXED, DEPTH, DISABLED }

    public enum ColorSource { WATER, BOTTOM }

    public WaterRenderingSettings {
        opacity = clamp(opacity);
        minOpacity = clamp(minOpacity);
        maxOpacity = clamp(maxOpacity);
        if (maxOpacity < minOpacity)
            maxOpacity = minOpacity;
        depthForMaxOpacity = Math.max(1, depthForMaxOpacity);
        maxSampledDepth = Math.max(1, maxSampledDepth);
        underwaterDarken = clamp(underwaterDarken);
    }

    public float opacityForDepth(int depth) {
        if (mode == Mode.FIXED)
            return opacity;
        if (mode != Mode.DEPTH)
            return 0f;
        float progress = Math.min(Math.max(depth, 0), depthForMaxOpacity) / (float) depthForMaxOpacity;
        return minOpacity + (maxOpacity - minOpacity) * progress;
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
