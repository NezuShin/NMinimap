package su.nezushin.nminimap.util;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import su.nezushin.nminimap.NMinimap;

import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;


//Folia compatibility
public class SchedulerUtil {

    private static Scheduler scheduler;

    static {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            scheduler = new FoliaScheduler();
        } catch (ClassNotFoundException e) {
            scheduler = new SpigotScheduler();
        }
    }

    public static Scheduler getScheduler() {
        return scheduler;
    }

    public static interface RunningTask {

        public void cancel();
    }

    public static interface Scheduler {

        public void cancelAllTasks();

        public RunningTask async(Runnable run, long delay, long period);

        public void async(Runnable run, long delay);

        public void sync(Runnable run);

        /**
         * Repeat task on the thread that owns the entity, until task returns false or the entity is removed
         */
        public void entityTimer(Entity entity, BooleanSupplier task, long delay, long period);

        public boolean isFolia();
    }

    private static class SpigotScheduler implements Scheduler {


        @Override
        public void cancelAllTasks() {
            Bukkit.getScheduler().cancelTasks(NMinimap.getInstance());
        }

        @Override
        public RunningTask async(Runnable run, long delay, long period) {
            var task = Bukkit.getScheduler().runTaskTimerAsynchronously(NMinimap.getInstance(), run, delay, period);
            return task::cancel;
        }

        @Override
        public void async(Runnable run, long delay) {
            Bukkit.getScheduler().runTaskLaterAsynchronously(NMinimap.getInstance(), run, delay);

        }

        @Override
        public void sync(Runnable run) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(NMinimap.getInstance(), run);
        }

        @Override
        public void entityTimer(Entity entity, BooleanSupplier task, long delay, long period) {
            Bukkit.getScheduler().runTaskTimer(NMinimap.getInstance(), t -> {
                if (!entity.isValid() || !task.getAsBoolean())
                    t.cancel();
            }, delay, period);
        }

        @Override
        public boolean isFolia() {
            return false;
        }
    }

    private static class FoliaScheduler implements Scheduler {

        @Override
        public void cancelAllTasks() {
            Bukkit.getAsyncScheduler().cancelTasks(NMinimap.getInstance());
            Bukkit.getGlobalRegionScheduler().cancelTasks(NMinimap.getInstance());
        }

        @Override
        public RunningTask async(Runnable run, long delay, long period) {
            var task = Bukkit.getAsyncScheduler().runAtFixedRate(NMinimap.getInstance(), (ScheduledTask scheduledTask) -> {
                        run.run();
                    },
                    delay * 50L, period * 50L, TimeUnit.MILLISECONDS
            );
            return task::cancel;
        }

        @Override
        public void async(Runnable run, long delay) {
            Bukkit.getAsyncScheduler().runDelayed(NMinimap.getInstance(), (ScheduledTask scheduledTask) -> {
                        run.run();
                    },
                    delay * 50L, TimeUnit.MILLISECONDS
            );
        }

        @Override
        public void sync(Runnable run) {
            Bukkit.getGlobalRegionScheduler().execute(NMinimap.getInstance(), run);
        }

        @Override
        public void entityTimer(Entity entity, BooleanSupplier task, long delay, long period) {
            // Entity scheduler stops by itself when the entity is removed
            entity.getScheduler().runAtFixedRate(NMinimap.getInstance(), t -> {
                if (!task.getAsBoolean())
                    t.cancel();
            }, null, delay, period);
        }

        @Override
        public boolean isFolia() {
            return true;
        }
    }
}
