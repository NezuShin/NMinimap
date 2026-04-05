package su.nezushin.nminimap.resourcepack.font;

import java.util.List;
/**
 * Used for resource pack json serialization
 */
public class FontImageProviders {

    private List<BitmapFontImage> providers;

    public FontImageProviders(List<BitmapFontImage> providers) {
        this.providers = providers;
    }

    public List<BitmapFontImage> getProviders() {
        return providers;
    }
}
