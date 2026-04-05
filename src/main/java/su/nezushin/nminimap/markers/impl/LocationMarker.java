package su.nezushin.nminimap.markers.impl;

import org.bukkit.Location;
import su.nezushin.nminimap.markers.NMapMarker;
import su.nezushin.nminimap.player.NMapPlayer;

/**
 * Marker with strictly defined location in world
 */
public class LocationMarker extends NMapMarker {

    Location location;

    public LocationMarker(String icon, Location location) {
        super(icon);
        this.location = location;
    }

    @Override
    public int[] getPositionOnMap(NMapPlayer player) {
        var ploc = player.getPlayer().getLocation();
        return new int[]{
                ((location.getBlockX() - ploc.getBlockX()) * 2) / player.getScale(),
                ((location.getBlockZ() - ploc.getBlockZ()) * 2) / player.getScale(),
                (int) (((Math.floorMod((int) location.getYaw(), 360) / 360.0f) * 256.0) - 127)};
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }
}
