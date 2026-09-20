package su.nezushin.nminimap.frames;

/**
 * One frame overlay drawn on the minimap.
 * {@code texture} is a file in {@code NMinimap/frames/} (without extension) or a packed variant id.
 * {@code zIndex} 0–127 draws behind the map; 128–255 draws in front.
 */
public class FrameLayer {

    private String texture;
    private int zIndex;

    public FrameLayer(String texture) {
        this(texture, 128);
    }

    public FrameLayer(String texture, int zIndex) {
        this.texture = texture;
        this.zIndex = zIndex;
    }

    public String getTexture() {
        return texture;
    }

    public void setTexture(String texture) {
        this.texture = texture;
    }

    public int getZIndex() {
        return zIndex;
    }

    public void setZIndex(int zIndex) {
        this.zIndex = zIndex;
    }
}
