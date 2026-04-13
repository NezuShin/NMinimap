package su.nezushin.nminimap.compatibility.providers;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class XaerosMinimapProvider implements ModInterfaceProvider {

    @Override
    public void disableMap(Player p) {
        p.sendMessage(Component.text("§n§o§m§i§n§i§m§a§p"));
    }

    @Override
    public void resetMap(Player p) {
        p.sendMessage(Component.text("§r§e§s§e§t§x§a§e§r§o"));
    }
}
