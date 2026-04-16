package su.nezushin.nminimap.compatibility.providers;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import su.nezushin.nminimap.NMinimap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class JourneyMapProvider implements ModInterfaceProvider {

    private static final String JOURNEY_MAP_SETTINGS_CHANNEL = "journeymap:perm_req";

    public JourneyMapProvider() {
        if (!Bukkit.getMessenger().isOutgoingChannelRegistered(NMinimap.getInstance(), JOURNEY_MAP_SETTINGS_CHANNEL))
            Bukkit.getMessenger().registerOutgoingPluginChannel(NMinimap.getInstance(), JOURNEY_MAP_SETTINGS_CHANNEL);
    }

    @Override
    public void disableMap(Player p) {
        Map<String, Object> settings = new HashMap<>();
        settings.put("journeymapEnabled", false);

        sendData(p, settings);
    }

    @Override
    public void resetMap(Player p) {
        Map<String, Object> settings = new HashMap<>();
        settings.put("journeymapEnabled", true);

        sendData(p, settings);
    }

    private void sendData(Player p, Map<String, Object> settings) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(NMinimap.getInstance(), () -> {//We need this delay because mod does not receive messages immediately after player join

            Gson gson = new Gson();
            String payload = gson.toJson(settings);
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeByte(42);
            //out.writeBoolean(player.hasPermission("minimap.jm.admin"));
            out.writeBoolean(false);
            writeUtf(payload, out);
            out.writeBoolean(true);
            p.sendPluginMessage(NMinimap.getInstance(), "journeymap:perm_req", out.toByteArray());
        }, 2L);

    }

    private static void writeUtf(String value, ByteArrayDataOutput out) {
        byte[] src = value.getBytes(StandardCharsets.UTF_8);

        if (src.length > 32767) {
            throw new RuntimeException("String too big (was " + src.length + " bytes encoded, max " + 32767 + ")");
        } else {
            writeVarInt(src.length, out);
            out.write(src);
        }
    }

    private static void writeVarInt(int value, ByteArrayDataOutput out) {
        while ((value & -128) != 0) {
            out.writeByte(value & 127 | 128);
            value >>>= 7;
        }

        out.writeByte(value);
    }

}
