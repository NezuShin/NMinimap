package su.nezushin.nminimap.frames;

import org.jetbrains.annotations.ApiStatus;
import su.nezushin.nminimap.resourcepack.GlyphVariant;
import su.nezushin.nminimap.resourcepack.PackContext;
import su.nezushin.nminimap.util.ImageCanvasUtil;
import su.nezushin.nminimap.util.config.FrameDefinition;
import su.nezushin.nminimap.util.config.FrameLayerDefinition;

import javax.imageio.ImageIO;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Registry of frames packed into the resourcepack.
 * <p>
 * Everything here is shared between all players, so {@link FrameLayer#copy()} anything you intend
 * to change.
 */
public class FrameManager {

    private Map<String, Frame> frames = Map.of();
    private List<String> frameNames = List.of();
    private Map<String, List<FrameLayer>> layersByTexture = Map.of();
    private List<FrameLayer> layers = List.of();

    /**
     * @param name frame name from the config, case insensitive
     * @return null when there is no such frame
     */
    public Frame getFrame(String name) {
        return name == null ? null : frames.get(name.toLowerCase(Locale.ROOT));
    }

    /**
     * Names of all frames, in config order.
     */
    public List<String> getFrameNames() {
        return frameNames;
    }

    /**
     * Every packed variant of one image, in the order they were packed. An image used by several
     * frames with different offsets or inset has one entry per variant.
     *
     * @param texture image name in {@code NMinimap/frames/}, without extension, case insensitive
     */
    public List<FrameLayer> getLayers(String texture) {
        if (texture == null)
            return List.of();
        return layersByTexture.getOrDefault(texture.toLowerCase(Locale.ROOT), List.of());
    }

    /**
     * Every packed variant of every image.
     */
    public List<FrameLayer> getLayers() {
        return layers;
    }

    @ApiStatus.Internal
    public void bake(PackContext context, File framesDir, Map<String, FrameDefinition> definitions) {
        Map<String, File> textureFiles = new HashMap<>();
        var files = framesDir.listFiles();
        if (files != null)
            for (var file : files) {
                var name = file.getName();
                var dot = name.lastIndexOf('.');
                if (!file.isFile() || dot < 1)
                    continue;
                textureFiles.put(name.substring(0, dot).toLowerCase(Locale.ROOT), file);
            }

        Map<String, Frame> frames = new HashMap<>();
        List<String> frameNames = new ArrayList<>();
        Map<String, List<FrameLayer>> layersByTexture = new LinkedHashMap<>();
        List<FrameLayer> layers = new ArrayList<>();
        Map<String, String[]> symbolsById = new HashMap<>();

        for (var definition : definitions.values()) {
            var square = bakeLayers(definition.squareLayers(), context, textureFiles, symbolsById, layersByTexture, layers);
            var round = bakeLayers(definition.roundLayers(), context, textureFiles, symbolsById, layersByTexture, layers);

            frames.put(definition.name().toLowerCase(Locale.ROOT),
                    new Frame(definition.name(), definition.usePermission(), square, round));
            frameNames.add(definition.name());
        }

        layersByTexture.replaceAll((texture, variants) -> Collections.unmodifiableList(variants));

        this.frames = frames;
        this.frameNames = List.copyOf(frameNames);
        this.layersByTexture = layersByTexture;
        this.layers = Collections.unmodifiableList(layers);
    }

    private List<FrameLayer> bakeLayers(List<FrameLayerDefinition> definitions, PackContext context,
                                        Map<String, File> textureFiles, Map<String, String[]> symbolsById,
                                        Map<String, List<FrameLayer>> layersByTexture, List<FrameLayer> layers) {
        List<FrameLayer> result = new ArrayList<>();

        for (var index = 0; index < definitions.size(); index++) {
            var definition = definitions.get(index);
            var id = definition.packedId();
            var zIndex = FrameLayerDefinition.resolveZIndex(definition.zIndex(), index);

            var symbols = symbolsById.get(id);
            if (symbols == null) {
                symbols = packVariants(definition, id, context, textureFiles);
                if (symbols == null)
                    continue;
                symbolsById.put(id, symbols);

                //Registry entries keep the default z-index, frames keep the one from their config
                var registered = newLayer(definition, id, symbols, 128);
                layers.add(registered);
                layersByTexture.computeIfAbsent(definition.texture().toLowerCase(Locale.ROOT), k -> new ArrayList<>())
                        .add(registered);
            }

            result.add(newLayer(definition, id, symbols, zIndex));
        }

        return result;
    }

    private FrameLayer newLayer(FrameLayerDefinition definition, String id, String[] symbols, int zIndex) {
        return new FrameLayer(definition.texture(), id, definition.isRound(), definition.rotateWithPlayer(),
                definition.inset(), definition.offsetX(), definition.offsetY(), symbols[0], symbols[1], zIndex);
    }

    /**
     * Writes the left and right image of one layer.
     *
     * @return their symbols, right first, or null when the image is missing or cannot be packed
     */
    private String[] packVariants(FrameLayerDefinition definition, String id, PackContext context,
                                  Map<String, File> textureFiles) {
        var logger = context.logger();

        var file = textureFiles.get(definition.texture().toLowerCase(Locale.ROOT));
        if (file == null) {
            logger.severe("Frame texture \"" + definition.texture() + "\" is not found!");
            return null;
        }

        try {
            var image = ImageIO.read(file);
            if (image == null) {
                logger.severe("Frame texture \"" + definition.texture() + "\" could not be read!");
                return null;
            }

            var variants = definition.isRound() ? GlyphVariant.ROUND_FRAME : GlyphVariant.SQUARE_FRAME;
            var symbols = new String[variants.length];

            for (var i = 0; i < variants.length; i++) {
                var imageName = id + variants[i].suffix();
                boolean packed;

                if (definition.isRound()) {
                    packed = ImageCanvasUtil.processFramePng(image, variants[i].colors(), context.imageFile(imageName),
                            definition.rotateWithPlayer(), definition.inset());
                    if (!packed)
                        logger.severe("Frame texture \"" + definition.texture() + "\" is too large to pack (max 256x256 after slicing into 256px rows)!");
                } else {
                    packed = ImageCanvasUtil.processSquareFramePng(image, variants[i].colors(), context.imageFile(imageName),
                            definition.offsetX(), definition.offsetY(), definition.rotateWithPlayer());
                    if (!packed)
                        logger.severe("Frame texture \"" + definition.texture() + "\" has unsupported size (max 254x256, min height 5)!");
                }

                if (!packed)
                    return null;

                symbols[i] = context.registerGlyph(imageName);
            }

            return symbols;
        } catch (Exception ex) {
            logger.severe("Frame texture \"" + definition.texture() + "\" could not be packed: " + ex.getMessage());
            return null;
        }
    }
}
