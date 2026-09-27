package su.nezushin.nminimap.util.config;

import java.util.List;

public record FrameDefinition(
        String name,
        boolean usePermission,
        List<FrameLayerDefinition> squareLayers,
        List<FrameLayerDefinition> roundLayers
) {
}
