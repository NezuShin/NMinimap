package su.nezushin.nminimap.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import su.nezushin.nminimap.api.events.AsyncMarkerRenderEvent;
import su.nezushin.nminimap.markers.impl.LocationMarker;
import su.nezushin.nminimap.markers.impl.PositionMarker;
import su.nezushin.nminimap.util.config.Config;

public class MarkerListener implements Listener {

    @EventHandler
    public void mapOwner(AsyncMarkerRenderEvent e) {
        var player = e.getPlayer();
        if(Config.playerMarker.isEmpty())
            return;

        e.getMarkers().add(new PositionMarker(Config.playerMarker, 0, 0, (int) (((Math.floorMod((int) player.getPlayer().getLocation().getYaw(), 360) / 360.0f) * 256.0) - 127)));
    }

    @EventHandler
    public void anotherPlayers(AsyncMarkerRenderEvent e) {
        var player = e.getPlayer();
        var p = player.getPlayer();
        if(Config.anotherPlayerMarker.isEmpty())
            return;

        p.getWorld().getPlayers().stream().filter(i -> !i.equals(p) && p.getWorld().equals(i.getWorld())).forEach(i -> {
            e.getMarkers().add(new LocationMarker(Config.anotherPlayerMarker, i.getLocation()));
        });
    }
}
