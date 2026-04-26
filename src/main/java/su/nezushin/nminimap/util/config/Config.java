package su.nezushin.nminimap.util.config;

import com.tchristofferson.configupdater.ConfigUpdater;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import su.nezushin.nminimap.NMinimap;
import su.nezushin.nminimap.util.ChunkLoadingUtil;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Config {

    public static FileConfiguration config;

    public static int mapId, maxRenderThreads = 30, maxTilesInRam = 100, maxScale = 8, mysqlPort, defaultScale, mapRenderInterval;

    public static boolean allowFileCache = true, useMysql = false, mysqlUseSSL = false, resourcepackCopyDefaults = true,
            scaleUsePermission, defaultEnableAnyway, defaultRightSide, defaultRound, renderNewChunks, disableModMapActivated,
            disableModMapAlways, enableModVoxelMap, enableModXaerosMap, enableModJourneyMap, skipCeiling, allowModRadar,
            packEnable1_21_11, packEnable26_1, packMcMetaChangeEnabled;

    public static List<String> resourcepackCopyDestinations = new ArrayList<>(), resourcepackZipDestinations = new ArrayList<>(), defaultEnableBrands = new ArrayList<>();

    public static String playerMarker, anotherPlayerMarker, mysqlHost, mysqlUser, mysqlPassword, mysqlDatabase, mysqlPlayersTableName, langName,
            packDescription;

    public static File cacheFolder;

    public static void init() {


        var plugin = NMinimap.getInstance();
        var configFile = new File(plugin.getDataFolder() + File.separator + "config.yml");
        if (!configFile.exists()) {
            plugin.getConfig().options().copyDefaults(true);
            plugin.saveDefaultConfig();
            if (!ChunkLoadingUtil.isPaper()) {
                config = YamlConfiguration.loadConfiguration(configFile);
                config.set("max-render-threads", 1);
                config.set("scale.max-scale", 2);
                try {
                    config.save(configFile);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        } else {
            try {
                ConfigUpdater.update(NMinimap.getInstance(), "config.yml", configFile);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        maxRenderThreads = config.getInt("max-render-threads", 30);

        allowFileCache = config.getBoolean("cache.allow-file-cache", true);
        maxTilesInRam = config.getInt("cache.max-tiles-in-ram", 9999);
        renderNewChunks = config.getBoolean("cache.render-new-chunks", false);

        mapId = config.getInt("map-id", 0);

        mapRenderInterval = config.getInt("player-render-interval", 1);

        skipCeiling = config.getBoolean("skip-ceiling", true);

        useMysql = config.getBoolean("database.mysql.use", false);

        mysqlUseSSL = config.getBoolean("database.mysql.ssl", false);
        mysqlHost = config.getString("database.mysql.host");
        mysqlPort = config.getInt("database.mysql.port");
        mysqlUser = config.getString("database.mysql.username");
        mysqlPassword = config.getString("database.mysql.password");
        mysqlDatabase = config.getString("database.mysql.database");
        mysqlPlayersTableName = config.getString("database.mysql.table-names.players", "nminimap_players");

        playerMarker = config.getString("markers.player-marker", "");
        anotherPlayerMarker = config.getString("markers.another-players-marker", "");

        resourcepackCopyDestinations = config.getStringList("resourcepack.copy-destinations");
        resourcepackZipDestinations = config.getStringList("resourcepack.zip-destinations");
        resourcepackCopyDefaults = config.getBoolean("resourcepack.copy-defaults", true);

        scaleUsePermission = config.getBoolean("scale.use-permission", false);
        maxScale = config.getInt("scale.max-scale", 8);

        langName = config.getString("language", "en_US");

        defaultEnableBrands = config.getStringList("default-settings.enable-if-brand-is");
        defaultEnableAnyway = config.getBoolean("default-settings.enable-anyway", false);
        defaultScale = config.getInt("default-settings.scale", 1);
        defaultRightSide = config.getString("default-settings.side", "left").equalsIgnoreCase("right");
        defaultRound = config.getString("default-settings.style", "square").equalsIgnoreCase("round");

        var modsCompatibilityMode = config.getInt("mods-compatibility.mode", 2);

        if (modsCompatibilityMode == 1) {
            disableModMapAlways = true;
            disableModMapActivated = false;
        } else if (modsCompatibilityMode == 2) {
            disableModMapAlways = false;
            disableModMapActivated = true;
        } else {
            disableModMapAlways = false;
            disableModMapActivated = false;
        }

        enableModVoxelMap = config.getBoolean("mods-compatibility.enable-voxel-map", true);
        enableModXaerosMap = config.getBoolean("mods-compatibility.enable-xaeros-map", true);
        enableModJourneyMap = config.getBoolean("mods-compatibility.enable-journey-map", true);

        allowModRadar = config.getBoolean("mods-compatibility.allow-radar", false);

        packDescription = config.getString("resourcepack.pack-mcmeta.description", "NMinimap pack");
        packEnable1_21_11 = config.getBoolean("resourcepack.pack-mcmeta.overlays.enable-1-21-11", true);
        packEnable26_1 = config.getBoolean("resourcepack.pack-mcmeta.overlays.enable-26-1", true);
        packMcMetaChangeEnabled = config.getBoolean("resourcepack.pack-mcmeta.enable");

        cacheFolder = new File(plugin.getDataFolder(), "cache");

        cacheFolder.mkdirs();

        Message.load();
    }


    public static void copyDefaults(String resourcePath, File dest) {
        if (dest.exists())
            return;
        dest.getParentFile().mkdirs();
        try (InputStream in = NMinimap.getInstance().getResource(resourcePath.replace('\\', '/')); OutputStream out = new FileOutputStream(dest);) {
            if (in == null) {
                throw new IllegalArgumentException("The embedded resource '" + resourcePath + "' cannot be found in " + resourcePath);
            }
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static List<File> getResourcepackCopyDestinationFiles() {
        return resourcepackCopyDestinations.stream().map(i -> Path.of(i).isAbsolute() ? new File(i) : new File(NMinimap.getInstance().getDataFolder().getParentFile(), i)).toList();
    }

    public static List<File> getResourcepackZipDestinationFiles() {
        return resourcepackZipDestinations.stream().map(i -> Path.of(i).isAbsolute() ? new File(i) : new File(NMinimap.getInstance().getDataFolder().getParentFile(), i)).toList();
    }
}
