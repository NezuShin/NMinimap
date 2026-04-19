package su.nezushin.nminimap.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import su.nezushin.nminimap.NMinimap;
import su.nezushin.nminimap.util.SchedulerUtil;

public class PlayerListener implements Listener {


    @EventHandler
    public void join(PlayerJoinEvent e) {
        NMinimap.getInstance().loadPlayer(e.getPlayer());
    }

    @EventHandler
    public void quit(PlayerQuitEvent e) {
        var player = e.getPlayer();

        NMinimap.async(() -> {
            NMinimap.getInstance().getPlayersWithMap().removeIf(i -> {
                if (i.getPlayer().equals(player)) {
                    i.onQuit();
                    return true;
                }
                return false;
            });
        });
    }

    @EventHandler
    public void teleport(PlayerTeleportEvent e) {
        var p = e.getPlayer();

        SchedulerUtil.getScheduler().async(() -> {
            var player = NMinimap.getInstance().getPlayersWithMap().stream().filter(i -> i.getPlayer().equals(p)).findFirst().orElse(null);

            if (player != null && player.isEnabled()) {
                NMinimap.getInstance().getPacketManager().spawnEntities(p);
            }
        }, 4);
    }

}
