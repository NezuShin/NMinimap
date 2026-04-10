package su.nezushin.nminimap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import su.nezushin.nminimap.bstats.Metrics;
import su.nezushin.nminimap.command.MinimapCommand;
import su.nezushin.nminimap.database.DatabaseManager;
import su.nezushin.nminimap.listeners.BlockListener;
import su.nezushin.nminimap.listeners.ChunkListener;
import su.nezushin.nminimap.listeners.PlayerListener;
import su.nezushin.nminimap.listeners.MarkerListener;
import su.nezushin.nminimap.packets.PacketManager;
import su.nezushin.nminimap.player.NMapPlayer;
import su.nezushin.nminimap.chunks.ChunkManager;
import su.nezushin.nminimap.resourcepack.MarkerImageManager;
import su.nezushin.nminimap.util.config.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class NMinimap extends JavaPlugin {

    private static NMinimap instance;

    private PacketManager packetManager;
    private ChunkManager chunkManager;
    private MarkerImageManager markerImageManager;
    private DatabaseManager databaseManager;

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

        if (Bukkit.getPluginManager().getPlugin("AnvilORM") == null) {
            setEnabled(false);
            this.getLogger().severe("AnvilORM plugin is not found. It is mandatory dependency. Please download it from https://github.com/NezuShin/AnvilORM/releases/");

            return;
        }

        if (!this.packetManager.isReady()) {
            setEnabled(false);
            this.getLogger().severe("Packetevents plugin is not found. It is mandatory dependency. Please download it from https://www.spigotmc.org/resources/packetevents-api.80279/");

            return;
        }
        chunkManager = new ChunkManager();

        markerImageManager = new MarkerImageManager();

        databaseManager = new DatabaseManager();


        Bukkit.getScheduler().runTaskTimerAsynchronously(getInstance(), () -> {
            playersWithMap.forEach(NMapPlayer::sendMap);
        }, 1, 1).getTaskId();

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
        Bukkit.getScheduler().cancelTasks(getInstance());
    }

    public void loadPlayer(Player p) {
        NMinimap.async(() -> {
            var player = NMinimap.getInstance().getDatabaseManager()
                    .getPlayersTable()
                    .query()
                    .where("id", p.getUniqueId().toString())
                    .completeAsOne();

            if (player == null) {
                player = new NMapPlayer(p, Config.defaultEnableAnyway || Config.defaultEnableBrands.contains(p.getClientBrandName()));

                player.setRight(Config.defaultRightSide);
                player.setRound(Config.defaultRound);
                player.setScale(Config.defaultScale);

                player.saveAsync();
            }

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

    public static NMinimap getInstance() {
        return instance;
    }

    public static void sync(Runnable run) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(NMinimap.getInstance(), run);
    }

    public static void async(Runnable run) {

        //Bukkit.getScheduler().runTaskAsynchronously(getInstance(), run);
        var thread = new Thread(run);
        thread.setName("NMinimapThread");
        thread.start();
    }
}
