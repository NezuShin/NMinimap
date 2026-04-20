package su.nezushin.nminimap.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

public class SpigotEntityIdUtil {

    public static int nextEntityId() {
        try {
            return Bukkit.getUnsafe().nextEntityId();
        } catch (NoSuchMethodError e) {
            var world = Bukkit.getWorlds().get(0);
            var entity = world.spawnEntity(new Location(world, 0, 0, 0), EntityType.SHEEP);

            entity.remove();

            return entity.getEntityId();
        }
    }

}
