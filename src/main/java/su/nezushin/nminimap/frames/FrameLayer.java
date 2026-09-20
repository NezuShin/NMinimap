package su.nezushin.nminimap.frames;

import org.jetbrains.annotations.ApiStatus;

/**
 * One frame overlay drawn on the minimap.
 * <p>
 * Layers are packed into the resourcepack on plugin load, so texture, type, offsets, inset,
 * rotate-with-player and inverse-rotation cannot change at runtime. Obtain a layer from
 * {@link FrameManager#getFrame(String)} or {@link FrameManager#getLayers(String)} and change
 * {@link #setZIndex(int)} / {@link #setRotation(int)} as you like.
 * <p>
 * Layers handed out by {@link FrameManager} are shared between all players. The copy passed to
 * {@code AsyncFrameRenderEvent} is yours to mutate; anything else should be changed through
 * {@link #copy()}.
 */
public class FrameLayer {

    private final String texture;
    private final String id;
    private final boolean round;
    private final boolean rotateWithPlayer;
    private final boolean inverseRotation;
    private final int inset;
    private final int offsetX;
    private final int offsetY;

    private final String symbolRight;
    private final String symbolLeft;

    private int zIndex;
    private int rotation;

    FrameLayer(String texture, String id, boolean round, boolean rotateWithPlayer, boolean inverseRotation,
               int inset, int offsetX, int offsetY, String symbolRight, String symbolLeft, int zIndex) {
        this.texture = texture;
        this.id = id;
        this.round = round;
        this.rotateWithPlayer = rotateWithPlayer;
        this.inverseRotation = inverseRotation;
        this.inset = inset;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.symbolRight = symbolRight;
        this.symbolLeft = symbolLeft;
        setZIndex(zIndex);
    }

    private FrameLayer(FrameLayer other) {
        this.texture = other.texture;
        this.id = other.id;
        this.round = other.round;
        this.rotateWithPlayer = other.rotateWithPlayer;
        this.inverseRotation = other.inverseRotation;
        this.inset = other.inset;
        this.offsetX = other.offsetX;
        this.offsetY = other.offsetY;
        this.symbolRight = other.symbolRight;
        this.symbolLeft = other.symbolLeft;
        this.zIndex = other.zIndex;
        this.rotation = other.rotation;
    }

    public FrameLayer copy() {
        return new FrameLayer(this);
    }

    /**
     * Name of the image in {@code NMinimap/frames/}, without extension.
     */
    public String getTexture() {
        return texture;
    }

    /**
     * Identifies the packed variant of {@link #getTexture()}. Two layers using the same image with
     * different offsets or inset have different ids.
     */
    public String getId() {
        return id;
    }

    public boolean isRound() {
        return round;
    }

    public boolean isRotateWithPlayer() {
        return rotateWithPlayer;
    }

    /**
     * Flips yaw-follow direction. Only applies when {@link #isRotateWithPlayer()} is true.
     */
    public boolean isInverseRotation() {
        return inverseRotation;
    }

    public int getInset() {
        return inset;
    }

    public int getOffsetX() {
        return offsetX;
    }

    public int getOffsetY() {
        return offsetY;
    }

    /**
     * 0-127 draws behind the map, 128-255 in front. Values outside the range are clamped.
     */
    public int getZIndex() {
        return zIndex;
    }

    public void setZIndex(int zIndex) {
        this.zIndex = Math.max(0, Math.min(255, zIndex));
    }

    /**
     * Rotation on screen, 0-255 for a full turn, 0 points up. Applied on top of
     * {@link #isRotateWithPlayer()}. Values outside the range wrap around.
     */
    public int getRotation() {
        return rotation;
    }

    public void setRotation(int rotation) {
        this.rotation = Math.floorMod(rotation, 256);
    }

    @ApiStatus.Internal
    public String symbol(boolean isRight) {
        return isRight ? symbolRight : symbolLeft;
    }

    /**
     * The shader rotates round layers the opposite way, so they get a mirrored value to keep
     * {@link #getRotation()} pointing the same direction for both types.
     */
    @ApiStatus.Internal
    public int rotationChannel() {
        return round ? (256 - rotation) % 256 : rotation;
    }

    @Override
    public String toString() {
        return "FrameLayer{" +
                "texture='" + texture + '\'' +
                ", id='" + id + '\'' +
                ", round=" + round +
                ", rotateWithPlayer=" + rotateWithPlayer +
                ", inverseRotation=" + inverseRotation +
                ", inset=" + inset +
                ", offsetX=" + offsetX +
                ", offsetY=" + offsetY +
                ", symbolRight='" + symbolRight + '\'' +
                ", symbolLeft='" + symbolLeft + '\'' +
                ", zIndex=" + zIndex +
                ", rotation=" + rotation +
                '}';
    }
}
