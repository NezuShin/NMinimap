package su.nezushin.nminimap.util.config;

import org.bukkit.command.CommandSender;

public enum Permission {

    admin("admin"),
    hide_on_map("hide-on-map");

    private final String node;

    Permission(String node) {
        this.node = node;
    }

    public boolean has(CommandSender p) {
        return p.hasPermission("nminimap." + node);
    }
}
