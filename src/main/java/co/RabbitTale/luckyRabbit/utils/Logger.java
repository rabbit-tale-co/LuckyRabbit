package co.RabbitTale.luckyRabbit.utils;

import org.bukkit.Bukkit;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public class Logger {
    private static LuckyRabbit plugin;
    private static boolean debugEnabled = false;

    private static final TextColor BLUE = TextColor.color(55, 89, 255);
    private static final TextColor WHITE = TextColor.color(255, 255, 255);
    private static final TextColor GREEN = TextColor.color(108, 255, 93);
    private static final TextColor YELLOW = TextColor.color(255, 220, 34);
    private static final TextColor RED = TextColor.color(255, 53, 61);
    private static final TextColor DARK_GRAY = TextColor.color(100, 100, 100);
    private static final TextColor INFO_COLOR = TextColor.color(42, 119, 255);

    private static final Component PREFIX = Component.text("Lucky Rabbit").color(BLUE);
    private static final Component SEPARATOR = Component.text(" | ").color(NamedTextColor.GRAY);

    public static void init(LuckyRabbit instance) {
        plugin = instance;
        debugEnabled = instance.getConfig().getBoolean("settings.debug", false);
        if (debugEnabled) {
            debug("Logger initialized with debug mode enabled");
            debug("Debug setting from config: " + instance.getConfig().getBoolean("settings.debug", false));
        }
    }

    private static String getCallerInfo() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (int i = 3; i < stackTrace.length; i++) {
            StackTraceElement element = stackTrace[i];
            if (!element.getClassName().equals(Logger.class.getName())) {
                String fileName = element.getFileName();
                int lineNumber = element.getLineNumber();
                return String.format("%s:%d", fileName, lineNumber);
            }
        }
        return "Unknown";
    }

    private static Component getCallerComponent() {
        if (debugEnabled) {
            return Component.text("[" + getCallerInfo() + "] ").color(DARK_GRAY);
        }
        return Component.empty();
    }

    public static void success(String message) {
        Bukkit.getConsoleSender().sendMessage(PREFIX.append(SEPARATOR)
            .append(Component.text("SUCCESS: ").color(GREEN))
            .append(getCallerComponent())
            .append(Component.text(message).color(WHITE)));
    }

    public static void info(String message) {
        Bukkit.getConsoleSender().sendMessage(PREFIX.append(SEPARATOR)
            .append(Component.text("INFO: ").color(INFO_COLOR))
            .append(getCallerComponent())
            .append(Component.text(message).color(WHITE)));
    }

    public static void warning(String message) {
        Bukkit.getConsoleSender().sendMessage(PREFIX.append(SEPARATOR)
            .append(Component.text("WARNING: ").color(YELLOW))
            .append(getCallerComponent())
            .append(Component.text(message).color(WHITE)));
    }

    public static void error(String message) {
        Bukkit.getConsoleSender().sendMessage(PREFIX.append(SEPARATOR)
            .append(Component.text("ERROR: ").color(RED))
            .append(getCallerComponent())
            .append(Component.text(message).color(WHITE)));
    }

    public static void error(String message, Throwable e) {
        Bukkit.getConsoleSender().sendMessage(PREFIX.append(SEPARATOR)
            .append(Component.text("ERROR: ").color(RED))
            .append(getCallerComponent())
            .append(Component.text(message).color(WHITE)));

        // Log the exception details
        Bukkit.getConsoleSender().sendMessage(Component.text("Exception: " + e.getClass().getName()).color(RED));
        Bukkit.getConsoleSender().sendMessage(Component.text("Message: " + e.getMessage()).color(RED));

        // Log the stack trace
        for (StackTraceElement element : e.getStackTrace()) {
            Bukkit.getConsoleSender().sendMessage(Component.text("    at " + element.toString()).color(RED));
        }
    }

    public static void debug(String message) {
        if (plugin != null) {
            debugEnabled = plugin.getConfig().getBoolean("settings.debug", false);
        }

        if (debugEnabled) {
            Bukkit.getConsoleSender().sendMessage(PREFIX.append(SEPARATOR)
                .append(Component.text("DEBUG: ").color(DARK_GRAY))
                .append(getCallerComponent())
                .append(Component.text(message).color(WHITE)));
        }
    }
}
