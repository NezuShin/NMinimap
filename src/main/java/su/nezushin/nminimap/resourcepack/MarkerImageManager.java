package su.nezushin.nminimap.resourcepack;

import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import su.nezushin.nminimap.NMinimap;
import su.nezushin.nminimap.resourcepack.cache.FontImageIdCache;
import su.nezushin.nminimap.resourcepack.font.BitmapFontImage;
import su.nezushin.nminimap.resourcepack.packmcmeta.PackMcMeta;
import su.nezushin.nminimap.util.config.Config;
import su.nezushin.nminimap.util.ImageCanvasUtil;
import su.nezushin.nminimap.util.ZipUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MarkerImageManager {


    private Map<String, String[]> markerImages = new HashMap<>();

    public MarkerImageManager() {
        load();
    }

    /**
     * Determines marker type:
     * for left and right screen side;
     * for round and square map
     *
     * @param suffix
     * @param color
     */
    private record MarkerType(String suffix, Color color) {

    }

    public void load() {
        try {
            var cache = FontImageIdCache.load();

            var resourcepackDir = new File(NMinimap.getInstance().getDataFolder(), "resourcepack");
            var namespaceDir = new File(resourcepackDir, "assets/nminimap/");
            var texturesDir = new File(namespaceDir, "textures/font/");
            var fontsDir = new File(namespaceDir, "font");

            var markersDir = new File(NMinimap.getInstance().getDataFolder(), "markers");

            texturesDir.mkdirs();
            fontsDir.mkdirs();
            markersDir.mkdirs();


            if (Config.resourcepackCopyDefaults) {
                Config.copyDefaults("defaults/markers/player.png", new File(markersDir, "player.png"), false);
                Config.copyDefaults("defaults/markers/player_small.png", new File(markersDir, "player_small.png"), false);
                Config.copyDefaults("defaults/markers/white_banner.png", new File(markersDir, "white_banner.png"), false);

                var niminimapShadersDir = new File(namespaceDir, "shaders");


                if (Config.packEnable1_21_11) {
                    Config.copyDefaults("defaults/shaders/core/v1_21_11/rendertype_text.fsh", new File(resourcepackDir, "nminimap_1_21_11/assets/minecraft/shaders/core/rendertype_text.fsh"), true);
                    Config.copyDefaults("defaults/shaders/core/v1_21_11/rendertype_text.vsh", new File(resourcepackDir, "nminimap_1_21_11/assets/minecraft/shaders/core/rendertype_text.vsh"), true);
                }

                if (Config.packEnable26_1) {
                    Config.copyDefaults("defaults/shaders/core/v26_1/rendertype_text.fsh", new File(resourcepackDir, "nminimap_26_1/assets/minecraft/shaders/core/rendertype_text.fsh"), true);
                    Config.copyDefaults("defaults/shaders/core/v26_1/rendertype_text.vsh", new File(resourcepackDir, "nminimap_26_1/assets/minecraft/shaders/core/rendertype_text.vsh"), true);
                }


                //Config.copyDefaults("defaults/shaders/include/config.glsl", new File(niminimapShadersDir, "include/config.glsl"), true);
                Config.copyDefaults("defaults/shaders/include/vertex_body.glsl", new File(niminimapShadersDir, "include/vertex_body.glsl"), true);
                Config.copyDefaults("defaults/shaders/include/vertex_utils.glsl", new File(niminimapShadersDir, "include/vertex_utils.glsl"), true);
                Config.copyDefaults("defaults/shaders/include/fragment_body.glsl", new File(niminimapShadersDir, "include/fragment_body.glsl"), true);

                Files.write(Config.getResourceAsString("defaults/shaders/include/config.glsl")
                                .replace("{radius}", String.valueOf((int) Math.floor(((double) Config.mapPixelSize) / 2.0)))
                                .getBytes(StandardCharsets.UTF_8),
                        new File(niminimapShadersDir, "include/config.glsl"));
            }
            if (Config.packMcMetaChangeEnabled) {
                List<PackMcMeta.Overlay> packOverlays = Lists.newArrayList();
                if (Config.packEnable1_21_11)
                    packOverlays.add(new PackMcMeta.Overlay("nminimap_1_21_11", 75, 84, new int[]{75, 84}));
                if (Config.packEnable26_1)
                    packOverlays.add(new PackMcMeta.Overlay("nminimap_26_1", 84, 9999, new int[]{84, 9999}));
                Files.write(new GsonBuilder().setPrettyPrinting().create().toJson(
                        new PackMcMeta(
                                new PackMcMeta.Pack(Config.packDescription,
                                        75, 9999, new int[]{75, 9999}),
                                new PackMcMeta.Overlays(packOverlays))
                ).getBytes(StandardCharsets.UTF_8), new File(resourcepackDir, "pack.mcmeta"));
            }


            for (var i : markersDir.listFiles()) {
                var img = ImageIO.read(i);

                var markerImageName = getNameWithoutExt(i);
                var images = new String[4];
                var k = 0;
                for (var j : new MarkerType[]{
                        //right for square map
                        new MarkerType("_r",
                                new Color(1.0f / 255.0f, 1.0f / 255.0f, 0.0f, 1.0f / 100f)),

                        //left for square map
                        new MarkerType("_l",
                                new Color(2.0f / 255.0f, 1.0f / 255.0f, 0.0f, 1.0f / 100f)),

                        //left for round map
                        new MarkerType("_r_round",
                                new Color(3.0f / 255.0f, 1.0f / 255.0f, 0.0f, 1.0f / 100f)),

                        //right for round map
                        new MarkerType("_l_round",
                                new Color(4.0f / 255.0f, 1.0f / 255.0f, 0.0f, 1.0f / 100f)),
                }) {
                    var imgName = markerImageName + j.suffix();
                    ImageCanvasUtil.processPng(img, j.color(), new File(texturesDir, imgName + ".png"));

                    var symbol = String.valueOf((char) cache.getOrCreateFontImageId(imgName));
                    cache.getRegisteredCharIds().put(imgName, new BitmapFontImage(9, 8, "nminimap:font/" + imgName + ".png", symbol));
                    images[k++] = symbol;
                }
                markerImages.put(markerImageName, images);
            }

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

    public String getMarkerIcon(String image, boolean isRight, boolean isRoundMap) {
        return markerImages.get(image)[(isRoundMap ? 2 : 0) + (isRight ? 0 : 1)];
    }

    public Map<String, String[]> getMarkerImages() {
        return markerImages;
    }

    private String getNameWithoutExt(File f) {
        var name = f.getName();
        return name.substring(0, name.lastIndexOf("."));
    }
}
