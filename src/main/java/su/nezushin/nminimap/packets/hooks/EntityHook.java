package su.nezushin.nminimap.packets.hooks;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;


public interface EntityHook {

    public void spawnMarker(Player p, int id);


    /**
     *
     * @param p
     * @param id
     * @param item
     * @param direction true - down, false - up
     */
    public void spawnItemFrame(Player p, int id, ItemStack item, boolean direction);

    public void teleportItemFrame(Player p, int id);

    public void removeEntities(Player p, int... ids);

    public void sendMapData(Player p, int mapId, int scale, byte[] mapData);

    public void sendMarkerData(Player p, int id, Component markerData);

}
