package su.nezushin.nminimap.api.events;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class AsyncEntityIconSelectEvent extends Event implements Cancellable {
    private static final HandlerList handlerList = new HandlerList();

    private String selectedIcon;
    private boolean allowRotation;

    private final Entity entity;
    private final Player player;
    private boolean cancelled = false;

    public AsyncEntityIconSelectEvent(Player player, Entity entity, String selectedIcon, boolean allowRotation) {
        super(true);
        this.player = player;
        this.entity = entity;
        this.selectedIcon = selectedIcon;
        this.allowRotation = allowRotation;
    }

    public String getSelectedIcon() {
        return selectedIcon;
    }

    public void setSelectedIcon(String selectedIcon) {
        this.selectedIcon = selectedIcon;
    }

    public boolean isAllowRotation() {
        return allowRotation;
    }

    public void setAllowRotation(boolean allowRotation) {
        this.allowRotation = allowRotation;
    }

    public Entity getEntity() {
        return entity;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        cancelled = cancel;
    }
}
