package su.nezushin.nminimap.compatibility;

import org.bukkit.entity.Player;
import su.nezushin.nminimap.compatibility.providers.*;
import su.nezushin.nminimap.util.config.Config;

import java.util.ArrayList;
import java.util.List;

public class ModCompatibilityManager {
    private final List<ModInterfaceProvider> providers = new ArrayList<>();

    public ModCompatibilityManager() {
        if (Config.enableModVoxelMap)
            providers.add(new VoxelMapProvider());
        if (Config.enableModXaerosMap)
            providers.add(new XaerosMinimapProvider());
        if (Config.enableModJourneyMap)
            providers.add(new JourneyMapProvider());

    }

    public void disableModMinimap(Player p) {
        for (var i : providers)
            i.disableMap(p);
    }

    public void resetModMinimap(Player p) {
        for (var i : providers)
            i.resetMap(p);
    }

}
