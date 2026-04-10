package su.nezushin.nminimap.database;

import su.nezushin.anvil.orm.AnvilORMFactory;
import su.nezushin.anvil.orm.table.AnvilORMTable;
import su.nezushin.nminimap.NMinimap;
import su.nezushin.nminimap.player.NMapPlayer;
import su.nezushin.nminimap.util.config.Config;

import java.io.File;
import java.io.IOException;

public class DatabaseManager {

    private AnvilORMTable<NMapPlayer> playersTable;

    public DatabaseManager() {

        if (Config.useMysql)
            playersTable = AnvilORMFactory.factory().buildMysqlTable(NMapPlayer.class, Config.mysqlPlayersTableName, Config.mysqlHost, Config.mysqlPort, Config.mysqlDatabase, Config.mysqlUser, Config.mysqlPassword, Config.mysqlUseSSL);
        else {
            var file = new File(NMinimap.getInstance().getDataFolder(), "database.db");
            if(!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            playersTable = AnvilORMFactory.factory().buildSqliteTable(NMapPlayer.class, "nminimap_players", file);
        }
    }

    public AnvilORMTable<NMapPlayer> getPlayersTable() {
        return playersTable;
    }
}
