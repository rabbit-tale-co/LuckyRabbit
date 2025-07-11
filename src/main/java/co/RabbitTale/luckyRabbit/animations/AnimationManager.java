package co.RabbitTale.luckyRabbit.animations;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import co.RabbitTale.luckyRabbit.utils.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class AnimationManager {

    private final LuckyRabbit plugin;
    private final File animationsFolder;
    private final Map<String, AnimationConfig> animations;

    public AnimationManager(LuckyRabbit plugin) {
        this.plugin = plugin;
        this.animationsFolder = new File(plugin.getDataFolder(), "animations");
        this.animations = new HashMap<>();
        loadAnimations();
    }

    private void loadAnimations() {
        // Utworz folder jesli nie istnieje
        if (!animationsFolder.exists() && !animationsFolder.mkdirs()) {
            Logger.error("Failed to create animations directory!");
            return;
        }

        // Skopiuj domyslne pliki animacji z zasobow
        copyDefaultAnimations();

        // Zaladuj wszystkie pliki animacji
        File[] files = animationsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            Logger.error("Failed to list animation files!");
            return;
        }

        // Wyczysc poprzednie animacje
        animations.clear();

        // Zaladuj i zwaliduj kazda animacje
        for (File file : files) {
            AnimationConfig config = AnimationConfig.fromFile(file);
            if (config != null) {
                if (config.isValid()) {
                    animations.put(config.getId(), config);
                    Logger.debug("Loaded animation: " + config.getName() + " (" + config.getId() + ")");
                } else {
                    Logger.warning("Invalid animation license: " + file.getName());
                }
            }
        }

        // Upewnij sie ze mamy przynajmniej domyslna animacje
        if (!animations.containsKey("HORIZONTAL")) {
            Logger.error("Default animation (HORIZONTAL) not found or invalid!");
        }

        Logger.info("Loaded " + animations.size() + " animations");
    }

    private void copyDefaultAnimations() {
        // Kopiujemy tylko podstawowa animacje pozioma oraz README
        copyResourceFile("animations/horizontal.yml");
        copyResourceFile("animations/README.md");
    }

    private void copyResourceFile(String resourcePath) {
        File targetFile = new File(plugin.getDataFolder(), resourcePath);
        if (!targetFile.exists()) {
            try {
                // Utworz folder nadrzedny jesli nie istnieje
                if (!targetFile.getParentFile().exists()) {
                    targetFile.getParentFile().mkdirs();
                }

                // Skopiuj plik z zasobow
                InputStream resource = plugin.getResource(resourcePath);
                if (resource != null) {
                    Files.copy(resource, targetFile.toPath());
                    Logger.debug("Copied resource file: " + resourcePath);
                    resource.close();
                }
            } catch (IOException e) {
                Logger.error("Failed to copy resource file " + resourcePath + ": " + e.getMessage());
            }
        }
    }

    @Nullable
    public AnimationConfig getAnimation(@NotNull String id) {
        return animations.get(id);
    }

    public boolean isAnimationAvailable(@NotNull String id) {
        AnimationConfig config = animations.get(id);
        return config != null && config.isValid();
    }

    public void reload() {
        loadAnimations();
    }

    public Map<String, AnimationConfig> getAvailableAnimations() {
        return new HashMap<>(animations);
    }
}
