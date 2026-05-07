package su.nezushin.nminimap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import su.nezushin.nminimap.bstats.Metrics;
import su.nezushin.nminimap.command.MinimapCommand;
import su.nezushin.nminimap.compatibility.ModCompatibilityManager;
import su.nezushin.nminimap.database.DatabaseManager;
import su.nezushin.nminimap.listeners.BlockListener;
import su.nezushin.nminimap.listeners.ChunkListener;
import su.nezushin.nminimap.listeners.PlayerListener;
import su.nezushin.nminimap.listeners.MarkerListener;
import su.nezushin.nminimap.packets.PacketManager;
import su.nezushin.nminimap.papi.NMinimapPAPIExpansion;
import su.nezushin.nminimap.player.NMapPlayer;
import su.nezushin.nminimap.chunks.ChunkManager;
import su.nezushin.nminimap.resourcepack.MarkerImageManager;
import su.nezushin.nminimap.updatechecker.UpdateCheckerManager;
import su.nezushin.nminimap.util.ChunkLoadingUtil;
import su.nezushin.nminimap.util.SchedulerUtil;
import su.nezushin.nminimap.util.config.Config;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class NMinimap extends JavaPlugin {

    private static NMinimap instance;

    private PacketManager packetManager;
    private ChunkManager chunkManager;
    private MarkerImageManager markerImageManager;
    private DatabaseManager databaseManager;
    private ModCompatibilityManager modCompatibilityManager;
    private UpdateCheckerManager updateCheckerManager;

    private NMinimapPAPIExpansion placeholderAPIExpansion;

    private final Set<NMapPlayer> playersWithMap = ConcurrentHashMap.newKeySet();//Collections.synchronizedList(new ArrayList<>());

    @Override
    public void onEnable() {
        instance = this;

        getCommand("minimap").setExecutor(new MinimapCommand());

        int pluginId = 30414;
        Metrics metrics = new Metrics(this, pluginId);

        load();
    }


    public void load() {
        Config.init();
        this.packetManager = new PacketManager();

        //Fix to display both messages about AnvilORM and Packetevents
        boolean shouldExit = false;

        if (!Bukkit.getPluginManager().isPluginEnabled("AnvilORM")) {
            shouldExit = true;
            this.getLogger().severe("AnvilORM plugin is not found. It is mandatory dependency. Please download it from https://github.com/NezuShin/AnvilORM/releases/");
        }

        if (!this.packetManager.isReady()) {
            shouldExit = true;
            this.getLogger().severe("Packetevents plugin is not found. It is mandatory dependency. Please download it from https://www.spigotmc.org/resources/packetevents-api.80279/");
        }

        if (shouldExit) {
            setEnabled(false);
            return;
        }

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            placeholderAPIExpansion = new NMinimapPAPIExpansion();
            sync(() -> {
                placeholderAPIExpansion.register();
            });
        }

        chunkManager = new ChunkManager();
        markerImageManager = new MarkerImageManager();
        databaseManager = new DatabaseManager();
        modCompatibilityManager = new ModCompatibilityManager();
        updateCheckerManager = new UpdateCheckerManager();


        SchedulerUtil.getScheduler().async(() -> {
            playersWithMap.forEach(NMapPlayer::sendMap);
        }, 1, Config.mapRenderInterval);

        Bukkit.getPluginManager().registerEvents(new MarkerListener(), getInstance());
        Bukkit.getPluginManager().registerEvents(new PlayerListener(), getInstance());
        Bukkit.getPluginManager().registerEvents(new ChunkListener(), getInstance());
        new BlockListener().registerListener();

        for (var p : Bukkit.getOnlinePlayers())
            loadPlayer(p);
    }

    public void unload() {
        playersWithMap.forEach(i -> i.onQuit());
        playersWithMap.clear();
        HandlerList.unregisterAll(getInstance());
        SchedulerUtil.getScheduler().cancelAllTasks();
        if (placeholderAPIExpansion != null && isEnabled())
            sync(() -> {
                placeholderAPIExpansion.unregister();
            });
    }

    public void loadPlayer(Player p) {
        NMinimap.async(() -> {
            var player = NMinimap.getInstance().getDatabaseManager()
                    .getPlayersTable()
                    .query()
                    .where("id", p.getUniqueId().toString())
                    .completeAsOne();

            if (player == null) {
                player = new NMapPlayer(p, Config.defaultEnableAnyway || (ChunkLoadingUtil.isPaper() ? Config.defaultEnableBrands.contains(p.getClientBrandName()) : false));

                player.setRight(Config.defaultRightSide);
                player.setRound(Config.defaultRound);
                player.setScale(Config.defaultScale);

                player.saveAsync();
            }
            if (Config.disableModMapAlways)
                getModCompatibilityManager().disableModMinimap(p);

            player.setPlayer(p);
            player.setEnabled(player.isEnabled());

            NMinimap.getInstance().getPlayersWithMap().add(player);
        });
    }

    @Override
    public void onDisable() {
        unload();
    }

    public PacketManager getPacketManager() {
        return packetManager;
    }

    public ChunkManager getChunkManager() {
        return chunkManager;
    }

    public MarkerImageManager getMarkerImageManager() {
        return markerImageManager;
    }

    public Set<NMapPlayer> getPlayersWithMap() {
        return playersWithMap;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ModCompatibilityManager getModCompatibilityManager() {
        return modCompatibilityManager;
    }

    public UpdateCheckerManager getUpdateCheckerManager() {
        return updateCheckerManager;
    }

    public static NMinimap getInstance() {
        return instance;
    }

    public static void sync(Runnable run) {
        SchedulerUtil.getScheduler().sync(run);
    }

    public static void async(Runnable run) {
        var thread = new Thread(run);
        thread.setName("NMinimapThread");
        thread.start();
    }

}
