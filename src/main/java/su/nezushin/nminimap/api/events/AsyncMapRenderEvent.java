package su.nezushin.nminimap.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import su.nezushin.nminimap.player.NMapPlayer;

public class AsyncMapRenderEvent extends Event {
    private static final HandlerList handlerList = new HandlerList();

    private NMapPlayer player;

    private byte[] mapData;

    public AsyncMapRenderEvent(NMapPlayer player, byte[] mapData) {
        super(true);
        this.player = player;
        this.mapData = mapData;
    }

    public NMapPlayer getPlayer() {
        return player;
    }

    public byte[] getMapData() {
        return mapData;
    }

    public void setMapData(byte[] mapData) {
        this.mapData = mapData;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }
}
