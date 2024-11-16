package co.RabbitTale.luckyRabbit.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import co.RabbitTale.luckyRabbit.commands.LootboxCommand;
import co.RabbitTale.luckyRabbit.utils.Logger;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

/**
 * LicenseManager.java
 *
 * This class handles all license-related functionality for the LuckyRabbit
 * plugin. It manages license verification, trial status checks, and server IP
 * validation.
 *
 * Key features: - License verification with API server - Trial mode management
 * - Server IP validation and public IP detection - Secure API communication
 * with bearer token authentication - Status notifications for admins
 *
 * The license system supports three modes: - PREMIUM: Full access to all
 * features - TRIAL: Limited time access to premium features - FREE: Basic
 * functionality only
 *
 */
public class LicenseManager {

    private static LuckyRabbit plugin;
    private static String apiUrl = null;
    private static HttpClient httpClient = null;
    private static boolean isPremium = false;
    private static boolean isTrialActive = false;
    private static long lastTrialCheck = 0;
    private static final long TRIAL_CHECK_INTERVAL = TimeUnit.MINUTES.toMillis(1);
    private static String accessKey = null;
    @Getter
    private static boolean periodicChecksEnabled = true;
    @Getter
    private static int taskId = -1;
    private static boolean isVerifying = false;
    private static String previousStatus = "FREE";

    private static final TextColor DESCRIPTION_COLOR = TextColor.color(180, 180, 180);

    public LicenseManager(LuckyRabbit plugin) {
        LicenseManager.plugin = plugin;

        // Read values from .env file inside JAR
        Properties envProps = new Properties();
        try {
            // Load .env directly from JAR resources
            InputStream envStream = plugin.getClass().getResourceAsStream("/.env");
            if (envStream != null) {
                envProps.load(envStream);
                apiUrl = envProps.getProperty("API_URL", "https://api.rabbittale.co/api");
                accessKey = envProps.getProperty("ACCESS_KEY");
                if (accessKey == null || accessKey.isEmpty()) {
                    Logger.error("Missing ACCESS_KEY in .env file");
                }
            } else {
                Logger.error("Could not find .env file in JAR");
            }
        } catch (IOException e) {
            Logger.error("Failed to load .env file from JAR", e);
        }

        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        // Check if license key is set in config
        String licenseKey = plugin.getConfig().getString("license-key", "");
        if (!licenseKey.isEmpty()) {
            verifyLicense(licenseKey, false);
        } else {
            checkTrialStatus();
        }
    }

    /**
     * Gets the server's public IP address. Prevents usage of localhost or
     * invalid IPs.
     *
     * @return The server's IP address and port, or null if invalid
     */
    private static String getServerIp() {
        String ip = plugin.getServer().getIp();
        int port = plugin.getServer().getPort();

        // Block localhost and invalid IPs
        if (ip.isEmpty() || ip.equals("0.0.0.0") || ip.equals("127.0.0.1") || ip.equals("localhost")) {
            try {
                // Try to get the server's public IP
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.ipify.org"))
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    String publicIp = response.body().trim();
                    // Verify it's not a localhost IP
                    if (!publicIp.startsWith("127.") && !publicIp.equals("0.0.0.0")) {
                        ip = publicIp;
                    } else {
                        Logger.error("Invalid public IP detected: " + publicIp);
                        return null;
                    }
                } else {
                    Logger.error("Failed to get public IP, status code: " + response.statusCode());
                    return null;
                }
            } catch (IOException | InterruptedException e) {
                Logger.error("Failed to get server IP", e);
                return null;
            }
        }

        // Additional validation
        if (ip.startsWith("127.") || ip.equals("localhost")) {
            Logger.error("Invalid server IP detected: " + ip);
            return null;
        }

