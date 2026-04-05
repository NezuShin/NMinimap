package su.nezushin.nminimap.util.config;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public enum Permission {

    admin;


    public boolean has(CommandSender p) {
        return p.hasPermission("nminimap." + this.name());
    }
}
