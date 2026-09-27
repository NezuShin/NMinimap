package su.nezushin.nminimap.frames;

import java.util.List;

/**
 * Frame from the {@code frames} config section, with both layer lists packed into the resourcepack.
 * Layers that failed to pack are not present here.
 * <p>
 * Layers are shared between all players, use {@link FrameLayer#copy()} before changing them.
 */
public record Frame(String name, boolean usePermission, List<FrameLayer> squareLayers, List<FrameLayer> roundLayers) {

    public Frame {
        squareLayers = List.copyOf(squareLayers);
        roundLayers = List.copyOf(roundLayers);
    }

    public List<FrameLayer> layers(boolean isRound) {
        return isRound ? roundLayers : squareLayers;
    }
}
