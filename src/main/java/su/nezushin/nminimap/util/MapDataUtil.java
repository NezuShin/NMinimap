package su.nezushin.nminimap.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MapDataUtil {

    private static byte[] prepareEmptyMapForScale(int scale) {
        var arr = new byte[scale * scale];
        Arrays.fill(arr, (byte) 0);

        return arr;
    }

    private static byte[] loadMapForScale(int scale, InputStream is) throws IOException {
        var arr = new byte[scale * scale];


        int read;
        if (arr.length != (read = is.read(arr, 0, arr.length))) {
            throw new IOException("Failed to read chunk data (" + read + " instead of " + arr.length + ")");
        }

        return arr;
    }

    public static Map<Integer, byte[]> prepareEmptyMap() {
        Map<Integer, byte[]> scales = new HashMap<>();
        scales.put(1, prepareEmptyMapForScale(16));
        scales.put(2, prepareEmptyMapForScale(8));
        scales.put(4, prepareEmptyMapForScale(4));
        scales.put(8, prepareEmptyMapForScale(2));

        return scales;
    }


    public static Map<Integer, byte[]> readMap(InputStream is) throws IOException {
        Map<Integer, byte[]> scales = new HashMap<>();
        scales.put(1, loadMapForScale(16, is));
        scales.put(2, loadMapForScale(8, is));
        scales.put(4, loadMapForScale(4, is));
        scales.put(8, loadMapForScale(2, is));


        return scales;
    }


    public static void saveMap(Map<Integer, byte[]> scales, OutputStream is) throws IOException {
        is.write(scales.get(1));
        is.write(scales.get(2));
        is.write(scales.get(4));
        is.write(scales.get(8));
    }
}
