package su.nezushin.nminimap.compatibility.providers;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import su.nezushin.nminimap.util.config.Config;
import su.nezushin.nminimap.util.config.Message;

public class XaerosMinimapProvider implements ModInterfaceProvider {

    @Override
    public void disableMap(Player p) {
        Message.getAdventure().sender(p).sendMessage(Component.text("§n§o§m§i§n§i§m§a§p"));
    }

    @Override
    public void resetMap(Player p) {
        Message.getAdventure().sender(p).sendMessage(Component.text("§r§e§s§e§t§x§a§e§r§o"));

        if (!Config.allowModRadar) {
            Message.getAdventure().sender(p).sendMessage(Component.text("§f§a§i§r§x§a§e§r§o"));
            if (Config.skipCeiling)
                Message.getAdventure().sender(p).sendMessage(Component.text("§x§a§e§r§o§m§m§n§e§t§h§e§r§i§s§f§a§i§r"));
        }

    }
}
