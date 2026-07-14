package su.nezushin.nminimap.listeners;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffectType;
import su.nezushin.nminimap.NMinimap;
import su.nezushin.nminimap.api.events.AsyncMarkerRenderEvent;
import su.nezushin.nminimap.api.events.AsyncEntityIconSelectEvent;
import su.nezushin.nminimap.markers.impl.LocationMarker;
import su.nezushin.nminimap.markers.impl.PositionMarker;
import su.nezushin.nminimap.util.config.Config;
import su.nezushin.nminimap.util.config.Permission;
import su.nezushin.nminimap.util.config.StaticMarker;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MarkerListener implements Listener {

    @EventHandler
    public void mapOwner(AsyncMarkerRenderEvent e) {
        var player = e.getPlayer();
        if (Config.playerMarker.isEmpty())
            return;

        e.getMarkers().add(new PositionMarker(Config.playerMarker, 0, 0, (int) (((Math.floorMod((int) player.getPlayer().getLocation().getYaw() - 2, 360) / 360.0f) * 256.0) - 127)));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void anotherPlayers(AsyncMarkerRenderEvent e) {
        var player = e.getPlayer();
        var p = player.getPlayer();
        if (Config.anotherPlayerMarker.isEmpty())
            return;

        p.getWorld().getPlayers().stream().filter(i ->
                        !i.equals(p)
                                && !Config.anotherPlayerMarkerHideGameModes.contains(i.getGameMode())
                                && (!Config.anotherPlayerMarkerHideInvisibilityPotionEffect || !i.hasPotionEffect(PotionEffectType.INVISIBILITY))
                                && (!Config.anotherPlayerMarkerHidePermission || !Permission.hide_on_map.has(i))
                                && Config.isInRadius(i.getLocation(), p.getLocation(),
                                Config.anotherPlayerMarkerHideRadiusXZ, Config.anotherPlayerMarkerHideRadiusY))
                .forEach(i -> {
                    e.getMarkers().add(new LocationMarker(Config.anotherPlayerMarker, i.getLocation()));
                });
    }

    @EventHandler(priority = EventPriority.HIGH)//to insure player's marker always higher than static markers
    public void staticMarkers(AsyncMarkerRenderEvent e) {
        var player = e.getPlayer();
        var p = player.getPlayer();

        e.getMarkers().addAll(Config.staticMarkers.stream().map(StaticMarker::marker).filter(i -> i.getLocation().getWorld().equals(p.getWorld())).toList());
    }

    Map<World, Collection<Entity>> entityList = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.HIGH)
    public void entityRadar(AsyncMarkerRenderEvent e) {
        if (!Config.allowMobRadar)
            return;
        var player = e.getPlayer();
        var p = player.getPlayer();

        NMinimap.getInstance().getMobRadarManager().getEntities(p.getWorld()).stream().filter(i ->
                        i.getType() != EntityType.PLAYER
                                && !Config.mobRadarDisallowedEntities.contains(i.getType())
                                && (Config.mobRadarAllowedEntities.isEmpty() || Config.mobRadarAllowedEntities.contains(i.getType()))
                                && Config.isInRadius(i.getLocation(), p.getLocation(),
                                Config.mobRadarHideRadiusXZ, Config.mobRadarHideRadiusY))
                .forEach(i -> {
                    var settings = Config.getMobRadarMarker(i.getType());

                    var event = new AsyncEntityIconSelectEvent(p, i, settings.icon(), settings.allowRotation());

                    Bukkit.getPluginManager().callEvent(event);

                    if (event.isCancelled())
                        return;

                    var loc = i.getLocation().clone();
                    if (!event.isAllowRotation()) {
                        loc.setYaw(180);
                    }

                    e.getMarkers().add(new LocationMarker(event.getSelectedIcon(), loc));
                });

    }

}
