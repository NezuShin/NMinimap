package su.nezushin.nminimap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import su.nezushin.nminimap.bstats.Metrics;
import su.nezushin.nminimap.command.MinimapCommand;
import su.nezushin.nminimap.compatibility.GeyserManager;
import su.nezushin.nminimap.compatibility.ModCompatibilityManager;
import su.nezushin.nminimap.compatibility.WorldGuardManager;
import su.nezushin.nminimap.database.DatabaseManager;
import su.nezushin.nminimap.listeners.BlockListener;
import su.nezushin.nminimap.listeners.ChunkListener;
import su.nezushin.nminimap.listeners.CommandPermissionListener;
import su.nezushin.nminimap.listeners.PlayerListener;
import su.nezushin.nminimap.listeners.MarkerListener;
import su.nezushin.nminimap.packets.PacketManager;
import su.nezushin.nminimap.papi.NMinimapPAPIExpansion;
import su.nezushin.nminimap.player.NMapPlayer;
import su.nezushin.nminimap.chunks.ChunkManager;
import su.nezushin.nminimap.radar.MobRadarManager;
import su.nezushin.nminimap.resourcepack.MarkerImageManager;
import su.nezushin.nminimap.updatechecker.UpdateCheckerManager;
import su.nezushin.nminimap.util.ChunkLoadingUtil;
import su.nezushin.nminimap.util.SchedulerUtil;
import su.nezushin.nminimap.util.config.Config;
import su.nezushin.nminimap.util.config.UndergroundLayer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class NMinimap extends JavaPlugin {

    private static NMinimap instance;

    private PacketManager packetManager;
    private ChunkManager chunkManager;
    private MarkerImageManager markerImageManager;
    private DatabaseManager databaseManager;
    private ModCompatibilityManager modCompatibilityManager;
    private WorldGuardManager worldGuardManager;
    private GeyserManager geyserManager;
    private UpdateCheckerManager updateCheckerManager;
    private MobRadarManager mobRadarManager;

    private NMinimapPAPIExpansion placeholderAPIExpansion;

    private boolean emergencyExit = false;//Do not call unload method if no dependencies installed

    private final Set<NMapPlayer> playersWithMap = ConcurrentHashMap.newKeySet();

    @Override
    public void onEnable() {
        instance = this;

        getCommand("minimap").setExecutor(new MinimapCommand());

        int pluginId = 30414;
        Metrics metrics = new Metrics(this, pluginId);
        metrics.addCustomChart(new Metrics.AdvancedPie("player_platform", () -> {
            int bedrock = 0, java = 0;
            for (var p : playersWithMap) {
                if (p.isBedrockPlayer()) bedrock++;
                else java++;
            }
            Map<String, Integer> map = new HashMap<>();
            if (java > 0) map.put("Java", java);
            if (bedrock > 0) map.put("Bedrock", bedrock);
            return map;
        }));
        metrics.addCustomChart(new Metrics.SingleLineChart("bedrock_players", () ->
                (int) playersWithMap.stream().filter(NMapPlayer::isBedrockPlayer).count()));
        metrics.addCustomChart(new Metrics.SimplePie("servers_with_geyser", () ->
                Bukkit.getPluginManager().isPluginEnabled("Geyser-Spigot") ? "Yes" : "No"));

        load();
    }


    public void load() {
        Config.init();

        //Fix to display both messages about AnvilORM and Packetevents
        emergencyExit = false;

        var anvilOrm = Bukkit.getPluginManager().getPlugin("AnvilORM");
        if (anvilOrm == null || !anvilOrm.isEnabled()) {
            emergencyExit = true;
            this.getLogger().severe("AnvilORM plugin is not found. It is mandatory dependency. Please download it from https://github.com/NezuShin/AnvilORM/releases/");
        } else if (UpdateCheckerManager.versionToNumber(anvilOrm.getDescription().getVersion()) < UpdateCheckerManager.versionToNumber("1.0.2")) {
            emergencyExit = true;
            this.getLogger().severe("AnvilORM version " + anvilOrm.getDescription().getVersion() + " is too old. NMinimap requires AnvilORM 1.0.2 or newer. Please download it from https://github.com/NezuShin/AnvilORM/releases/");
        }

        if (!emergencyExit) {
            this.packetManager = new PacketManager();

            if (!this.packetManager.isReady()) {
                emergencyExit = true;
                this.getLogger().severe("Packetevents plugin is not found. It is mandatory dependency. Please download it from https://www.spigotmc.org/resources/packetevents-api.80279/");
            }
        }

        if (emergencyExit) {
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
        worldGuardManager = new WorldGuardManager();
        if (worldGuardManager.isEnabled()) {
            getLogger().info("WorldGuard compatibility is enabled, underground layers will be supported");
        }
        geyserManager = new GeyserManager();
        if (geyserManager.isEnabled()) {
            getLogger().info("Geyser-Spigot found, Bedrock player detection is enabled");
        }
        updateCheckerManager = new UpdateCheckerManager();
        mobRadarManager = new MobRadarManager();

        Config.validateLocationMarkers();

        SchedulerUtil.getScheduler().async(() -> {
            playersWithMap.forEach(NMapPlayer::sendMap);
        }, 1, Config.mapRenderInterval);

        SchedulerUtil.getScheduler().async(() -> {
            if (worldGuardManager == null || !worldGuardManager.isEnabled()) return;

            for (NMapPlayer mapPlayer : playersWithMap) {
                var player = mapPlayer.getPlayer();
                if (player == null || !player.isOnline()) continue;
                var loc = player.getLocation();
                var activeLayer = worldGuardManager.getActiveLayer(loc);
                mapPlayer.setActiveLayer(activeLayer);
            }
        }, 10, Config.wgRegionUpdateInterval);


        Bukkit.getPluginManager().registerEvents(new MarkerListener(), getInstance());
        Bukkit.getPluginManager().registerEvents(new PlayerListener(), getInstance());
        Bukkit.getPluginManager().registerEvents(new ChunkListener(), getInstance());
        if (Config.commandPermissionApplyToMinimap)
            Bukkit.getPluginManager().registerEvents(new CommandPermissionListener(), getInstance());
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
                player.setRadarEnabled(Config.defaultEnableMobRadar);

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
        if (!emergencyExit)
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

    public WorldGuardManager getWorldGuardManager() {
        return this.worldGuardManager;
    }

    public GeyserManager getGeyserManager() {
        return geyserManager;
    }

    public UpdateCheckerManager getUpdateCheckerManager() {
        return updateCheckerManager;
    }

    public MobRadarManager getMobRadarManager() {
        return mobRadarManager;
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
