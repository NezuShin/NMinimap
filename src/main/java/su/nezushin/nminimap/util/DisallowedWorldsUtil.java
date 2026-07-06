package su.nezushin.nminimap.util;

import org.bukkit.World;
import su.nezushin.nminimap.util.config.Config;

public class DisallowedWorldsUtil {

    public static boolean isAllowed(World world) {
        var name = world.getName();
        if (Config.disallowedWorlds.contains(name))
            return false;

        if (!Config.useDisallowedWorldsRegex)
            return true;

        return !Config.disallowedWorldsRegex.matcher(name).matches();
    }

}
