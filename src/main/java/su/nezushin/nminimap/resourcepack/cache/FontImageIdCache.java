package su.nezushin.nminimap.resourcepack.cache;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import su.nezushin.nminimap.NMinimap;
import su.nezushin.nminimap.resourcepack.font.BitmapFontImage;
import su.nezushin.nminimap.resourcepack.font.FontImageProviders;
import su.nezushin.nminimap.util.FontUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Used to store and assign new ids (symbols) for blocks. Assigns Private Use Area symbols (starts from \uE000)
 */
public class FontImageIdCache {

    private Map<String, Integer> charIds = new HashMap<>();
    private transient Map<String, BitmapFontImage> registeredCharIds = new HashMap<>();

    private static final Gson gson = new Gson();

    private int nextCharId = FontUtil.unicodeEscapeSequenceToInt("\\uE000");

    public FontImageIdCache() {
    }

    public int getOrCreateFontImageId(String name) {
        var id = charIds.get(name);

        if (id == null) {
            id = nextCharId++;
            charIds.put(name, id);
        }

        return id;
    }


    public void build(File fontsDir) throws IOException {
        var array = new ArrayList<BitmapFontImage>(registeredCharIds.values());

        var data = gson.toJson(new FontImageProviders(array));

        for (var file : new File[]{
                new File(fontsDir, "default.json")
        })
            Files.writeString(file.toPath(), data, Charsets.UTF_8);
    }

    public void save() throws IOException {
        var cacheFile = new File(NMinimap.getInstance().getDataFolder(), "markers-font-cache.json");

        Files.writeString(cacheFile.toPath(), gson.toJson(this), Charsets.UTF_8);
    }

    public static FontImageIdCache load() throws IOException {
        var cacheFile = new File(NMinimap.getInstance().getDataFolder(), "markers-font-cache.json");

        if (!cacheFile.exists())
            return new FontImageIdCache();

        return gson
                .fromJson(Files.readString(cacheFile.toPath(), StandardCharsets.UTF_8), FontImageIdCache.class);
    }

    public Map<String, BitmapFontImage> getRegisteredCharIds() {
        return registeredCharIds;
    }

    public void cleanRegistered() {
        this.registeredCharIds = new HashMap<>();
    }
}
