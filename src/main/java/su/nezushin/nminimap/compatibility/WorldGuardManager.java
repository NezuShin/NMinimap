package su.nezushin.nminimap.compatibility;

import org.bukkit.Location;
import su.nezushin.nminimap.util.config.Config;
import su.nezushin.nminimap.util.config.UndergroundLayer;

public class WorldGuardManager {

    private static boolean enabled = false;

    static {
        try {
            Class.forName("com.sk89q.worldguard.WorldGuard");
            enabled = true;
        } catch (ClassNotFoundException e) {
            enabled = false;
        }
    }

    public static UndergroundLayer getActiveLayer(Location location) {
        if (!enabled || Config.undergroundLayers.isEmpty()) {
            return null;
        }
        return WGImpl.getActiveLayer(location);
    }
    
    public static boolean isInsideLayer(Location location, UndergroundLayer layer) {
        if (!enabled || layer == null) return false;
        return WGImpl.isInsideLayer(location, layer);
    }

    private static class WGImpl {
        static UndergroundLayer getActiveLayer(Location location) {
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
                return null;
            }
        }

        static boolean isInsideLayer(Location location, UndergroundLayer targetLayer) {
            try {
                var rm = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer().get(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(location.getWorld()));
                if (rm == null) return false;
                
                for (String regionId : targetLayer.wgRegions()) {
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
                return false;
            }
        }
    }
}
