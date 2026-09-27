package su.nezushin.nminimap.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import su.nezushin.nminimap.frames.FrameLayer;
import su.nezushin.nminimap.player.NMapPlayer;

import java.util.List;

public class AsyncFrameRenderEvent extends Event {
    private static final HandlerList handlerList = new HandlerList();

    private final NMapPlayer player;
    private final List<FrameLayer> layers;

    public AsyncFrameRenderEvent(NMapPlayer player, List<FrameLayer> layers) {
        super(true);
        this.player = player;
        this.layers = layers;
    }

    public NMapPlayer getPlayer() {
        return player;
    }

    public List<FrameLayer> getLayers() {
        return layers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }
}
