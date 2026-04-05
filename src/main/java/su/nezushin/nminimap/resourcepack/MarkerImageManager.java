package su.nezushin.nminimap.resourcepack;

import su.nezushin.nminimap.NMinimap;
import su.nezushin.nminimap.resourcepack.cache.FontImageIdCache;
import su.nezushin.nminimap.resourcepack.font.BitmapFontImage;
import su.nezushin.nminimap.util.config.Config;
import su.nezushin.nminimap.util.ImageCanvasUtil;
import su.nezushin.nminimap.util.ZipUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MarkerImageManager {


    Map<String, String[]> markerImages = new HashMap<>();

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
                Config.copyDefaults("defaults/markers/player.png", new File(markersDir, "player.png"));
                Config.copyDefaults("defaults/markers/player_small.png", new File(markersDir, "player_small.png"));
                Config.copyDefaults("defaults/pack.mcmeta", new File(resourcepackDir, "pack.mcmeta"));

                var shadersDir = new File(resourcepackDir, "assets/minecraft/shaders");

                Config.copyDefaults("defaults/shaders/core/rendertype_text.fsh", new File(shadersDir, "core/rendertype_text.fsh"));
                Config.copyDefaults("defaults/shaders/core/rendertype_text.vsh", new File(shadersDir, "core/rendertype_text.vsh"));
                Config.copyDefaults("defaults/shaders/include/minimap/config.glsl", new File(shadersDir, "include/minimap/config.glsl"));
                Config.copyDefaults("defaults/shaders/include/minimap/vertex_body.glsl", new File(shadersDir, "include/minimap/vertex_body.glsl"));
                Config.copyDefaults("defaults/shaders/include/minimap/vertex_utils.glsl", new File(shadersDir, "include/minimap/vertex_utils.glsl"));
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

            for(var i : Config.getResourcepackCopyDestinationFiles()) {
                ZipUtil.deleteDirectory(i);
                ZipUtil.copyDirectory(resourcepackDir, i);
            }
            for(var i : Config.getResourcepackZipDestinationFiles())
                ZipUtil.pack(resourcepackDir, i);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public String getMarkerIcon(String image, boolean isRight, boolean isRoundMap) {
        return markerImages.get(image)[(isRoundMap ? 2 : 0) + (isRight ? 0 : 1)];
    }

    private String getNameWithoutExt(File f) {
        var name = f.getName();
        return name.substring(0, name.lastIndexOf("."));
    }
}
