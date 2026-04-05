package su.nezushin.nminimap.util.config;

import com.google.common.collect.Lists;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import su.nezushin.nminimap.NMinimap;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public enum Message {

    map_enabled, map_disabled, scale_set, style_set, side_set, side_left, side_right, style_round, style_square, help, incorrect_scale, insufficient_permissions, reload_complete, reload_failed, reload_start, admin_stats, you_cannot_use_this_scale;


    private List<String> message;


    public static void load() {
        var file = getLangFile(Config.langName);

        if (file == null) {
            NMinimap.getInstance().getLogger().severe("Language file for lang " + Config.langName + " is not found. Using en_US instead");
            file = getLangFile("en_US");
        }

        loadFromFile(file);
    }

    private static File getLangFile(String lang) {
        var file = new File(NMinimap.getInstance().getDataFolder(), "lang/" + lang + ".yml");
        if (!file.exists()) {
            try {
                Config.copyDefaults("defaults/lang/" + lang + ".yml", file);
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }

        return file;
    }

    private static void loadFromFile(File file) {
        var config = YamlConfiguration.loadConfiguration(file);
        for (var msg : Message.values())
            msg.load(config);
    }

    public void load(FileConfiguration messages) {
        var path = this.name();
        if (messages.isList(path)) {
            message = messages.getStringList(path);
        } else if (messages.isString(path)) {
            message = Lists.newArrayList(messages.getString(path));
        } else {
            message = Lists.newArrayList();
        }
    }

    public void send(CommandSender p) {
        new Sender(this.message).send(p);
    }

    public Sender replace(String... strings) {
        return new Sender(this.message).replace(strings);
    }

    public String asString() {
        return String.join("\n", message);
    }

    public static class Sender {

        private List<String> message;

        public Sender(List<String> message) {
            this.message = new ArrayList<>(message);
        }

        public Sender replace(String... strings) {
            var flag = false;
            var replace = "";
            for (var str : strings) {
                if (!flag) {
                    replace = str;
                } else {
                    for (var i = 0; i < this.message.size(); i++)
                        this.message.set(i, this.message.get(i).replace(replace, str));
                }
                flag = !flag;
            }
            return this;
        }

        public Sender send(CommandSender p) {
            for (var msg : message)
                p.sendMessage(MiniMessage.miniMessage().deserialize(msg));
            return this;
        }

    }
}