        return ip + ":" + port;
    }

    /**
     * Verifies a license key with the API server.
     *
     * @param licenseKey The license key to verify
     * @param isFromCommand Whether this verification was triggered by a command
     */
    public void verifyLicense(String licenseKey, boolean isFromCommand) {
        if (isVerifying) {
            return;
        }
        isVerifying = true;

        CompletableFuture.runAsync(() -> {
            try {
                Logger.debug("Starting license verification...");
                String serverIp = getServerIp();
                if (serverIp == null) {
                    Logger.error("Cannot verify license: Invalid server IP");
                    isPremium = false;
                    periodicChecksEnabled = true;
                    checkTrialStatus();
                    return;
                }
                Logger.debug("Server IP: " + serverIp);

                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("license_key", licenseKey);
                requestBody.addProperty("server_ip", serverIp);

                if (accessKey == null || accessKey.isEmpty()) {
                    Logger.error("Missing access key from .env file");
                    isPremium = false;
                    periodicChecksEnabled = true;
                    checkTrialStatus();
                    return;
                }

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl + "/v2/license/verify"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + accessKey)
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                JsonObject jsonResponse = parseJsonResponse(response.body());
                if (jsonResponse == null) {
                    Logger.warning("License verification failed: Invalid JSON response");
                    isPremium = false;
                    periodicChecksEnabled = true;
                    checkTrialStatus();
                    return;
                }

                isPremium = jsonResponse.get("valid").getAsBoolean();
                String message = jsonResponse.get("message").getAsString();

                Logger.debug("License status: " + (isPremium ? "PREMIUM" : "FREE"));
                Logger.debug("API Message: " + message);

                String currentStatus = isPremium ? "PREMIUM" : "FREE";

                if (isFromCommand) {
                    if (isPremium) {
                        plugin.getServer().getOnlinePlayers().forEach(player -> {
                            if (player.hasPermission("luckyrabbit.admin")) {
                                player.sendMessage(Component.empty());
                                player.sendMessage(Component.text()
                                        .append(Component.text("License Status Update", LootboxCommand.SUCCESS_COLOR))
                                        .build());
                                player.sendMessage(Component.text()
                                        .append(Component.text("Status: ", DESCRIPTION_COLOR))
                                        .append(Component.text("PREMIUM", LootboxCommand.SUCCESS_COLOR))
                                        .build());
                                player.sendMessage(Component.text()
                                        .append(Component.text("Message: ", DESCRIPTION_COLOR))
                                        .append(Component.text(message, LootboxCommand.SUCCESS_COLOR))
                                        .build());
                                player.sendMessage(Component.empty());
                            }
                        });
                    } else {
                        plugin.getServer().getOnlinePlayers().forEach(player -> {
                            if (player.hasPermission("luckyrabbit.admin")) {
                                player.sendMessage(Component.empty());
                                player.sendMessage(Component.text()
                                        .append(Component.text("License Status Update", LootboxCommand.ERROR_COLOR))
                                        .build());
                                player.sendMessage(Component.text()
                                        .append(Component.text("Status: ", DESCRIPTION_COLOR))
                                        .append(Component.text("FREE", LootboxCommand.ERROR_COLOR))
                                        .build());
                                player.sendMessage(Component.text()
                                        .append(Component.text("Reason: ", DESCRIPTION_COLOR))
                                        .append(Component.text(message, LootboxCommand.ERROR_COLOR))
                                        .build());
                                player.sendMessage(Component.empty());
                            }
                        });
                    }
                } else {
                    if (!currentStatus.equals(previousStatus)) {
                        plugin.getServer().getOnlinePlayers().forEach(player -> {
                            if (player.hasPermission("luckyrabbit.admin")) {
                                player.sendMessage(Component.text()
                                        .append(Component.text("License mode changed: ", DESCRIPTION_COLOR))
                                        .append(Component.text(previousStatus, LootboxCommand.ERROR_COLOR))
                                        .append(Component.text(" -> ", DESCRIPTION_COLOR))
                                        .append(Component.text(currentStatus, LootboxCommand.SUCCESS_COLOR))
                                        .build());
                            }
                        });
                    }
                }

                previousStatus = currentStatus;

                isTrialActive = false;
                periodicChecksEnabled = !isPremium;
                FeatureManager.updatePlanType();

            } catch (Exception e) {
                Logger.error("Failed to verify license", e);
                isPremium = false;
                periodicChecksEnabled = true;
                checkTrialStatus();

                // Notify about error
                plugin.getServer().getOnlinePlayers().forEach(player -> {
                    if (player.hasPermission("luckyrabbit.admin")) {
                        player.sendMessage(Component.empty());
                        player.sendMessage(Component.text()
                                .append(Component.text("License Verification Error", LootboxCommand.ERROR_COLOR))
                                .build());
                        player.sendMessage(Component.text()
                                .append(Component.text("Error: ", DESCRIPTION_COLOR))
                                .append(Component.text(e.getMessage(), LootboxCommand.ERROR_COLOR))
                                .build());
                        player.sendMessage(Component.empty());
                    }
                });
            } finally {
                isVerifying = false;
            }
        });
    }

    public static void checkTrialStatus() {
        Logger.debug("Starting trial status check...");

        // Don't check trial if premium is active
        if (isPremium) {
            Logger.debug("Premium active, skipping trial check");
            isTrialActive = false;
            return;
        }

        lastTrialCheck = System.currentTimeMillis();

        try {
            String serverIp = getServerIp();
            Logger.debug("Checking trial for server: " + serverIp);

            if (serverIp == null) {
                Logger.error("Cannot check trial: Invalid server IP");
                isTrialActive = false;
                FeatureManager.updatePlanType();
                return;
            }

            if (accessKey == null || accessKey.isEmpty()) {
                Logger.error("Missing access key from .env file");
                isTrialActive = false;
                FeatureManager.updatePlanType();
                return;
            }

            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("server_ip", serverIp);
            requestBody.addProperty("plugin_name", "LuckyRabbit");

            Logger.debug("Sending trial check request...");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "/v2/license/trial"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + accessKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            Logger.debug("Trial API Response: " + response.body());

            JsonObject jsonResponse = new Gson().fromJson(response.body(), JsonObject.class);

            boolean wasTrialActive = isTrialActive;
            boolean valid = jsonResponse.has("valid") && jsonResponse.get("valid").getAsBoolean();
            String message = jsonResponse.has("message") ? jsonResponse.get("message").getAsString() : "No message from server";

            Logger.debug("Trial valid: " + valid);
            Logger.debug("Trial message: " + message);

            isTrialActive = valid;

            if (isTrialActive) {
                String expiresAt = jsonResponse.has("expires_at") ? jsonResponse.get("expires_at").getAsString() : null;
                Logger.debug("Trial expiration: " + (expiresAt != null ? expiresAt : "Not specified"));
                Logger.success("Trial status: " + message + (expiresAt != null ? " (Expires: " + expiresAt + ")" : ""));
                periodicChecksEnabled = true;

                if (!wasTrialActive) {
                    Logger.debug("Updating plan type due to trial activation");
                    FeatureManager.updatePlanType();
                }
            } else {
                Logger.debug("Trial status: " + message);
                periodicChecksEnabled = false;
                if (wasTrialActive) {
                    Logger.debug("Updating plan type due to trial deactivation");
                    FeatureManager.updatePlanType();
                }
            }

        } catch (Exception e) {
            Logger.error("Failed to check trial status", e);
            Logger.debug("Exception details: " + e.getMessage());
            isTrialActive = false;
            Logger.warning("Running in FREE mode due to connection error");
            FeatureManager.updatePlanType();
            periodicChecksEnabled = true;
        }
    }

    public static boolean isPremium() {
        return isPremium;
    }

    public static boolean isTrialActive() {
        if (System.currentTimeMillis() - lastTrialCheck > TRIAL_CHECK_INTERVAL) {
            checkTrialStatus();
        }
        return isTrialActive;
    }

    private JsonObject parseJsonResponse(String responseBody) {
        try {
            return new Gson().fromJson(responseBody, JsonObject.class);
        } catch (Exception e) {
            Logger.error("Failed to parse JSON response", e);
            return null;
        }
    }

    public static boolean isVerifying() {
        return isVerifying;
    }
}
