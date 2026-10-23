package su.nezushin.nminimap.util;

import java.util.concurrent.atomic.AtomicInteger;

public class SpigotEntityIdUtil {

    // Our entities exist only in packets, so we hand out our own ids.
    // The server counts entity ids up from 0, so negative ids never clash with real entities.
    // Starts well below -1, because some packets use -1 to mean "no entity".
    private static final AtomicInteger NEXT_ID = new AtomicInteger(-1_000_000);

    public static int nextEntityId() {
        return NEXT_ID.getAndDecrement();
    }

}
