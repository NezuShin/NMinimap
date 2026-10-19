package su.nezushin.nminimap.chunks;

import java.util.List;

/**
 * Multi-layer chunk tile. Each cell stores its open spaces from the top down: layer 0 is the space under the sky
 * (or under the skipped ceiling), deeper indices are caves and lakes below it.
 */
public record ChunkTile(List<LayerData> layers, TileBakeSettings bakeSettings) {

    /** Placeholder tile with no bake recipe. */
    public ChunkTile(List<LayerData> layers) {
        this(layers, null);
    }

    public int layerCount() {
        return layers.size();
    }

    /**
     * Layer whose space contains {@code y} ({@code floorY <= y <= topY}), or -1 when {@code y} is inside solid blocks.
     */
    public int containingIndex(int scale, int indexXX, int indexZZ, int chunkSize, int y) {
        for (int i = 0; i < layers.size(); i++) {
            var layer = layers.get(i);
            short floorY = layer.floorAt(scale, indexXX, indexZZ, chunkSize);
            if (floorY != LayerData.NO_LAYER && floorY <= y && y <= layer.topAt(scale, indexXX, indexZZ, chunkSize))
                return i;
        }
        return -1;
    }

    /**
     * Deepest layer with {@code floorY > y}: the floor on top of the solid mass {@code y} is inside, or -1.
     */
    public int aboveIndex(int scale, int indexXX, int indexZZ, int chunkSize, int y) {
        int best = -1;
        int bestFloor = Integer.MAX_VALUE;
        for (int i = 0; i < layers.size(); i++) {
            short floorY = layers.get(i).floorAt(scale, indexXX, indexZZ, chunkSize);
            if (floorY != LayerData.NO_LAYER && floorY > y && floorY < bestFloor) {
                bestFloor = floorY;
                best = i;
            }
        }
        return best;
    }

    public byte colorAt(int layerIndex, int scale, int indexXX, int indexZZ, int chunkSize) {
        if (layerIndex < 0 || layerIndex >= layers.size())
            return 0;
        return layers.get(layerIndex).colorAt(scale, indexXX, indexZZ, chunkSize);
    }

    public short drawY(int layerIndex, int scale, int indexXX, int indexZZ, int chunkSize) {
        if (layerIndex < 0 || layerIndex >= layers.size())
            return LayerData.NO_LAYER;
        return layers.get(layerIndex).drawAt(scale, indexXX, indexZZ, chunkSize);
    }

    public short floorY(int layerIndex, int scale, int indexXX, int indexZZ, int chunkSize) {
        if (layerIndex < 0 || layerIndex >= layers.size())
            return LayerData.NO_LAYER;
        return layers.get(layerIndex).floorAt(scale, indexXX, indexZZ, chunkSize);
    }

    /** {@link LayerData#SKY} when the layer is open to the sky. */
    public short topY(int layerIndex, int scale, int indexXX, int indexZZ, int chunkSize) {
        if (layerIndex < 0 || layerIndex >= layers.size())
            return LayerData.NO_LAYER;
        return layers.get(layerIndex).topAt(scale, indexXX, indexZZ, chunkSize);
    }
}
