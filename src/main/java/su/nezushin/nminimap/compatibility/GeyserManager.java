package su.nezushin.nminimap.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.geyser.api.GeyserApi;

public class GeyserManager {

    private final boolean enabled;

    public GeyserManager() {
        this.enabled = Bukkit.getPluginManager().isPluginEnabled("Geyser-Spigot");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isBedrockPlayer(Player player) {
        if (!enabled) {
            return false;
        }
        GeyserApi api = GeyserApi.api();
        return api != null && api.isBedrockPlayer(player.getUniqueId());
    }
}
