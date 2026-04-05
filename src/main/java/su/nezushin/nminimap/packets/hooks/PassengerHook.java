package su.nezushin.nminimap.packets.hooks;

import org.bukkit.entity.Player;

public interface PassengerHook {

    public void updatePassengers(Player p, int... passengers);
}
