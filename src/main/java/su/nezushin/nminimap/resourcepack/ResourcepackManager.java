package su.nezushin.nminimap.resourcepack;

import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.gson.GsonBuilder;
import net.kyori.adventure.key.Key;
import su.nezushin.nminimap.NMinimap;
import su.nezushin.nminimap.frames.FrameManager;
import su.nezushin.nminimap.markers.MarkerManager;
import su.nezushin.nminimap.resourcepack.cache.FontImageIdCache;
import su.nezushin.nminimap.resourcepack.packmcmeta.PackMcMeta;
import su.nezushin.nminimap.util.config.Config;
import su.nezushin.nminimap.util.ZipUtil;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Builds the resourcepack: copies defaults, writes shaders and pack.mcmeta, runs the marker and
 * frame packing passes and ships the result to the configured destinations.
 * <p>
 * Owns the font character cache, both passes write into the same font.
 */
public class ResourcepackManager {

    public static final Key FONT = Key.key("nminimap:default");

    private final MarkerManager markerManager = new MarkerManager();
    private final FrameManager frameManager = new FrameManager();

    public ResourcepackManager() {
        build();
    }

    public MarkerManager getMarkerManager() {
        return markerManager;
    }

    public FrameManager getFrameManager() {
        return frameManager;
    }

    public void build() {
        try {
            var cache = FontImageIdCache.load();

            var resourcepackDir = new File(NMinimap.getInstance().getDataFolder(), "resourcepack");
            var namespaceDir = new File(resourcepackDir, "assets/nminimap/");
            var texturesDir = new File(namespaceDir, "textures/font/");
            var fontsDir = new File(namespaceDir, "font");

            var markersDir = new File(NMinimap.getInstance().getDataFolder(), "markers");
            var framesDir = new File(NMinimap.getInstance().getDataFolder(), "frames");

            texturesDir.mkdirs();
            fontsDir.mkdirs();
            markersDir.mkdirs();
            framesDir.mkdirs();

            copyDefaultImages(markersDir, framesDir);
            if (Config.resourcepackCopyShaders)
                writeShaders(resourcepackDir, namespaceDir);
            if (Config.packMcMetaChangeEnabled)
                writePackMcMeta(resourcepackDir);

            var context = new PackContext(texturesDir, cache, NMinimap.getInstance().getLogger());
            markerManager.bake(context, markersDir);
            frameManager.bake(context, framesDir, Config.frames);

            cache.build(fontsDir);
            cache.save();

            for (var i : Config.getResourcepackCopyDestinationFiles()) {
                ZipUtil.deleteDirectory(i);
                ZipUtil.copyDirectory(resourcepackDir, i);
            }
            for (var i : Config.getResourcepackZipDestinationFiles())
                ZipUtil.pack(resourcepackDir, i);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void copyDefaultImages(File markersDir, File framesDir) {
        if (Config.resourcepackCopyMarkers)
            for (var i : new String[]{"player", "player_small", "white_banner", "red_marker", "skeleton", "spider", "zombie"})
                Config.copyDefaults("defaults/markers/" + i + ".png", new File(markersDir, i + ".png"), false);

        if (Config.resourcepackCopyFrames)
            for (var i : new String[]{"default", "inventory", "inventory_with_title"})
                for (var j : new String[]{"square", "round"}) {
                    var filename = i + "_" + j + ".png";
                    Config.copyDefaults("defaults/frames/" + filename, new File(framesDir, filename), false);
                }
    }

    private void writeShaders(File resourcepackDir, File namespaceDir) throws Exception {
        var nminimapShadersDir = new File(namespaceDir, "shaders");

        if (Config.packEnable1_21_11) {
            Config.copyDefaults("defaults/shaders/core/v1_21_11/rendertype_text.fsh", new File(resourcepackDir, "nminimap_1_21_11/assets/minecraft/shaders/core/rendertype_text.fsh"), true);
            Config.copyDefaults("defaults/shaders/core/v1_21_11/rendertype_text.vsh", new File(resourcepackDir, "nminimap_1_21_11/assets/minecraft/shaders/core/rendertype_text.vsh"), true);
        }

        if (Config.packEnable26_1) {
            Config.copyDefaults("defaults/shaders/core/v26_1/rendertype_text.fsh", new File(resourcepackDir, "nminimap_26_1/assets/minecraft/shaders/core/rendertype_text.fsh"), true);
            Config.copyDefaults("defaults/shaders/core/v26_1/rendertype_text.vsh", new File(resourcepackDir, "nminimap_26_1/assets/minecraft/shaders/core/rendertype_text.vsh"), true);
        }

        if (Config.packEnable26_2) {
            Config.copyDefaults("defaults/shaders/core/v26_2/text.fsh", new File(resourcepackDir, "nminimap_26_2/assets/minecraft/shaders/core/text.fsh"), true);
            Config.copyDefaults("defaults/shaders/core/v26_2/text.vsh", new File(resourcepackDir, "nminimap_26_2/assets/minecraft/shaders/core/text.vsh"), true);
        }

        //Config.copyDefaults("defaults/shaders/include/config.glsl", new File(nminimapShadersDir, "include/config.glsl"), true);
        Config.copyDefaults("defaults/shaders/include/vertex_body.glsl", new File(nminimapShadersDir, "include/vertex_body.glsl"), true);
        Config.copyDefaults("defaults/shaders/include/vertex_utils.glsl", new File(nminimapShadersDir, "include/vertex_utils.glsl"), true);
        Config.copyDefaults("defaults/shaders/include/fragment_body.glsl", new File(nminimapShadersDir, "include/fragment_body.glsl"), true);

        Files.write(Config.getResourceAsString("defaults/shaders/include/config.glsl")
                        .replace("{content}", String.valueOf(Config.mapPixelSize))
                        .replace("{offset-x}", String.valueOf(Config.mapDisplayOffsetX))
                        .replace("{offset-y}", String.valueOf(Config.mapDisplayOffsetY))
                        .replace("{scale}", String.valueOf(Config.mapDisplayScale))
                        .getBytes(StandardCharsets.UTF_8),
                new File(nminimapShadersDir, "include/config.glsl"));
    }

    private void writePackMcMeta(File resourcepackDir) throws Exception {
        List<PackMcMeta.Overlay> packOverlays = Lists.newArrayList();
        if (Config.packEnable1_21_11)
            packOverlays.add(new PackMcMeta.Overlay("nminimap_1_21_11", 75, 84, Config.packUseFormats ? new int[]{75, 84} : null));
        if (Config.packEnable26_1)
            packOverlays.add(new PackMcMeta.Overlay("nminimap_26_1", 84, 88, Config.packUseFormats ? new int[]{84, 88} : null));
        if (Config.packEnable26_2)
            packOverlays.add(new PackMcMeta.Overlay("nminimap_26_2", 88, 9999, Config.packUseFormats ? new int[]{88, 9999} : null));

        Files.write(new GsonBuilder().setPrettyPrinting().create().toJson(
                new PackMcMeta(
                        new PackMcMeta.Pack(Config.packDescription,
                                75, 9999, 75, Config.packUseFormats ? new int[]{75, 9999} : null),
                        new PackMcMeta.Overlays(packOverlays))
        ).getBytes(StandardCharsets.UTF_8), new File(resourcepackDir, "pack.mcmeta"));
    }
}
