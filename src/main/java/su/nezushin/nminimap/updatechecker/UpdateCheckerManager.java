package su.nezushin.nminimap.updatechecker;

import de.clickism.modrinthupdatechecker.ModrinthUpdateChecker;
import org.bukkit.entity.Player;
import su.nezushin.nminimap.NMinimap;
import su.nezushin.nminimap.util.config.Config;
import su.nezushin.nminimap.util.config.Message;


public class UpdateCheckerManager {

    private String newVersion;


    public UpdateCheckerManager() {
        NMinimap.async(() -> {
            probe();
            if (hasNewVersion()) {
                NMinimap.getInstance().getLogger().info("New version found: " + newVersion);
                NMinimap.getInstance().getLogger().info("Download link: https://modrinth.com/plugin/nminimap");
            }
        });
    }

    public boolean hasNewVersion() {
        return newVersion != null;
    }

    public void notifyIfHasNewVersion(Player p) {
        if (!hasNewVersion())
            return;

        Message.new_version_found.replace("{version}", newVersion).send(p);
    }

    private void probe() {
        if (!Config.checkForUpdates)
            return;

        new ModrinthUpdateChecker("nminimap", "spigot", null)
                .checkVersion(version -> {
                    if (versionToNumber(NMinimap.getInstance().getDescription().getVersion()) < versionToNumber(version)) {
                        newVersion = version;
                    }
                });
    }

    private int versionToNumber(String version) {
        int quickfixNumber = 0;
        if (version.contains("quickfix")) {
            quickfixNumber = Integer.parseInt(version.substring(version.indexOf("quickfix")).replaceAll("\\D+", ""));
            version = version.substring(0, version.indexOf("quickfix"));
        }

        return Integer.parseInt(version.replaceAll("\\D+", "")) * 100 + quickfixNumber;
    }


}
