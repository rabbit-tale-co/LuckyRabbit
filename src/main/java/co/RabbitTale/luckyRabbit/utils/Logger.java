package co.RabbitTale.luckyRabbit.utils;

import org.bukkit.Bukkit;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

/*
 * Logger.java
 *
 * Custom logging utility for the LuckyRabbit plugin.
 * Provides formatted console output with colors, prefixes, and debug information.
 *
 * Features:
 * - Colored output using Adventure API
 * - Debug mode toggle via config
 * - File and line number tracking in debug mode
 * - Different log levels (DEBUG, INFO, WARNING, ERROR, SUCCESS)
 * - Stack trace formatting for errors
 *
 * Color Scheme:
 * - PREFIX (Lucky Rabbit): Blue (#375AFF)
 * - DEBUG: Gray (#969696)
 * - INFO: Light Blue (#2A77FF)
 * - WARNING: Yellow (#FFDC22)
 * - ERROR: Red (#FF353D)
 * - SUCCESS: Green (#6CFF5D)
 *
 * Usage:
 * Logger.debug("Debug message");  // Only shown if debug mode is enabled
 * Logger.info("Info message");    // General information
 * Logger.warning("Warning message");  // Warnings
 * Logger.error("Error message");  // Errors
 * Logger.error("Error message", exception);  // Errors with stack trace
 * Logger.success("Success message");  // Success messages
 *
 */
public class Logger {

    // Color constants with RGB values for easy modification
    private static final TextColor BLUE = TextColor.color(55, 89, 255);      // Plugin name color
    private static final TextColor WHITE = TextColor.color(255, 255, 255);   // Message text color
    private static final TextColor GREEN = TextColor.color(108, 255, 93);    // Success messages
    private static final TextColor YELLOW = TextColor.color(255, 220, 34);   // Warning messages
    private static final TextColor RED = TextColor.color(255, 53, 61);       // Error messages
    private static final TextColor DARK_GRAY = TextColor.color(100, 100, 100); // Separator color
    private static final TextColor INFO_COLOR = TextColor.color(42, 119, 255); // Info messages
    private static final TextColor DEBUG_COLOR = TextColor.color(150, 150, 150); // Debug messages

    // Static components used in all messages
    private static final Component PREFIX = Component.text("Lucky Rabbit").color(BLUE);
    private static final Component SEPARATOR = Component.text(" | ").color(DARK_GRAY);

    private static boolean debugEnabled = false;

    /**
     * Initializes the logger with plugin instance. Reads debug setting from
     * config.
     *
     * @param instance The LuckyRabbit plugin instance
     */
    public static void init(LuckyRabbit instance) {
        debugEnabled = instance.getConfig().getBoolean("settings.debug", false);
        if (debugEnabled) {
            debug("Logger initialized with debug mode enabled");
            debug("Debug setting from config: " + instance.getConfig().getBoolean("settings.debug", false));
        }
    }

    /**
     * Gets caller information for debug messages. Returns file name and line
     * number of the calling method.
     *
     * @return String in format "FileName.java:lineNumber"
     */
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

    /**
     * Creates a component with caller information if debug is enabled.
     *
     * @return Component with caller info or empty component if debug is
     * disabled
     */
    private static Component getCallerComponent() {
        if (debugEnabled) {
            return Component.text("[" + getCallerInfo() + "] ").color(DARK_GRAY);
        }
        return Component.empty();
    }

    /**
     * Logs a debug message. Only shown if debug mode is enabled. Includes file
     * and line number information.
     *
     * @param message The debug message to log
     */
    public static void debug(String message) {
        if (!debugEnabled) {
            return;
        }

        Bukkit.getConsoleSender().sendMessage(
                PREFIX
                        .append(SEPARATOR)
                        .append(Component.text("DEBUG: ").color(DEBUG_COLOR))
                        .append(getCallerComponent())
                        .append(Component.text(message).color(WHITE))
        );
    }

    /**
     * Logs an informational message.
     *
     * @param message The info message to log
     */
    public static void info(String message) {
        Bukkit.getConsoleSender().sendMessage(
                PREFIX
                        .append(SEPARATOR)
                        .append(Component.text("INFO: ").color(INFO_COLOR))
                        .append(getCallerComponent())
                        .append(Component.text(message).color(WHITE))
        );
    }

    /**
     * Logs a warning message.
     *
     * @param message The warning message to log
     */
    public static void warning(String message) {
        Bukkit.getConsoleSender().sendMessage(
                PREFIX
                        .append(SEPARATOR)
                        .append(Component.text("WARNING: ").color(YELLOW))
                        .append(getCallerComponent())
                        .append(Component.text(message).color(WHITE))
        );
    }

    /**
     * Logs an error message.
     *
     * @param message The error message to log
     */
    public static void error(String message) {
        Bukkit.getConsoleSender().sendMessage(
                PREFIX
                        .append(SEPARATOR)
                        .append(Component.text("ERROR: ").color(RED))
                        .append(getCallerComponent())
                        .append(Component.text(message).color(WHITE))
        );
    }

    /**
     * Logs an error message with exception details. Includes stack trace
     * formatting.
     *
     * @param message The error message to log
     * @param e The exception that caused the error
     */
    public static void error(String message, Throwable e) {
        Bukkit.getConsoleSender().sendMessage(
                PREFIX
                        .append(SEPARATOR)
                        .append(Component.text("ERROR: ").color(RED))
                        .append(getCallerComponent())
                        .append(Component.text(message + " - " + e.getMessage()).color(WHITE))
        );

        // Log stack trace
        for (StackTraceElement element : e.getStackTrace()) {
            Bukkit.getConsoleSender().sendMessage(
                    Component.text("    at " + element.toString()).color(RED)
            );
        }
    }

    /**
     * Logs a success message.
     *
     * @param message The success message to log
     */
    public static void success(String message) {
        Bukkit.getConsoleSender().sendMessage(
                PREFIX
                        .append(SEPARATOR)
                        .append(Component.text("SUCCESS: ").color(GREEN))
                        .append(getCallerComponent())
                        .append(Component.text(message).color(WHITE))
        );
    }
}
