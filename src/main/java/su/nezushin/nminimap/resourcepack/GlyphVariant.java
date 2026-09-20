package su.nezushin.nminimap.resourcepack;

import java.util.List;

/**
 * Image variant baked for one combination of screen side and map style.
 *
 * @param suffix appended to the image name
 * @param colors corner ids counterclockwise, read by the shader to tell the variants apart
 */
public record GlyphVariant(String suffix, List<Integer> colors) {

    public static final GlyphVariant SQUARE_RIGHT = new GlyphVariant("_r", List.of(1, 2, 3, 4));
    public static final GlyphVariant SQUARE_LEFT = new GlyphVariant("_l", List.of(5, 6, 7, 8));
    public static final GlyphVariant ROUND_RIGHT = new GlyphVariant("_r_round", List.of(9, 10, 11, 12));
    public static final GlyphVariant ROUND_LEFT = new GlyphVariant("_l_round", List.of(13, 14, 15, 16));

    /**
     * Right side first, then left. Markers are packed for both map styles, frames only for their own.
     */
    public static final GlyphVariant[] MARKER = {SQUARE_RIGHT, SQUARE_LEFT, ROUND_RIGHT, ROUND_LEFT};
    public static final GlyphVariant[] SQUARE_FRAME = {SQUARE_RIGHT, SQUARE_LEFT};
    public static final GlyphVariant[] ROUND_FRAME = {ROUND_RIGHT, ROUND_LEFT};
}
