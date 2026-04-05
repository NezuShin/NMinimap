package su.nezushin.nminimap.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import su.nezushin.nminimap.markers.NMapMarker;
import su.nezushin.nminimap.player.NMapPlayer;

import java.util.ArrayList;
import java.util.List;

public class AsyncMarkerRenderEvent extends Event {
    private static final HandlerList handlerList = new HandlerList();

    private NMapPlayer player;

    private List<NMapMarker> markers;

    public AsyncMarkerRenderEvent(NMapPlayer player) {
        super(true);
        this.player = player;
        this.markers = new ArrayList<>();
    }

    public NMapPlayer getPlayer() {
        return player;
    }

    public List<NMapMarker> getMarkers() {
        return markers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }
}
