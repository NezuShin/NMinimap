package su.nezushin.nminimap.chunks.cache;

import su.nezushin.nminimap.util.MapDataUtil;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** All plugin reads, replacements and deletes share a bounded set of path locks. */
public final class TileFiles {
    private final Object[] locks = new Object[128];

    public TileFiles() {
        for (int i = 0; i < locks.length; i++) locks[i] = new Object();
    }

    private Object lock(Path path) {
        return locks[Math.floorMod(path.toAbsolutePath().normalize().hashCode(), locks.length)];
    }

    public Map<Integer, byte[]> read(Path path) throws IOException {
        synchronized (lock(path)) {
            try (var in = new GZIPInputStream(Files.newInputStream(path))) {
                return MapDataUtil.readMap(in);
            }
        }
    }

    public void delete(Path path) throws IOException {
        synchronized (lock(path)) {
            Files.deleteIfExists(path);
        }
    }

    public void write(Path path, Map<Integer, byte[]> scales) throws IOException {
        synchronized (lock(path)) {
            Files.createDirectories(path.toAbsolutePath().getParent());
            Path temporary = Files.createTempFile(path.toAbsolutePath().getParent(), path.getFileName() + ".tmp.", "");
            try {
                try (var out = new GZIPOutputStream(Files.newOutputStream(temporary))) {
                    MapDataUtil.saveMap(scales, out);
                }
                replace(temporary, path);
            } finally {
                Files.deleteIfExists(temporary);
            }
        }
    }

    private static void replace(Path temporary, Path target) throws IOException {
        for (int attempt = 0; ; attempt++) {
            try {
                try {
                    Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException ex) {
                    Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
                }
                return;
            } catch (AccessDeniedException ex) {
                // External readers (e.g. antivirus) can briefly hold a Windows file handle.
                if (attempt == 3) throw ex;
                try {
                    Thread.sleep(25L << attempt);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while replacing cache tile", interrupted);
                }
            }
        }
    }
}
