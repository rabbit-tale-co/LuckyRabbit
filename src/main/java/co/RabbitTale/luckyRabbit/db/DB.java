package co.RabbitTale.luckyRabbit.db;

import co.RabbitTale.luckyRabbit.animations.AnimationRecord;
import co.RabbitTale.luckyRabbit.utils.Logger;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.*;

/**
 * Database manager for Supabase connections using HikariCP Handles animation
 * validation and plugin version checking
 */
public final class DB {

    private static HikariDataSource dataSource;
    private static boolean isConnected = false;

    private DB() {
        // Utility class
    }

    /**
     * Initialize the database connection pool
     *
     * @param plugin The plugin instance
     * @return true if connection was successfully established
     */
    public static boolean init(JavaPlugin plugin) {
        try {
            File configFile = new File(plugin.getDataFolder(), "config.yml");
            Logger.debug("Loading DB configuration from " + configFile.getAbsolutePath());
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

            String url = config.getString("database.url");
            Logger.debug("Database JDBC URL: " + (url != null ? url : "<not configured>"));

            if (url == null) {
                Logger.warning("Database URL not configured - using offline mode");
                return false;
            }

            // Wymuszone ladowanie sterownika PostgreSQL – ulatwia diagnostyke
            try {
                Class.forName("org.postgresql.Driver");
                Logger.debug("PostgreSQL JDBC driver loaded");
            } catch (ClassNotFoundException cnfe) {
                Logger.error("PostgreSQL JDBC driver not found in classpath – add dependency to plugin JAR");
                return false;
            }

            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setDriverClassName("org.postgresql.Driver");
            Logger.debug("Configuring HikariCP pool...");
            hikariConfig.setJdbcUrl(url);

            // Optional separate username/password (if not in URL)
            String username = config.getString("database.user");
            String password = config.getString("database.password");
            if (username != null && password != null) {
                hikariConfig.setUsername(username);
                hikariConfig.setPassword(password);
            }
            hikariConfig.setMaximumPoolSize(config.getInt("database.pool.maxSize", 5));
            hikariConfig.setIdleTimeout(config.getLong("database.pool.idleTimeout", 30_000));

            Logger.debug("HikariCP pool size: " + hikariConfig.getMaximumPoolSize());

            // Additional connection settings for reliability
            hikariConfig.setConnectionTimeout(30_000);
            hikariConfig.setValidationTimeout(5_000);
            hikariConfig.setLeakDetectionThreshold(60_000);

            Logger.debug("Creating HikariCP data source...");
            dataSource = new HikariDataSource(hikariConfig);

            // Test the connection
            Logger.debug("Testing database connection...");
            try (Connection connection = dataSource.getConnection()) {
                Logger.debug("Database connection test successful: " + connection.getMetaData().getURL());
                Logger.success("Database connection established successfully");
                isConnected = true;
                return true;
            }
        } catch (Exception e) {
            Logger.error("Failed to initialize database connection: " + e.getMessage());
            Logger.warning("Plugin will run in offline mode with default animations only");
            isConnected = false;
            return false;
        }
    }

    /**
     * Fetch animation data from the database
     *
     * @param id The animation ID to fetch
     * @return AnimationRecord if found, null otherwise
     */
    @Nullable
    public static AnimationRecord fetchAnimation(String id) {
        if (!isConnected || dataSource == null) {
            Logger.debug("Database not connected - skipping animation fetch for " + id);
            return null;
        }

        Logger.debug("Executing animation fetch query for ID: " + id);
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "SELECT validation_key, min_api_version FROM animations WHERE id = ?")) {

            statement.setString(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    Logger.debug("No record found for animation " + id);
                    return null;
                }
                Logger.debug("Record found for animation " + id + ", returning validation key");
                return new AnimationRecord(
                        resultSet.getString("validation_key"),
                        resultSet.getString("min_api_version")
                );
            }
        } catch (SQLException e) {
            Logger.error("Failed to fetch animation " + id + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Get the latest plugin version from the database
     *
     * @return The latest version string, or "unknown" if unavailable
     */
    public static String getLatestPluginVersion() {
        if (!isConnected || dataSource == null) {
            Logger.debug("Database not connected - cannot check latest version");
            return "unknown";
        }

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery("SELECT latest_version FROM plugininfo LIMIT 1")) {

            if (resultSet.next()) {
                return resultSet.getString("latest_version");
            }
            return "unknown";
        } catch (SQLException e) {
            Logger.error("Failed to fetch latest plugin version: " + e.getMessage());
            return "unknown";
        }
    }

    /**
     * Check if the database connection is active
     *
     * @return true if connected to database
     */
    public static boolean isConnected() {
        return isConnected && dataSource != null && !dataSource.isClosed();
    }

    /**
     * Close the database connection pool
     */
    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            isConnected = false;
            Logger.info("Database connection closed");
        }
    }
}
