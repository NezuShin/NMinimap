package su.nezushin.nminimap.resourcepack.packmcmeta;

import su.nezushin.nminimap.util.config.Config;

import java.util.List;

//Helper class for pack.mcmeta serialization
public class PackMcMeta {


    private Pack pack;

    private Overlays overlays;

    public PackMcMeta(Pack pack, Overlays overlays) {
        this.pack = pack;
        this.overlays = overlays;
    }

    public static record Pack(String description, int min_format, int max_format, int[] supported_formats) {

    }

    public static record Overlay(String directory, int min_format, int max_format, int[] formats) {

    }

    public static record Overlays(List<Overlay> entries) {

    }
}
