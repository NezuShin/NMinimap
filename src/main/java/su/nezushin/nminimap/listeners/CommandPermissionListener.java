package su.nezushin.nminimap.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import su.nezushin.nminimap.api.events.AsyncMapRenderEvent;
import su.nezushin.nminimap.util.config.Permission;

public class CommandPermissionListener implements Listener {

    @EventHandler
    public void onMapRender(AsyncMapRenderEvent event) {
        var player = event.getPlayer();
        if (!Permission.command_minimap.has(player.getPlayer())) {
            player.setEnabled(false);
        }
    }
}
