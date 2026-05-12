package su.nezushin.nminimap.util;

import su.nezushin.nminimap.util.config.Config;

public class DiskCapacityUtil {


    public static long getTotalSpace() {
        return Config.cacheFolder.getTotalSpace();
    }

    public static long getUsableSpace() {
        return Config.cacheFolder.getUsableSpace();
    }
}
