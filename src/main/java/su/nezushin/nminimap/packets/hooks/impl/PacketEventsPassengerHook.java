package su.nezushin.nminimap.packets.hooks.impl;

import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers;
import org.bukkit.entity.Player;
import su.nezushin.nminimap.packets.hooks.PassengerHook;
import su.nezushin.nminimap.util.PacketEventsUtil;

public class PacketEventsPassengerHook implements PassengerHook {
    @Override
    public void updatePassengers(Player p, int[] passengers) {
        PacketEventsUtil.sendPackets(p, new WrapperPlayServerSetPassengers(p.getEntityId(), passengers));
    }
}
