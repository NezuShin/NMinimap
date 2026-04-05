package su.nezushin.nminimap.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;

public class VanillaMapUtil {


    /**
     * Create map ItemStack and remove vanilla renderer
     *
     * @param mapId
     * @return
     */
    public static ItemStack createItem(int mapId) {
        var map = Bukkit.getMap(mapId);

        if (map != null)
            for (var renderer : map.getRenderers())
                map.removeRenderer(renderer);

        var item = new ItemStack(Material.FILLED_MAP);

        if (item.getItemMeta() instanceof MapMeta meta) {

            meta.setMapId(mapId);
            item.setItemMeta(meta);
        }
        return item;
    }
}
