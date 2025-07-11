package co.RabbitTale.luckyRabbit.animations;

import co.RabbitTale.luckyRabbit.utils.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import co.RabbitTale.luckyRabbit.LuckyRabbit;
import co.RabbitTale.luckyRabbit.db.DB;

public class AnimationConfig {

    private final String name;
    private final String id;
    private final String description;
    private final String validationKey;

    private AnimationConfig(String name, String id, String description, String validationKey) {
        this.name = name;
        this.id = id;
        this.description = description;
        this.validationKey = validationKey;
    }

    @Nullable
    public static AnimationConfig fromFile(@NotNull File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        try {
            String name = config.getString("name");
            String id = config.getString("id");
            String description = config.getString("description");
            String validationKey = config.getString("validation_key");

            if (id == null || validationKey == null) {
                Logger.error("Invalid animation config in " + file.getName() + ": Missing required fields");
                return null;
            }

            return new AnimationConfig(name, id, description, validationKey);
        } catch (Exception e) {
            Logger.error("Failed to load animation config from " + file.getName() + ": " + e.getMessage());
            return null;
        }
    }

    public boolean isValid() {

        if ("HORIZONTAL".equals(id)) {
            return true;
        }

        // Fetch animation data from database
        AnimationRecord dbRecord = DB.fetchAnimation(id);
        if (dbRecord == null) {
            Logger.debug("Cannot validate animation " + id + " - database unavailable or record missing");
            return false;
        }

        return validateWithDatabaseData(dbRecord);
    }

    /**
     * Validate animation using data from the database
     */
    private boolean validateWithDatabaseData(AnimationRecord dbRecord) {
        // Compare validation key from database with local file
        boolean valid = validationKey != null && validationKey.equals(dbRecord.validationKey());
        if (!valid) {
            Logger.debug("Validation key mismatch for animation " + id + " (DB vs local)");
        }
        return valid;
    }

    // API version compatibility check removed – no longer necessary
    // Gettery
    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }
}
