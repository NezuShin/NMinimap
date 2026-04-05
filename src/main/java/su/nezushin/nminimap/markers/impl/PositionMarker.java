package su.nezushin.nminimap.markers.impl;

import org.bukkit.Location;
import su.nezushin.nminimap.markers.NMapMarker;
import su.nezushin.nminimap.player.NMapPlayer;
/**
 * Marker with relative position on map
 */
public class PositionMarker extends NMapMarker {

    private int x, z, rotation;

    public PositionMarker(String icon, int x, int z, int rotation) {
        super(icon);
        this.x = x;
        this.z = z;
        this.rotation = rotation;
    }

    @Override
    public int[] getPositionOnMap(NMapPlayer player) {
        return new int[]{x, z, rotation};
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getZ() {
        return z;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public int getRotation() {
        return rotation;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
    }
}
