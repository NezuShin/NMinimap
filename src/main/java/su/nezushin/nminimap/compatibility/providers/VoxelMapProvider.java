package su.nezushin.nminimap.compatibility.providers;

import com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import su.nezushin.nminimap.NMinimap;
import su.nezushin.nminimap.util.SchedulerUtil;
import su.nezushin.nminimap.util.config.Config;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


/**
 * Taken from <a href="https://github.com/Updated-VoxelMap/VoxelMapPlugin">https://github.com/Updated-VoxelMap/VoxelMapPlugin</a>
 */
public class VoxelMapProvider implements ModInterfaceProvider {

    private static final String VOXELMAP_SETTINGS_CHANNEL = "voxelmap:settings";

    public VoxelMapProvider() {
        if (!Bukkit.getMessenger().isOutgoingChannelRegistered(NMinimap.getInstance(), VOXELMAP_SETTINGS_CHANNEL))
            Bukkit.getMessenger().registerOutgoingPluginChannel(NMinimap.getInstance(), VOXELMAP_SETTINGS_CHANNEL);
    }

    @Override
    public void disableMap(Player p) {
        Map<String, Object> settings = new HashMap<>();
        settings.put("minimapAllowed", false);
        putRadarData(settings);

        sendData(p, settings);
    }

    @Override
    public void resetMap(Player p) {
        Map<String, Object> settings = new HashMap<>();
        settings.put("minimapAllowed", true);
        putRadarData(settings);

        sendData(p, settings);
    }

    private void putRadarData(Map<String, Object> settings) {
        settings.put("radarAllowed", Config.allowModRadar);
        settings.put("radarMobsAllowed", Config.allowModRadar);
        settings.put("radarPlayersAllowed", Config.allowModRadar);
    }

    private void sendData(Player p, Map<String, Object> settings) {
        SchedulerUtil.getScheduler().async(() -> {//We need this delay because mod does not receive messages immediately after player join
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MinecraftDataOutputStream dataOut = new MinecraftDataOutputStream(out);
            try {
                dataOut.write(0); // channelid, always 0
                dataOut.writeString(new Gson().toJson(settings));
                dataOut.flush();
                p.sendPluginMessage(NMinimap.getInstance(), VOXELMAP_SETTINGS_CHANNEL, out.toByteArray());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, 10L);

    }

    private class MinecraftDataOutputStream extends DataOutputStream {
        public MinecraftDataOutputStream(OutputStream out) {
            super(out);
        }

        public void writeString(String string) throws IOException {
            byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
            writeVarInt(bytes.length);
            out.write(bytes);
        }

        public void writeVarInt(int value) throws IOException {
            while ((value & 0xFFFFFF80) != 0) {
                writeByte(value & 0x7F | 0x80);
                value >>>= 7;
            }
            writeByte(value);
        }

        public void writeVarLong(long value) throws IOException {
            while ((value & 0xFFFFFFFFFFFFFF80L) != 0) {
                writeByte((int) (value & 0x7F | 0x80));
                value >>>= 7;
            }
            writeByte((int) value);
        }

        public void writeUuid(UUID uuid) throws IOException {
            writeLong(uuid.getMostSignificantBits());
            writeLong(uuid.getLeastSignificantBits());
        }
    }
}
