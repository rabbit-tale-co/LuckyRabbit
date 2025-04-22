package co.RabbitTale.luckyRabbit.api;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.file.YamlConfiguration;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import co.RabbitTale.luckyRabbit.utils.Logger;

/**
 * AnimationManager.java
 *
 * Manages animation license files loaded from YML files. Each animation has its
 * own license file in the animations directory. License files are validated
 * against built-in validation keys.
 */
public class AnimationManager {

    private final Map<String, YamlConfiguration> animationConfigs;
    private final Set<String> availableAnimations;
    private final File animationsDirectory;

    // Validation keys for each animation - these should be updated with each plugin version
    private static final Map<String, String> VALIDATION_KEYS = new HashMap<>();
    private static final Map<String, String> CHECKSUMS = new HashMap<>();
    private static final String API_VERSION = "1.0.0";

    // Initialize validation keys - these must match the ones in the YML files
    static {
        // Default animation - always available
        VALIDATION_KEYS.put("HORIZONTAL", "HR-7821-DFLT-9433-BASE");
        CHECKSUMS.put("HORIZONTAL", "a7f92e36d8c14b8fb2ed0a5a6a7f7cb2");

        // Premium animations - require valid license files
        VALIDATION_KEYS.put("CIRCLE", "CR-2947-PRMM-5231-RING");
        CHECKSUMS.put("CIRCLE", "b8c45e89d7a23c9fa5d62b1f9c0e3a54");

        VALIDATION_KEYS.put("THREE_IN_ROW", "3R-6359-PRMM-8742-SLOT");
        CHECKSUMS.put("THREE_IN_ROW", "c5f83d47a9e12b45d8c73e6a4f1b2d9e");

        VALIDATION_KEYS.put("PIN_POINT", "PP-4129-PRMM-3517-SPOT");
        CHECKSUMS.put("PIN_POINT", "d2e71c95b8f34a6ed1c9a3f5e8d2b7c9");

        VALIDATION_KEYS.put("CASCADE", "CS-5896-PRMM-2183-FLOW");
        CHECKSUMS.put("CASCADE", "e1d83b27c6a49f5b3e7d2c8a1f9e3d6c");
    }

    /**
     * Creates a new animation manager.
     *
     * @param plugin Plugin instance
     */
    public AnimationManager(LuckyRabbit plugin) {
        this.animationConfigs = new HashMap<>();
        this.availableAnimations = new HashSet<>();
        this.animationsDirectory = new File(plugin.getDataFolder(), "animations");

        // Load all animation configurations
        loadAnimations();
    }

    /**
     * Loads and validates all animation license files from the animations
     * directory.
     */
    public void loadAnimations() {
        Logger.info("Loading animation license files...");
        animationConfigs.clear();
        availableAnimations.clear();

        // Always add HORIZONTAL as available since it's the default
        availableAnimations.add("HORIZONTAL");

        if (!animationsDirectory.exists()) {
            Logger.warning("Animations directory does not exist!");
            return;
        }

        File[] animationFiles = animationsDirectory.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (animationFiles == null || animationFiles.length == 0) {
            Logger.warning("No animation license files found!");
            return;
        }

        int validCount = 0;
        int invalidCount = 0;
        StringBuilder availableList = new StringBuilder("HORIZONTAL (default)");

        for (File file : animationFiles) {
            try {
                String fileName = file.getName();
                String animationId = fileName.substring(0, fileName.length() - 4).toUpperCase();

                // Skip if it's already processed (like HORIZONTAL)
                if (availableAnimations.contains(animationId)) {
                    continue;
                }

                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

                // Check if animation is enabled in the file
                if (!config.getBoolean("enabled", true)) {
                    Logger.debug("Animation " + animationId + " is disabled in its config file");
                    continue;
                }

                // Validate the license file
                if (validateLicense(animationId, config)) {
                    // Add to available animations
                    availableAnimations.add(animationId);
                    animationConfigs.put(animationId, config);
                    Logger.debug("Loaded and validated animation license: " + animationId);
                    availableList.append(", ").append(animationId);
                    validCount++;
                } else {
                    Logger.warning("Animation license validation failed for: " + animationId);
                    invalidCount++;
                }
            } catch (Exception e) {
                Logger.error("Failed to load animation license file: " + file.getName(), e);
                invalidCount++;
            }
        }

        Logger.info("Loaded " + validCount + " valid animations, rejected " + invalidCount + " invalid licenses");
        Logger.info("Available animations: " + availableList);
    }

    /**
     * Validates an animation license file against built-in validation keys.
     *
     * @param animationId The animation ID to validate
     * @param config The configuration from the license file
     * @return true if validation succeeded, false otherwise
     */
    private boolean validateLicense(String animationId, YamlConfiguration config) {
        // Check if we have validation data for this animation
        if (!VALIDATION_KEYS.containsKey(animationId)) {
            Logger.warning("No validation data found for animation: " + animationId);
            return false;
        }

        // Get validation data from file
        String validationKey = config.getString("validation_key", "");
        String checksum = config.getString("checksum", "");
        String apiVersion = config.getString("api_version", "");

        // Check if validation key matches
        if (!VALIDATION_KEYS.get(animationId).equals(validationKey)) {
            Logger.warning("Invalid validation key for animation: " + animationId);
            return false;
        }

        // Check if checksum matches
        if (!CHECKSUMS.get(animationId).equals(checksum)) {
            Logger.warning("Invalid checksum for animation: " + animationId);
            return false;
        }

        // Check if API version is compatible
        if (!apiVersion.equals(API_VERSION)) {
            Logger.warning("Incompatible API version for animation: " + animationId
                    + " (required: " + API_VERSION + ", found: " + apiVersion + ")");
            return false;
        }

        // All checks passed
        return true;
    }

    /**
     * Checks if an animation type is available.
     *
     * @param animationType Animation type to check
     * @return true if animation is available
     */
    public boolean isAnimationAvailable(String animationType) {
        if (animationType == null || animationType.isEmpty()) {
            return false;
        }

        // Always allow HORIZONTAL as the default animation
        if ("HORIZONTAL".equalsIgnoreCase(animationType)) {
            return true;
        }

        // Check if animation is in our available set
        return availableAnimations.contains(animationType.toUpperCase());
    }

    /**
     * Gets all available animation types.
     *
     * @return Set of available animation IDs
     */
    public Set<String> getAvailableAnimations() {
        return Collections.unmodifiableSet(availableAnimations);
    }

    /**
     * Reloads all animation configurations.
     */
    public void reloadAnimations() {
        loadAnimations();
    }
}
