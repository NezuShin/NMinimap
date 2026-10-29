package su.nezushin.nminimap.listeners;

import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import su.nezushin.nminimap.NMinimap;
import su.nezushin.nminimap.util.SchedulerUtil;

import java.util.concurrent.atomic.AtomicInteger;

/*
 The client simulates arrows too. When a burning arrow hits one of our fake item frames on the client,
 the client sets that frame on fire, and item frames never put the fire out on the client.
 No packet can clear it, so we watch burning arrows and re-spawn item frames of players they fly near.
 */
public class ProjectileListener implements Listener {

    // Frames are up to ~6 blocks from the player and arrows fly ~3 blocks per tick
    private static final double CHECK_RADIUS = 10;

    // Stop watching arrows that fly longer than this
    private static final int MAX_WATCH_TICKS = 200;

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void launch(ProjectileLaunchEvent e) {
        if (!(e.getEntity() instanceof AbstractArrow arrow))
            return;

        var watchedTicks = new AtomicInteger();

        // Flame sets the arrow on fire after this event, so start on the next tick
        SchedulerUtil.getScheduler().entityTimer(arrow, () -> {
            if (!arrow.isValid() || arrow.isInBlock() || watchedTicks.incrementAndGet() > MAX_WATCH_TICKS)
                return false;

            if (isBurning(arrow))
                for (var entity : arrow.getNearbyEntities(CHECK_RADIUS, CHECK_RADIUS, CHECK_RADIUS))
                    if (entity instanceof Player p)
                        NMinimap.getInstance().getPacketManager().requestItemFrameRefresh(p);

            return true;
        }, 1, 1);
    }

    @SuppressWarnings("deprecation") // getVisualFire() is Paper-only
    private boolean isBurning(AbstractArrow arrow) {
        return arrow.getFireTicks() > 0 || arrow.isVisualFire();
    }
}
