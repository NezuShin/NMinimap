package su.nezushin.nminimap.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MapDataUtil {

    private static byte[] prepareEmptyMapForScale(int scale) {
        var arr = new byte[scale * scale];
        Arrays.fill(arr, (byte) 0);

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
}
