package su.nezushin.nminimap.markers;

import org.jetbrains.annotations.ApiStatus;
import su.nezushin.nminimap.resourcepack.GlyphVariant;
import su.nezushin.nminimap.resourcepack.PackContext;
import su.nezushin.nminimap.util.ImageCanvasUtil;
import su.nezushin.nminimap.util.config.Config;

import javax.imageio.ImageIO;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry of marker icons packed into the resourcepack.
 */
public class MarkerManager {

    private Map<String, String[]> markerImages = new HashMap<>();

    /**
     * @param image marker image name in {@code NMinimap/markers/}, without extension
     * @return the character to print the marker with, or null when there is no such marker
     */
    public String getMarkerIcon(String image, boolean isRight, boolean isRoundMap) {
        var symbols = markerImages.get(image);
        return symbols == null ? null : symbols[(isRoundMap ? 2 : 0) + (isRight ? 0 : 1)];
    }

    public boolean hasMarker(String image) {
        return markerImages.containsKey(image);
    }

    public Set<String> getMarkerNames() {
        return markerImages.keySet();
    }

    @ApiStatus.Internal
    public void bake(PackContext context, File markersDir) {
        Map<String, String[]> markerImages = new HashMap<>();
        var files = markersDir.listFiles();
        if (files == null) {
            this.markerImages = markerImages;
            return;
        }

        for (var file : files) {
            var name = file.getName();
            var dot = name.lastIndexOf('.');
            if (!file.isFile() || dot < 1)
                continue;
            var markerName = name.substring(0, dot);

            try {
                var image = ImageIO.read(file);
                if (image == null) {
                    context.logger().severe("Marker image \"" + markerName + "\" could not be read!");
                    continue;
                }

                var symbols = new String[GlyphVariant.MARKER.length];
                for (var i = 0; i < GlyphVariant.MARKER.length; i++) {
                    var imageName = markerName + GlyphVariant.MARKER[i].suffix();
                    ImageCanvasUtil.processPng(image, GlyphVariant.MARKER[i].colors(), context.imageFile(imageName),
                            Config.getMarkerSize(markerName), 1);
                    symbols[i] = context.registerGlyph(imageName);
                }
                markerImages.put(markerName, symbols);
            } catch (Exception ex) {
                context.logger().severe("Marker image \"" + markerName + "\" could not be packed: " + ex.getMessage());
            }
        }

        this.markerImages = markerImages;
    }
}
