package su.nezushin.nminimap.resourcepack;

import su.nezushin.nminimap.resourcepack.cache.FontImageIdCache;
import su.nezushin.nminimap.resourcepack.font.BitmapFontImage;

import java.io.File;
import java.util.logging.Logger;

/**
 * Passed to every pack build pass. The font cache is owned by {@link ResourcepackManager}, which
 * writes and saves it once all passes are done.
 */
public record PackContext(File texturesDir, FontImageIdCache cache, Logger logger) {

    /**
     * Assigns a character to an already written image and registers it in the font.
     *
     * @param imageName image in {@link #texturesDir()}, without extension
     * @return the character to print the image with
     */
    public String registerGlyph(String imageName) {
        var symbol = String.valueOf((char) cache.getOrCreateFontImageId(imageName));
        cache.getRegisteredCharIds().put(imageName, new BitmapFontImage(9, 8, "nminimap:font/" + imageName + ".png", symbol));
        return symbol;
    }

    public File imageFile(String imageName) {
        return new File(texturesDir, imageName + ".png");
    }
}
