package su.nezushin.nminimap.packets.hooks.impl;

import com.maximde.passengerapi.PassengerAPI;
import com.maximde.passengerapi.PassengerActions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import su.nezushin.nminimap.NMinimap;
import su.nezushin.nminimap.packets.hooks.PassengerHook;

public class PassengerAPIPassengerHook implements PassengerHook {
    PassengerActions passengerActions;

    public PassengerAPIPassengerHook() {
        passengerActions = PassengerAPI.getAPI(NMinimap.getInstance());
    }

    @Override
    public void updatePassengers(Player p, int[] passengers) {
        passengerActions.addPassengers(!Bukkit.isPrimaryThread(), p.getEntityId(), passengers);
    }
}
