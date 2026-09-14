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
    private Map<String, String[]> frameImages = new HashMap<>();

    public MarkerImageManager() {
        load();
    }

    /**
     * Determines marker type:
     * for left and right screen side;
     * for round and square map
     *
     * @param suffix
     * @param colors - list of marker colors counterclockwise
     */
    private record MarkerType(String suffix, List<Integer> colors) {

    }

    public void load() {
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


            if (Config.resourcepackCopyDefaults) {
                for (var i : new String[]{"player", "player_small", "white_banner", "red_marker", "skeleton", "spider", "zombie"})
                    Config.copyDefaults("defaults/markers/" + i + ".png", new File(markersDir, i + ".png"), false);

                var niminimapShadersDir = new File(namespaceDir, "shaders");


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


                //Config.copyDefaults("defaults/shaders/include/config.glsl", new File(niminimapShadersDir, "include/config.glsl"), true);
                Config.copyDefaults("defaults/shaders/include/vertex_body.glsl", new File(niminimapShadersDir, "include/vertex_body.glsl"), true);
                Config.copyDefaults("defaults/shaders/include/vertex_utils.glsl", new File(niminimapShadersDir, "include/vertex_utils.glsl"), true);
                Config.copyDefaults("defaults/shaders/include/fragment_body.glsl", new File(niminimapShadersDir, "include/fragment_body.glsl"), true);

                Files.write(Config.getResourceAsString("defaults/shaders/include/config.glsl")
                                .replace("{content}", String.valueOf(Config.mapPixelSize))
                                .getBytes(StandardCharsets.UTF_8),
                        new File(niminimapShadersDir, "include/config.glsl"));
            }
            if (Config.packMcMetaChangeEnabled) {
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


            for (var i : markersDir.listFiles()) {
                var img = ImageIO.read(i);

                var markerImageName = getNameWithoutExt(i);
                var images = new String[4];
                var k = 0;
                //new Color(1.0f / 255.0f, 1.0f / 255.0f, 0.0f, 1.0f / 100f)
                for (var j : new MarkerType[]{
                        //right for square map
                        new MarkerType("_r", Lists.newArrayList(1, 2, 3, 4)),

                        //left for square map
                        new MarkerType("_l", Lists.newArrayList(5, 6, 7, 8)),

                        //left for round map
                        new MarkerType("_r_round", Lists.newArrayList(9, 10, 11, 12)),

                        //right for round map
                        new MarkerType("_l_round", Lists.newArrayList(13, 14, 15, 16))
                }) {
                    var imgName = markerImageName + j.suffix();
                    ImageCanvasUtil.processPng(img, j.colors(), new File(texturesDir, imgName + ".png"), Config.getMarkerSize(markerImageName), 1);

                    var symbol = String.valueOf((char) cache.getOrCreateFontImageId(imgName));
                    cache.getRegisteredCharIds().put(imgName, new BitmapFontImage(9, 8, "nminimap:font/" + imgName + ".png", symbol));
                    images[k++] = symbol;
                }
                markerImages.put(markerImageName, images);
            }

            var frameFiles = framesDir.listFiles();
            if (frameFiles != null) {
                Map<String, File[]> groupedFrames = new HashMap<>();
                for (var i : frameFiles) {
                    if (!i.isFile() || i.getName().lastIndexOf('.') < 1) {
                        continue;
                    }
                    var fileName = getNameWithoutExt(i);
                    String baseName;
                    int slot;
                    if (fileName.endsWith("_square") && fileName.length() > "_square".length()) {
                        baseName = fileName.substring(0, fileName.length() - "_square".length());
                        slot = 0;
                    } else if (fileName.endsWith("_round") && fileName.length() > "_round".length()) {
                        baseName = fileName.substring(0, fileName.length() - "_round".length());
                        slot = 1;
                    } else {
                        continue;
                    }
                    groupedFrames.computeIfAbsent(baseName, k -> new File[2])[slot] = i;
                }

                var logger = NMinimap.getInstance().getLogger();
                for (var entry : groupedFrames.entrySet()) {
                    var frameName = entry.getKey();
                    var files = entry.getValue();
                    var images = new String[4];
                    var rotateWithPlayer = Config.getFrameRotateWithPlayer(frameName);
                    var inset = Config.getFrameInset(frameName);

                    if (files[0] != null) {
                        var img = ImageIO.read(files[0]);
                        if (img != null) {
                            var k = 0;
                            for (var j : new MarkerType[]{
                                    new MarkerType("_r", Lists.newArrayList(1, 2, 3, 4)),
                                    new MarkerType("_l", Lists.newArrayList(5, 6, 7, 8))
                            }) {
                                var imgName = frameName + j.suffix();
                                ImageCanvasUtil.processPng(img, j.colors(), new File(texturesDir, imgName + ".png"), null, 2);

                                var symbol = String.valueOf((char) cache.getOrCreateFontImageId(imgName));
                                cache.getRegisteredCharIds().put(imgName, new BitmapFontImage(9, 8, "nminimap:font/" + imgName + ".png", symbol));
                                images[k++] = symbol;
                            }
                        }
                    } else {
                        logger.warning("Frame \"" + frameName + "\" is missing " + frameName + "_square.png");
                    }

                    if (files[1] != null) {
                        var img = ImageIO.read(files[1]);
                        if (img != null) {
                            var k = 2;
                            for (var j : new MarkerType[]{
                                    new MarkerType("_r_round", Lists.newArrayList(9, 10, 11, 12)),
                                    new MarkerType("_l_round", Lists.newArrayList(13, 14, 15, 16))
                            }) {
                                var imgName = frameName + j.suffix();
                                if (!ImageCanvasUtil.processFramePng(img, j.colors(), new File(texturesDir, imgName + ".png"),
                                        rotateWithPlayer, inset)) {
                                    logger.severe(
                                            "Frame \"" + frameName + "\" is too large to pack (max 256x256 after slicing into 256px rows)!");
                                    break;
                                }

                                var symbol = String.valueOf((char) cache.getOrCreateFontImageId(imgName));
                                cache.getRegisteredCharIds().put(imgName, new BitmapFontImage(9, 8, "nminimap:font/" + imgName + ".png", symbol));
                                images[k++] = symbol;
                            }
                        }
                    } else {
                        logger.warning("Frame \"" + frameName + "\" is missing " + frameName + "_round.png");
                    }

                    frameImages.put(frameName, images);
                }
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

    public String getFrameIcon(String image, boolean isRight, boolean isRoundMap) {
        var images = frameImages.get(image);
        return images == null ? null : images[(isRoundMap ? 2 : 0) + (isRight ? 0 : 1)];
    }

    public Map<String, String[]> getFrameImages() {
        return frameImages;
    }

    private String getNameWithoutExt(File f) {
        var name = f.getName();
        return name.substring(0, name.lastIndexOf("."));
    }
}
