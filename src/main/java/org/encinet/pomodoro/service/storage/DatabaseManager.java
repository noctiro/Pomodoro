package org.encinet.pomodoro.service.storage;

import org.encinet.pomodoro.Pomodoro;
import org.encinet.pomodoro.config.impl.PresetConfig;

import java.io.File;
import java.sql.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {
    private final Pomodoro plugin;
    private Connection connection;

    public DatabaseManager(Pomodoro plugin) {
        this.plugin = plugin;
        connect();
        initializeDatabase();
    }

    private void connect() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        File dbFile = new File(dataFolder, "playerdata.db");
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not connect to the database.", e);
        }
    }

    private void initializeDatabase() {
        String presetsTable = "CREATE TABLE IF NOT EXISTS presets (" +
                "uuid TEXT NOT NULL," +
                "key TEXT NOT NULL," +
                "name TEXT NOT NULL," +
                "icon TEXT NOT NULL," +
                "enchanted BOOLEAN NOT NULL DEFAULT FALSE," +
                "work INTEGER NOT NULL," +
                "break INTEGER NOT NULL," +
                "long_break INTEGER NOT NULL," +
                "sessions INTEGER NOT NULL," +
                "PRIMARY KEY (uuid, key)" +
                ")";
        String statsTable = "CREATE TABLE IF NOT EXISTS player_stats (" +
                "uuid TEXT PRIMARY KEY," +
                "total_focus_seconds INTEGER NOT NULL DEFAULT 0," +
                "total_work_sessions INTEGER NOT NULL DEFAULT 0" +
                ")";

        try (Statement statement = connection.createStatement()) {
            statement.execute(presetsTable);
            statement.execute(statsTable);

            if (!columnExists("presets", "enchanted")) {
                statement.execute("ALTER TABLE presets ADD COLUMN enchanted BOOLEAN NOT NULL DEFAULT FALSE");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not initialize database tables.", e);
        }
    }

    private boolean columnExists(String tableName, String columnName) throws SQLException {
        DatabaseMetaData md = connection.getMetaData();
        try (ResultSet rs = md.getColumns(null, null, tableName, columnName)) {
            return rs.next();
        }
    }

    public Map<String, PresetConfig.Preset> getPlayerPresets(UUID uuid) {
        Map<String, PresetConfig.Preset> presets = new HashMap<>();
        String sql = "SELECT key, name, icon, enchanted, work, break, long_break, sessions FROM presets WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                presets.put(rs.getString("key"), new PresetConfig.Preset(
                        rs.getString("name"),
                        rs.getString("icon"),
                        rs.getBoolean("enchanted"),
                        rs.getInt("work"),
                        rs.getInt("break"),
                        rs.getInt("long_break"),
                        rs.getInt("sessions")
                ));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not get player presets for " + uuid, e);
        }
        return presets;
    }
    public void savePlayerPresets(UUID uuid, Map<String, PresetConfig.Preset> presets) {
        String sql = "INSERT INTO presets (uuid, key, name, icon, enchanted, work, break, long_break, sessions) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(uuid, key) DO UPDATE SET " +
                "name = excluded.name, " +
                "icon = excluded.icon, " +
                "enchanted = excluded.enchanted, " +
                "work = excluded.work, " +
                "break = excluded.break, " +
                "long_break = excluded.long_break, " +
                "sessions = excluded.sessions";

        try {
            connection.setAutoCommit(false);
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                for (Map.Entry<String, PresetConfig.Preset> entry : presets.entrySet()) {
                    String key = entry.getKey();
                    String name = entry.getValue().name();


                    ps.setString(1, uuid.toString());
                    ps.setString(2, key);
                    ps.setString(3, name);
                    ps.setString(4, entry.getValue().icon());
                    ps.setBoolean(5, entry.getValue().enchanted());
                    ps.setInt(6, entry.getValue().work());
                    ps.setInt(7, entry.getValue().breakTime());
                    ps.setInt(8, entry.getValue().longBreak());
                    ps.setInt(9, entry.getValue().sessions());
                    ps.addBatch();
                }
                ps.executeBatch();
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                plugin.getLogger().log(Level.SEVERE, "Could not save player presets for " + uuid, e);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not manage transaction for saving presets.", e);
        }
    }

    public void updatePresetField(UUID uuid, String key, String field, Object value) {
        List<String> allowedFields = Arrays.asList("name", "icon", "enchanted", "work", "break", "long_break", "sessions");
        if (!allowedFields.contains(field)) {
            plugin.getLogger().warning("Attempted to update an invalid or restricted preset field: " + field);
            return;
        }

        String sql = "UPDATE presets SET " + field + " = ? WHERE uuid = ? AND key = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setObject(1, value);
            ps.setString(2, uuid.toString());
            ps.setString(3, key);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not update preset field for " + uuid + " with key " + key, e);
        }
    }

    public void deletePlayerPreset(UUID uuid, String key) {
        String sql = "DELETE FROM presets WHERE uuid = ? AND key = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, key);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not delete player preset for " + uuid + " with key " + key, e);
        }
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not close database connection.", e);
        }
    }

    public PlayerStats getPlayerStats(UUID uuid) {
        String sql = "SELECT total_focus_seconds, total_work_sessions FROM player_stats WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new PlayerStats(
                        rs.getInt("total_focus_seconds"),
                        rs.getInt("total_work_sessions")
                );
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not get player stats for " + uuid, e);
        }
        return new PlayerStats(0, 0);
    }

    public void addWorkSession(UUID uuid, int focusSeconds) {
        String sql = "INSERT INTO player_stats (uuid, total_focus_seconds, total_work_sessions) VALUES (?, ?, 1) " +
                "ON CONFLICT(uuid) DO UPDATE SET " +
                "total_focus_seconds = total_focus_seconds + excluded.total_focus_seconds, " +
                "total_work_sessions = total_work_sessions + 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, focusSeconds);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not add work session for " + uuid, e);
        }
    }

    public static class PlayerStats {
        private final int totalFocusSeconds;
        private final int totalWorkSessions;

        public PlayerStats(int totalFocusSeconds, int totalWorkSessions) {
            this.totalFocusSeconds = totalFocusSeconds;
            this.totalWorkSessions = totalWorkSessions;
        }

        public int getTotalFocusSeconds() {
            return totalFocusSeconds;
        }

        public int getTotalWorkSessions() {
            return totalWorkSessions;
        }
    }
}