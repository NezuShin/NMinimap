package su.nezushin.nminimap.compatibility.providers;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import su.nezushin.nminimap.util.ChunkLoadingUtil;
import su.nezushin.nminimap.util.config.Config;
import su.nezushin.nminimap.util.config.Message;

public class XaerosMinimapProvider implements ModInterfaceProvider {

    @Override
    public void disableMap(Player p) {
        sendMessage(p,"§n§o§m§i§n§i§m§a§p");
    }

    @Override
    public void resetMap(Player p) {
        sendMessage(p,"§r§e§s§e§t§x§a§e§r§o");

        if (!Config.allowModRadar) {
            sendMessage(p, "§f§a§i§r§x§a§e§r§o");
            if (Config.skipCeiling)
                sendMessage(p,"§x§a§e§r§o§m§m§n§e§t§h§e§r§i§s§f§a§i§r");
        }
    }

    public void sendMessage(Player p, String msg) {
        if(!p.isOnline())
            return;
        if (ChunkLoadingUtil.isPaper())
            Message.getAdventure().sender(p).sendMessage(Component.text(msg));
        else
            p.sendMessage(msg);

    }
}
