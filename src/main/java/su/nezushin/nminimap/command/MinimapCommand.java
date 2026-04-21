package su.nezushin.nminimap.command;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nezushin.nminimap.NMinimap;
import su.nezushin.nminimap.player.NMapPlayer;
import su.nezushin.nminimap.util.config.Config;
import su.nezushin.nminimap.util.config.Message;
import su.nezushin.nminimap.util.config.Permission;

import java.util.List;
import java.util.logging.Level;

public class MinimapCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {


        NMinimap.async(() -> {
            if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
                if (args[1].equalsIgnoreCase("reload")) {
                    if (!Permission.admin.has(sender)) {
                        Message.insufficient_permissions.send(sender);
                        return;
                    }
                    Message.reload_start.send(sender);
                    try {
                        var nminimap = NMinimap.getInstance();

                        nminimap.unload();
                        nminimap.load();
                        Message.reload_complete.send(sender);
                    } catch (Exception ex) {
                        Message.reload_failed.send(sender);
                        NMinimap.getInstance().getLogger().log(Level.SEVERE, "Failed to reload plugin: ", ex);
                    }

                } else if (args[1].equalsIgnoreCase("stats")) {
                    Message.admin_stats.replace("{loaded_tiles}", String.valueOf(NMinimap.getInstance().getChunkManager().getLoadedTiles().size()),
                            "{cache_size}", String.valueOf(NMinimap.getInstance().getChunkManager().getChunkCache().getCachedFiles().size()),
                            "{enabled_maps}", String.valueOf(NMinimap.getInstance().getPlayersWithMap().stream().filter(NMapPlayer::isEnabled).count()),
                            "{render_queue}", String.valueOf(NMinimap.getInstance().getChunkManager().getAwaitingChunksSize()),
                            "{loading_chunks}", String.valueOf(NMinimap.getInstance().getChunkManager().getLoadingChunks().size()),
                            "{threads}", String.valueOf(Thread.getAllStackTraces().keySet().stream().filter(i -> i.getName().equalsIgnoreCase("NMinimapThread")
                            ).count())
                    ).send(sender);
                    return;
                }
            }

            if (!(sender instanceof Player p))
                return;


            var player = NMinimap.getInstance().getPlayersWithMap().stream().filter(i -> i.getPlayer().equals(p)).findFirst().orElse(null);

            if (player == null)//!?
                return;
            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("enable")) {
                    player.setEnabled(true);
                    Message.map_enabled.send(p);
                    return;
                } else if (args[0].equalsIgnoreCase("disable")) {
                    player.setEnabled(false);
                    Message.map_disabled.send(p);
                    return;
                }
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("scale")) {

                    var scale = 0;
                    if (!Sets.newHashSet("1", "2", "4", "8").contains(args[1]) || (scale = Integer.parseInt(args[1])) > Config.maxScale) {
                        Message.incorrect_scale.send(p);
                        return;
                    }

                    if (Config.scaleUsePermission && !p.hasPermission("nminimap.scale." + scale)) {
                        Message.you_cannot_use_this_scale.send(p);
                        return;
                    }

                    player.setScale(scale);
                    Message.scale_set.replace("{scale}", args[1]).send(p);
                    return;
                } else if (args[0].equalsIgnoreCase("style")) {
                    player.setRound(args[1].equalsIgnoreCase("round"));
                    Message.style_set.replace("{style}", player.isRound() ? Message.style_round.asString() : Message.style_square.asString()).send(p);
                    return;
                } else if (args[0].equalsIgnoreCase("side")) {
                    player.setRight(args[1].equalsIgnoreCase("right"));
                    Message.side_set.replace("{side}", player.isRight() ? Message.side_right.asString() : Message.side_left.asString()).send(p);
                    return;
                }
            }
            Message.help.send(p);
        });


        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 1) {
            return Lists.newArrayList("scale", "style", "side", "enable", "disable", "admin")
                    .stream().filter(i -> StringUtil.startsWithIgnoreCase(i, args[0])).toList();
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("scale"))
                return Lists.newArrayList("1", "2", "4", "8")
                        .stream().filter(i -> StringUtil.startsWithIgnoreCase(i, args[1])).toList();
            else if (args[0].equalsIgnoreCase("style"))
                return Lists.newArrayList("square", "round")
                        .stream().filter(i -> StringUtil.startsWithIgnoreCase(i, args[1])).toList();
            else if (args[0].equalsIgnoreCase("side"))
                return Lists.newArrayList("left", "right")
                        .stream().filter(i -> StringUtil.startsWithIgnoreCase(i, args[1])).toList();
            else if (args[0].equalsIgnoreCase("admin"))
                return Lists.newArrayList("reload", "stats")
                        .stream().filter(i -> StringUtil.startsWithIgnoreCase(i, args[1])).toList();
        }
        return List.of();
    }
}
