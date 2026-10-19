package su.nezushin.nminimap.util;

import su.nezushin.nminimap.chunks.ChunkTile;
import su.nezushin.nminimap.chunks.LayerData;
import su.nezushin.nminimap.chunks.TileBakeSettings;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapDataUtil {

    /** v6: v5 grids plus the bake settings that produced the tile. */
    public static final int CACHE_VERSION = 6;

    private static final int[] SCALES = {1, 2, 4, 8};

    private static byte[] prepareEmptyMapForScale(int size) {
        var arr = new byte[size * size];
        Arrays.fill(arr, (byte) 0);
        return arr;
    }

    private static short[] prepareEmptyHeightsForScale(int size) {
        var arr = new short[size * size];
        Arrays.fill(arr, LayerData.NO_LAYER);
        return arr;
    }

    public static Map<Integer, byte[]> prepareEmptyScales() {
        Map<Integer, byte[]> scales = new HashMap<>();
        scales.put(1, prepareEmptyMapForScale(16));
        scales.put(2, prepareEmptyMapForScale(8));
        scales.put(4, prepareEmptyMapForScale(4));
        scales.put(8, prepareEmptyMapForScale(2));
        return scales;
    }

    public static Map<Integer, short[]> prepareEmptyHeights() {
        Map<Integer, short[]> heights = new HashMap<>();
        heights.put(1, prepareEmptyHeightsForScale(16));
        heights.put(2, prepareEmptyHeightsForScale(8));
        heights.put(4, prepareEmptyHeightsForScale(4));
        heights.put(8, prepareEmptyHeightsForScale(2));
        return heights;
    }

    public static ChunkTile prepareEmptyTile() {
        return new ChunkTile(List.of(new LayerData(prepareEmptyScales(), prepareEmptyHeights(), prepareEmptyHeights(),
                prepareEmptyHeights())));
    }

    private static byte[] loadMapForScale(int size, InputStream is) throws IOException {
        var arr = new byte[size * size];
        int read;
        if (arr.length != (read = is.read(arr, 0, arr.length))) {
            throw new IOException("Failed to read chunk data (" + read + " instead of " + arr.length + ")");
        }
        return arr;
    }

    private static short[] loadHeightsForScale(int size, DataInputStream in) throws IOException {
        short[] arr = new short[size * size];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = in.readShort();
        }
        return arr;
    }

    private static Map<Integer, byte[]> readScales(InputStream is) throws IOException {
        Map<Integer, byte[]> scales = new HashMap<>();
        scales.put(1, loadMapForScale(16, is));
        scales.put(2, loadMapForScale(8, is));
        scales.put(4, loadMapForScale(4, is));
        scales.put(8, loadMapForScale(2, is));
        return scales;
    }

    private static Map<Integer, short[]> readHeights(DataInputStream in) throws IOException {
        Map<Integer, short[]> heights = new HashMap<>();
        heights.put(1, loadHeightsForScale(16, in));
        heights.put(2, loadHeightsForScale(8, in));
        heights.put(4, loadHeightsForScale(4, in));
        heights.put(8, loadHeightsForScale(2, in));
        return heights;
    }

    private static void writeScales(Map<Integer, byte[]> scales, OutputStream os) throws IOException {
        os.write(scales.get(1));
        os.write(scales.get(2));
        os.write(scales.get(4));
        os.write(scales.get(8));
    }

    private static void writeHeights(Map<Integer, short[]> heights, DataOutputStream out) throws IOException {
        for (int scale : SCALES) {
            short[] grid = heights.get(scale);
            int expected = (16 / scale) * (16 / scale);
            if (grid == null || grid.length != expected) {
                throw new IOException("Invalid heights length for scale " + scale);
            }
            for (short y : grid) {
                out.writeShort(y);
            }
        }
    }

    public static ChunkTile readTile(InputStream is) throws IOException {
        DataInputStream in = new DataInputStream(is);
        int version = in.readUnsignedByte();
        if (version != CACHE_VERSION) {
            throw new IOException("Unsupported cache version " + version + " (expected " + CACHE_VERSION + ")");
        }
        var bakeSettings = readBakeSettings(in);
        int layerCount = in.readUnsignedByte();
        if (layerCount < 1 || layerCount > 64) {
            throw new IOException("Invalid layer count " + layerCount);
        }
        List<LayerData> layers = new ArrayList<>(layerCount);
        for (int i = 0; i < layerCount; i++) {
            var scales = readScales(in);
            var draws = readHeights(in);
            var floors = readHeights(in);
            var tops = readHeights(in);
            layers.add(new LayerData(scales, draws, floors, tops));
        }
        return new ChunkTile(layers, bakeSettings);
    }

    public static void saveTile(ChunkTile tile, OutputStream os) throws IOException {
        DataOutputStream out = new DataOutputStream(os);
        out.writeByte(CACHE_VERSION);
        if (tile.bakeSettings() == null) {
            throw new IOException("Cannot save a tile with no bake settings");
        }
        writeBakeSettings(tile.bakeSettings(), out);
        List<LayerData> layers = tile.layers();
        if (layers.isEmpty()) {
            throw new IOException("Cannot save empty ChunkTile");
        }
        out.writeByte(layers.size());
        for (LayerData layer : layers) {
            writeScales(layer.scales(), out);
            writeHeights(layer.draws(), out);
            writeHeights(layer.floors(), out);
            writeHeights(layer.tops(), out);
        }
        out.flush();
    }

    private static TileBakeSettings readBakeSettings(DataInputStream in) throws IOException {
        int maxLayers = in.readInt();
        int minAirGap = in.readInt();
        int minY = in.readInt();
        int maxY = in.readInt();
        int flags = in.readUnsignedByte();
        return new TileBakeSettings(maxLayers, minAirGap, minY, maxY,
                (flags & 1) != 0, (flags & 2) != 0, readNames(in), readNames(in));
    }

    private static void writeBakeSettings(TileBakeSettings settings, DataOutputStream out) throws IOException {
        out.writeInt(settings.maxLayers());
        out.writeInt(settings.minAirGap());
        out.writeInt(settings.minY());
        out.writeInt(settings.maxY());
        out.writeByte((settings.skipCeiling() ? 1 : 0) | (settings.hasCeiling() ? 2 : 0));
        writeNames(settings.skipBlocks(), out);
        writeNames(settings.ceilingBlocks(), out);
    }

    private static List<String> readNames(DataInputStream in) throws IOException {
        int count = in.readUnsignedShort();
        if (count > 1024) {
            throw new IOException("Invalid material list length " + count);
        }
        List<String> names = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            names.add(in.readUTF());
        }
        return names;
    }

    private static void writeNames(List<String> names, DataOutputStream out) throws IOException {
        out.writeShort(names.size());
        for (var name : names) {
            out.writeUTF(name);
        }
    }
}
