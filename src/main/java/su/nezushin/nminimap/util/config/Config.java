package su.nezushin.nminimap.util.config;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import su.nezushin.nminimap.NMinimap;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Config {

    public static FileConfiguration config;

    public static int mapId, maxRenderThreads = 30, maxTilesInRam = 100, maxScale = 8, mysqlPort, defaultScale;

    public static boolean allowFileCache = true, useMysql = false, mysqlUseSSL = false, resourcepackCopyDefaults = true, scaleUsePermission, defaultEnableAnyway, defaultRightSide, defaultRound, renderNewChunks;

    public static List<String> resourcepackCopyDestinations = new ArrayList<>(), resourcepackZipDestinations = new ArrayList<>(), defaultEnableBrands = new ArrayList<>();

    public static String playerMarker, anotherPlayerMarker, mysqlHost, mysqlUser, mysqlPassword, mysqlDatabase, mysqlPlayersTableName, langName;

    public static File cacheFolder;

    public static void init() {


        var plugin = NMinimap.getInstance();
        var configFile  = new File(plugin.getDataFolder() + File.separator + "config.yml");
        if (!configFile.exists()) {
            plugin.getConfig().options().copyDefaults(true);
            plugin.saveDefaultConfig();
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        allowFileCache = config.getBoolean("cache.allow-file-cache", true);
        maxTilesInRam = config.getInt("cache.max-tiles-in-ram", 9999);
        renderNewChunks = config.getBoolean("cache.render-new-chunks", false);

        mapId = config.getInt("map-id", 0);

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
        return resourcepackCopyDestinations.stream().map(i -> Path.of(i).isAbsolute() ? new File(i) : new File(Bukkit.getPluginsFolder(), i)).toList();
    }

    public static List<File> getResourcepackZipDestinationFiles() {
        return resourcepackZipDestinations.stream().map(i -> Path.of(i).isAbsolute() ? new File(i) : new File(Bukkit.getPluginsFolder(), i)).toList();
    }
}
