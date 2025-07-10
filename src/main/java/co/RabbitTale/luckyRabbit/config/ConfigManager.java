package co.RabbitTale.luckyRabbit.config;

import java.io.File;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import co.RabbitTale.luckyRabbit.utils.Logger;

public class ConfigManager {

    private final LuckyRabbit plugin;

    public ConfigManager(LuckyRabbit plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        // Create plugin folder if it doesn't exist
        if (!plugin.getDataFolder().exists()) {
            if (!plugin.getDataFolder().mkdirs()) {
                Logger.error("Failed to create plugin directory!");
                return;
            }
        }

        // Create lootboxes directory structure
        createLootboxDirectories();

        // Create animations directory
        createAnimationsDirectory();

        // Load main config
        plugin.reloadConfig();

        // Load lootbox config
        new LootboxConfig(plugin);

        Logger.debug("All configurations loaded successfully");
    }

    /**
     * Creates the directory structure for lootboxes and example lootboxes. Main
     * directory: plugins/LuckyRabbit/lootboxes/ Examples directory:
     * plugins/LuckyRabbit/lootboxes/examples/
     */
    private void createLootboxDirectories() {
        // Create main lootboxes directory
        File lootboxDir = new File(plugin.getDataFolder(), "lootboxes");
        if (!lootboxDir.exists()) {
            if (lootboxDir.mkdirs()) {
                Logger.debug("Created lootboxes directory");
            } else {
                Logger.error("Failed to create lootboxes directory!");
            }
        }

        // Create examples subdirectory
        File examplesDir = new File(lootboxDir, "examples");
        if (!examplesDir.exists()) {
            if (examplesDir.mkdirs()) {
                Logger.debug("Created examples directory");
            } else {
                Logger.error("Failed to create examples directory!");
                return;
            }
        }

        // Always check for missing example lootboxes and create them
        createExampleLootboxes(examplesDir);
    }

    /**
     * Creates the animations directory and saves default animation files.
     * Directory: plugins/LuckyRabbit/animations/
     */
    private void createAnimationsDirectory() {
        // Create animations directory
        File animationsDir = new File(plugin.getDataFolder(), "animations");
        if (!animationsDir.exists()) {
            if (animationsDir.mkdirs()) {
                Logger.debug("Created animations directory");

                // Save default horizontal animation file if it doesn't exist
                File horizontalFile = new File(animationsDir, "horizontal.yml");
                if (!horizontalFile.exists()) {
                    try {
                        plugin.saveResource("animations/horizontal.yml", false);
                        Logger.debug("Created default horizontal.yml animation file");
                    } catch (Exception e) {
                        Logger.error("Failed to create default horizontal.yml animation file", e);
                    }
                }

                // We don't auto-create other animation files (circle.yml, three_in_row.yml, etc.)
                // They can be downloaded or purchased separately
                Logger.info("Only default animation (HORIZONTAL) is available by default");
                Logger.info("Additional animations can be added manually to the animations folder");
            } else {
                Logger.error("Failed to create animations directory!");
            }
        }
    }

    /**
     * Creates example lootboxes in the examples directory by copying them from
     * resources.
     *
     * @param examplesDir The examples directory to create files in
     */
    private void createExampleLootboxes(File examplesDir) {
        try {
            // Get all .yml files from lootboxes resources
            java.util.List<String> lootboxFiles = findLootboxResources();

            if (lootboxFiles.isEmpty()) {
                Logger.warning("No example lootbox files found in resources");
                return;
            }

            int createdCount = 0;
            for (String resourcePath : lootboxFiles) {
                try {
                    // Extract just the filename without path for target file
                    String targetFileName = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
                    File targetFile = new File(examplesDir, targetFileName);

                    if (!targetFile.exists()) {
                        // Copy resource directly to target file using InputStream
                        java.io.InputStream inputStream = plugin.getResource(resourcePath);
                        if (inputStream != null) {
                            java.nio.file.Files.copy(inputStream, targetFile.toPath());
                            inputStream.close();
                            Logger.debug("Created example lootbox: " + targetFileName);
                            createdCount++;
                        } else {
                            Logger.warning("Could not find resource: " + resourcePath);
                        }
                    } else {
                        Logger.debug("Example lootbox already exists: " + targetFileName);
                    }
                } catch (Exception e) {
                    Logger.error("Failed to create example lootbox: " + resourcePath, e);
                }
            }

            Logger.info("Created " + createdCount + " example lootboxes in the examples directory");
        } catch (Exception e) {
            Logger.error("Failed to scan for example lootboxes", e);
        }
    }

    /**
     * Finds all .yml files in the lootboxes resources directory recursively.
     *
     * @return List of resource paths to lootbox files
     */
    private java.util.List<String> findLootboxResources() {
        java.util.List<String> files = new java.util.ArrayList<>();

        try {
            // Get the JAR file or class directory
            java.net.URL resourceUrl = plugin.getClass().getResource("/lootboxes");
            if (resourceUrl != null) {
                java.net.URI uri = resourceUrl.toURI();

                if (uri.getScheme().equals("jar")) {
                    // Running from JAR - scan the JAR file
                    try (java.nio.file.FileSystem fileSystem = java.nio.file.FileSystems.newFileSystem(uri, java.util.Collections.emptyMap())) {
                        java.nio.file.Path lootboxPath = fileSystem.getPath("/lootboxes");
                        scanDirectory(lootboxPath, "lootboxes", files);
                    }
                } else {
                    // Running from IDE/filesystem - scan directory
                    java.nio.file.Path lootboxPath = java.nio.file.Paths.get(uri);
                    scanDirectory(lootboxPath, "lootboxes", files);
                }
            }
        } catch (Exception e) {
            Logger.debug("Could not scan lootboxes resources: " + e.getMessage());
            // Fallback to known files if scanning fails
            files.add("lootboxes/horizontal_normal.yml");
            files.add("lootboxes/horizontal_custom.yml");
            files.add("lootboxes/premium/example3.yml");
            files.add("lootboxes/premium/three_in_row.yml");
        }

        return files;
    }

    /**
     * Recursively scans a directory for .yml files.
     *
     * @param path Directory path to scan
     * @param basePath Base path for constructing resource paths
     * @param files List to add found files to
     */
    private void scanDirectory(java.nio.file.Path path, String basePath, java.util.List<String> files) {
        try {
            if (java.nio.file.Files.exists(path)) {
                java.nio.file.Files.walk(path)
                        .filter(java.nio.file.Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".yml"))
                        .forEach(p -> {
                            // Convert path to resource path
                            String relativePath = path.relativize(p).toString().replace('\\', '/');
                            files.add(basePath + "/" + relativePath);
                        });
            }
        } catch (Exception e) {
            Logger.debug("Could not scan directory " + path + ": " + e.getMessage());
        }
    }

}
