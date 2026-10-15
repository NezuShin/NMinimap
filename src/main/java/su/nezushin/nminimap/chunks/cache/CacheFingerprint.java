package su.nezushin.nminimap.chunks.cache;

import org.bukkit.Material;
import su.nezushin.nminimap.chunks.ChunkEntry;
import su.nezushin.nminimap.util.PerWorldSettingsUtil;
import su.nezushin.nminimap.util.config.UndergroundLayer;
import su.nezushin.nminimap.util.config.WaterRenderingSettings;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Stable disk identity; Java object hash codes are used only for the in-memory lookup. */
public final class CacheFingerprint {
    private record Profile(String world, UndergroundLayer layer, boolean surface) {}
    private static final Map<Profile, String> namespaces = new ConcurrentHashMap<>();

    private CacheFingerprint() {}

    public static void clear() {
        namespaces.clear();
    }

    public static String namespace(ChunkEntry entry) {
        return namespaces.computeIfAbsent(new Profile(entry.world(), entry.layer(), entry.surface()), CacheFingerprint::calculate);
    }

    private static String calculate(Profile profile) {
        try {
            var bytes = new ByteArrayOutputStream();
            try (var out = new DataOutputStream(bytes)) {
                var world = PerWorldSettingsUtil.find(profile.world());
                writeNullableInt(out, world == null ? null : world.minY());
                writeNullableInt(out, world == null ? null : world.maxY());
                out.writeBoolean(world != null && world.skipCeiling() != null ? world.skipCeiling()
                        : su.nezushin.nminimap.util.config.Config.skipCeiling);
                writeMaterials(out, world != null && world.ceilingBlocks() != null
                        ? world.ceilingBlocks() : Set.of(Material.BEDROCK));
                var layer = profile.layer();
                writeWater(out, layer == null ? PerWorldSettingsUtil.getWaterRendering(profile.world()) : layer.waterRendering());
                boolean underground = layer != null && !profile.surface();
                out.writeBoolean(underground);
                if (underground) {
                    out.writeInt(layer.renderFromY());
                    out.writeFloat(layer.darken());
                    var regions = layer.wgRegions().stream().sorted().toList();
                    out.writeInt(regions.size());
                    for (var region : regions) out.writeUTF(region);
                    var smart = layer.smartDescend();
                    out.writeBoolean(smart.useRegionFloor());
                    out.writeInt(smart.minY());
                    out.writeUTF(smart.noOpeningMode().name());
                    out.writeInt(smart.minOpenHeight());
                    out.writeInt(smart.minConnectedColumns());
                    writeMaterials(out, smart.transparentBlocks());
                }
            }
            return "render-v9-" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
        } catch (IOException | NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Cannot build terrain cache fingerprint", ex);
        }
    }

    private static void writeNullableInt(DataOutputStream out, Integer value) throws IOException {
        out.writeBoolean(value != null);
        if (value != null) out.writeInt(value);
    }

    private static void writeMaterials(DataOutputStream out, Set<Material> materials) throws IOException {
        var names = materials.stream().map(Enum::name).sorted().toList();
        out.writeInt(names.size());
        for (var name : names) out.writeUTF(name);
    }

    private static void writeWater(DataOutputStream out, WaterRenderingSettings water) throws IOException {
        out.writeUTF(water.mode().name());
        out.writeFloat(water.opacity());
        out.writeFloat(water.minOpacity());
        out.writeFloat(water.maxOpacity());
        out.writeInt(water.depthForMaxOpacity());
        out.writeInt(water.tint().asRGB());
        out.writeUTF(water.colorSource().name());
        out.writeInt(water.maxSampledDepth());
        out.writeFloat(water.underwaterDarken());
    }
}
