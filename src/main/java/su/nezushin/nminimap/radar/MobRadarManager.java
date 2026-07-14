package su.nezushin.nminimap.radar;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import su.nezushin.nminimap.NMinimap;
import su.nezushin.nminimap.util.SchedulerUtil;
import su.nezushin.nminimap.util.config.Config;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MobRadarManager {

    Map<World, Collection<Entity>> entities = new ConcurrentHashMap<>();

    public MobRadarManager() {
        if (Config.allowMobRadar)
            SchedulerUtil.getScheduler().async(this::fetchAllMobs, 1, 1);
    }

    private void fetchAllMobs() {
        if (SchedulerUtil.getScheduler().isFolia()) {
            fetchAllMobsFolia();
            return;
        }

        SchedulerUtil.getScheduler().sync(() -> {
            for (World world : Bukkit.getWorlds()) {
                entities.put(world, new HashSet<>(world.getEntities()));
            }
        });
    }

    private void fetchAllMobsFolia() {
        Bukkit.getGlobalRegionScheduler().execute(NMinimap.getInstance(), () -> {
            for (World world : Bukkit.getWorlds()) {
                Chunk[] chunks = world.getLoadedChunks();
                Set<Entity> collected = ConcurrentHashMap.newKeySet();

                if (chunks.length == 0) {
                    entities.put(world, collected);
                    continue;
                }

                AtomicInteger pending = new AtomicInteger(chunks.length);

                for (Chunk chunk : chunks) {
                    var loc = chunk.getBlock(8, 64, 8).getLocation();
                    Bukkit.getRegionScheduler().execute(NMinimap.getInstance(), loc, () -> {
                        collected.addAll(Arrays.asList(chunk.getEntities()));
                        if (pending.decrementAndGet() == 0) {
                            entities.put(world, collected);
                        }
                    });
                }
            }
        });
    }

    public Collection<Entity> getEntities(World w) {
        return entities.getOrDefault(w, new HashSet<>());
    }

}
