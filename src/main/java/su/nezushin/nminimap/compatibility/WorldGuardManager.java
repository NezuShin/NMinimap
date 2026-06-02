package su.nezushin.nminimap.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import su.nezushin.nminimap.NMinimap;
import su.nezushin.nminimap.util.config.Config;
import su.nezushin.nminimap.util.config.UndergroundLayer;

import java.util.logging.Level;

public class WorldGuardManager {

    private final boolean enabled;

    public WorldGuardManager() {
        this.enabled = Bukkit.getPluginManager().isPluginEnabled("WorldGuard");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public UndergroundLayer getActiveLayer(Location location) {
        if (!enabled || Config.undergroundLayers.isEmpty()) {
            return null;
        }
        try {
            var rm = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer().get(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(location.getWorld()));
            if (rm == null) return null;

            var set = rm.getApplicableRegions(com.sk89q.worldedit.math.BlockVector3.at(location.getX(), location.getY(), location.getZ()));

            UndergroundLayer bestLayer = null;

            for (com.sk89q.worldguard.protection.regions.ProtectedRegion region : set) {
                for (UndergroundLayer layer : Config.undergroundLayers) {
                    for (String regionId : layer.wgRegions()) {
                        if (regionId.equalsIgnoreCase(region.getId())) {
                            if (bestLayer == null || layer.priority() > bestLayer.priority() ||
                                    (layer.priority() == bestLayer.priority() && layer.renderFromY() < bestLayer.renderFromY())) {
                                bestLayer = layer;
                            }
                        }
                    }
                }
            }

            return bestLayer;
        } catch (Exception e) {
            NMinimap.getInstance().getLogger().log(Level.SEVERE, "WorldGuard region fetch failed", e);
            return null;
        }
    }

    public boolean isInsideLayer(Location location, UndergroundLayer layer) {
        if (!enabled || layer == null) return false;
        try {
            var rm = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer().get(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(location.getWorld()));
            if (rm == null) return false;

            for (String regionId : layer.wgRegions()) {
                com.sk89q.worldguard.protection.regions.ProtectedRegion region = rm.getRegion(regionId);
                if (region != null) {
                    int mockY = region.getMinimumPoint().getBlockY();
                    if (region.contains(location.getBlockX(), mockY, location.getBlockZ())) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            NMinimap.getInstance().getLogger().log(Level.SEVERE, "WorldGuard region fetch failed", e);
            return false;
        }
    }


}