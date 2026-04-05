package su.nezushin.nminimap.markers;

import org.bukkit.Location;
import su.nezushin.nminimap.player.NMapPlayer;

public abstract class NMapMarker {

    protected String icon;

    public NMapMarker(String icon) {
        this.icon = icon;
    }

    public abstract int[] getPositionOnMap(NMapPlayer player);

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }
}

