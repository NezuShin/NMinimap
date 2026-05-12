package su.nezushin.nminimap.papi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nezushin.nminimap.NMinimap;
import su.nezushin.nminimap.player.NMapPlayer;
import su.nezushin.nminimap.util.DiskCapacityUtil;

public class NMinimapPAPIExpansion extends PlaceholderExpansion {
    @Override
    public @NotNull String getIdentifier() {
        return "nminimap";
    }

    @Override
    public @NotNull String getAuthor() {
        return "NezuShin";
    }

    @Override
    public @NotNull String getVersion() {
        return NMinimap.getInstance().getPluginMeta().getVersion();
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {

        if (params.equalsIgnoreCase("stats_loaded_tiles")) {
            return String.valueOf(NMinimap.getInstance().getChunkManager().getLoadedTiles().size());
        } else if (params.equalsIgnoreCase("stats_cache_size")) {
            return String.valueOf(NMinimap.getInstance().getChunkManager().getChunkCache().getCachedFiles().size());
        } else if (params.equalsIgnoreCase("stats_enabled_maps")) {
            return String.valueOf(NMinimap.getInstance().getPlayersWithMap().stream().filter(NMapPlayer::isEnabled).count());
        } else if (params.equalsIgnoreCase("stats_threads")) {
            return String.valueOf(Thread.getAllStackTraces().keySet()
                    .stream().filter(i -> i.getName().equalsIgnoreCase("NMinimapThread")).count());
        } else if (params.equalsIgnoreCase("stats_loading_chunks")) {
            return String.valueOf(NMinimap.getInstance().getChunkManager().getLoadingChunks().size());
        } else if (params.equalsIgnoreCase("stats_render_queue")) {
            return String.valueOf(NMinimap.getInstance().getChunkManager().getAwaitingChunksSize());
        } else if (params.equalsIgnoreCase("stats_disk_total_space_g")) {
            return String.format("%.1f", (double) DiskCapacityUtil.getTotalSpace() / (1024L * 1024L * 1024L));
        } else if (params.equalsIgnoreCase("stats_disk_free_space_g")) {
            return String.format("%.1f", (double) DiskCapacityUtil.getUsableSpace() / (1024L * 1024L * 1024L));
        } else if (params.equalsIgnoreCase("stats_disk_total_space")) {
            return String.valueOf(DiskCapacityUtil.getTotalSpace());
        } else if (params.equalsIgnoreCase("stats_disk_free_space")) {
            return String.valueOf(DiskCapacityUtil.getUsableSpace());
        }

        var nminimapPlayer = NMinimap.getInstance().getPlayersWithMap().stream()
                .filter(i -> i.getPlayer().equals(player))
                .findFirst().orElse(null);

        if (nminimapPlayer == null)
            return null;

        if (params.equalsIgnoreCase("enabled")) {
            return String.valueOf(nminimapPlayer.isEnabled());
        } else if (params.equalsIgnoreCase("scale")) {
            return String.valueOf(nminimapPlayer.getScale());
        } else if (params.equalsIgnoreCase("side")) {
            return nminimapPlayer.isRight() ? "right" : "left";
        } else if (params.equalsIgnoreCase("style")) {
            return nminimapPlayer.isRound() ? "round" : "square";
        }

        return null;
    }
}