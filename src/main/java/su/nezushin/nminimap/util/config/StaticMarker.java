package su.nezushin.nminimap.util.config;

import su.nezushin.nminimap.markers.impl.LocationMarker;

/**
 * World is kept as a name and matched at render time, so markers work for worlds loaded after the plugin.
 */
public record StaticMarker(LocationMarker marker, String name, String world) {
}
