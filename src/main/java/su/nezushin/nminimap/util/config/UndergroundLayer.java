package su.nezushin.nminimap.util.config;

import java.util.List;

public record UndergroundLayer(String id, List<String> wgRegions, int renderFromY, int priority, float darken) {
}
