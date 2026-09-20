package su.nezushin.nminimap.resourcepack;

import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.gson.GsonBuilder;
import su.nezushin.nminimap.NMinimap;
import su.nezushin.nminimap.resourcepack.cache.FontImageIdCache;
import su.nezushin.nminimap.resourcepack.font.BitmapFontImage;
import su.nezushin.nminimap.resourcepack.packmcmeta.PackMcMeta;
import su.nezushin.nminimap.util.config.Config;
import su.nezushin.nminimap.util.config.FrameLayerDefinition;
import su.nezushin.nminimap.util.ImageCanvasUtil;
import su.nezushin.nminimap.util.ZipUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class MarkerImageManager {


    private Map<String, String[]> markerImages = new HashMap<>();
    private Map<String, String[]> layerSymbols = new HashMap<>();
    private Map<String, String> textureAliases = new HashMap<>();

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


            if (Config.resourcepackCopyMarkers) {
                for (var i : new String[]{"player", "player_small", "white_banner", "red_marker", "skeleton", "spider", "zombie"})
                    Config.copyDefaults("defaults/markers/" + i + ".png", new File(markersDir, i + ".png"), false);
            }

            if (Config.resourcepackCopyFrames) {
                for (var i : new String[]{"default", "inventory", "inventory_with_title"}) {
                    for (var j : new String[]{"square", "round"}) {
                        var filename = i + "_" + j + ".png";
                        Config.copyDefaults("defaults/frames/" + filename, new File(framesDir, filename), false);
                    }
                }
            }

            if (Config.resourcepackCopyShaders) {
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
                                .replace("{offset-x}", String.valueOf(Config.mapDisplayOffsetX))
                                .replace("{offset-y}", String.valueOf(Config.mapDisplayOffsetY))
                                .replace("{scale}", String.valueOf(Config.mapDisplayScale))
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
            Map<String, File> textureFiles = new HashMap<>();
            if (frameFiles != null) {
                for (var i : frameFiles) {
                    if (!i.isFile() || i.getName().lastIndexOf('.') < 1)
                        continue;
                    textureFiles.put(getNameWithoutExt(i), i);
                }
            }

            var logger = NMinimap.getInstance().getLogger();
            Map<String, Set<String>> packedIdsByTexture = new HashMap<>();
            for (var frame : Config.frames.values()) {
                packFrameLayers(frame.squareLayers(), textureFiles, texturesDir, cache, packedIdsByTexture, logger);
                packFrameLayers(frame.roundLayers(), textureFiles, texturesDir, cache, packedIdsByTexture, logger);
            }
            for (var entry : packedIdsByTexture.entrySet()) {
                if (entry.getValue().size() == 1)
                    textureAliases.put(entry.getKey(), entry.getValue().iterator().next());
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

    public String getLayerSymbol(String texture, boolean isRight) {
        var images = lookupLayerSymbols(texture);
        return images == null ? null : images[isRight ? 0 : 1];
    }

    public Set<String> getFrameNames() {
        return Config.frames.keySet();
    }

    private String[] lookupLayerSymbols(String texture) {
        if (texture == null)
            return null;
        var images = layerSymbols.get(texture);
        if (images != null)
            return images;
        var aliased = textureAliases.get(texture);
        if (aliased != null)
            return layerSymbols.get(aliased);
        for (var entry : layerSymbols.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(texture))
                return entry.getValue();
        }
        for (var entry : textureAliases.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(texture))
                return layerSymbols.get(entry.getValue());
        }
        return null;
    }

    private void packFrameLayers(List<FrameLayerDefinition> layers, Map<String, File> textureFiles, File texturesDir,
                                 FontImageIdCache cache, Map<String, Set<String>> packedIdsByTexture,
                                 Logger logger) throws Exception {
        for (var layer : layers) {
            var packedId = layer.packedId();
            if (layerSymbols.containsKey(packedId)) {
                packedIdsByTexture.computeIfAbsent(layer.texture(), k -> new HashSet<>()).add(packedId);
                continue;
            }

            var file = textureFiles.get(layer.texture());
            if (file == null) {
                file = textureFiles.entrySet().stream()
                        .filter(e -> e.getKey().equalsIgnoreCase(layer.texture()))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElse(null);
            }
            if (file == null) {
                logger.severe("Frame texture \"" + layer.texture() + "\" is not found!");
                continue;
            }
            BufferedImage img = ImageIO.read(file);
            if (img == null) {
                logger.severe("Frame texture \"" + layer.texture() + "\" could not be read!");
                continue;
            }

            MarkerType[] types;
            if (layer.isRound()) {
                types = new MarkerType[]{
                        new MarkerType("_r_round", Lists.newArrayList(9, 10, 11, 12)),
                        new MarkerType("_l_round", Lists.newArrayList(13, 14, 15, 16))
                };
            } else {
                types = new MarkerType[]{
                        new MarkerType("_r", Lists.newArrayList(1, 2, 3, 4)),
                        new MarkerType("_l", Lists.newArrayList(5, 6, 7, 8))
                };
            }

            var images = new String[2];
            var packed = true;
            for (var k = 0; k < types.length; k++) {
                var markerType = types[k];
                var imgName = packedId + markerType.suffix();
                boolean ok;
                if (!layer.isRound()) {
                    ok = ImageCanvasUtil.processSquareFramePng(img, markerType.colors(), new File(texturesDir, imgName + ".png"),
                            layer.offsetX(), layer.offsetY(), layer.rotateWithPlayer());
                    if (!ok)
                        logger.severe("Frame texture \"" + layer.texture() + "\" has unsupported size (max 254x256, min height 5)!");
                } else {
                    ok = ImageCanvasUtil.processFramePng(img, markerType.colors(), new File(texturesDir, imgName + ".png"),
                            layer.rotateWithPlayer(), layer.inset());
                    if (!ok)
                        logger.severe("Frame texture \"" + layer.texture() + "\" is too large to pack (max 256x256 after slicing into 256px rows)!");
                }
                if (!ok) {
                    packed = false;
                    break;
                }
                var symbol = String.valueOf((char) cache.getOrCreateFontImageId(imgName));
                cache.getRegisteredCharIds().put(imgName, new BitmapFontImage(9, 8, "nminimap:font/" + imgName + ".png", symbol));
                images[k] = symbol;
            }
            if (packed) {
                layerSymbols.put(packedId, images);
                packedIdsByTexture.computeIfAbsent(layer.texture(), k -> new HashSet<>()).add(packedId);
            }
        }
    }

    private String getNameWithoutExt(File f) {
        var name = f.getName();
        return name.substring(0, name.lastIndexOf("."));
    }
}
