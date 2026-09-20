package su.nezushin.nminimap.util.config;

public record FrameLayerDefinition(
        String texture,
        boolean isRound,
        boolean rotateWithPlayer,
        int inset,
        int offsetX,
        int offsetY,
        Integer zIndex
) {

    public String packedId() {
        return texture + "_" + (isRound ? "round" : "square")
                + "_rot" + (rotateWithPlayer ? 1 : 0)
                + "_in" + inset
                + "_x" + offsetX
                + "_y" + offsetY;
    }

    public static int resolveZIndex(Integer configured, int index) {
        int z = configured != null ? configured : 128 + index;
        return Math.max(0, Math.min(255, z));
    }
}
