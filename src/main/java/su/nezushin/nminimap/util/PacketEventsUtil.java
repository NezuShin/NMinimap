package su.nezushin.nminimap.util;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import org.bukkit.entity.Player;

public class PacketEventsUtil {

    public static void sendPackets(Player p, PacketWrapper<?>... wrappers) {
        for (var i : wrappers)
            PacketEvents.getAPI().getPlayerManager().sendPacket(p, i);
    }

    public static int getBlockFace(org.bukkit.block.BlockFace face) {
        switch (face) {
            case DOWN:
                return 0;
            case UP:
                return 1;
            case NORTH:
                return 2;
            case SOUTH:
                return 3;
            case WEST:
                return 4;
            case EAST:
                return 5;
        }
        return 0;
    }
}
