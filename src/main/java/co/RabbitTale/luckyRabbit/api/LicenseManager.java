package co.RabbitTale.luckyRabbit.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import co.RabbitTale.luckyRabbit.utils.Logger;

public class LicenseManager {

    private static LuckyRabbit plugin;
    private static String apiUrl = null;
    private static HttpClient httpClient = null;
    private static boolean isPremium = false;
    private static boolean isTrialActive = false;
    private static long lastTrialCheck = 0;
    private static final long LICENSE_CHECK_TICKS = 20 * 60 * 15; // 15 minutes in ticks
    private static final long TRIAL_CHECK_INTERVAL = TimeUnit.MINUTES.toMillis(1);

    public LicenseManager(LuckyRabbit plugin) {
        LicenseManager.plugin = plugin;
        apiUrl = "https://api.rabbittale.co/api";
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        // Start periodic checks
        startPeriodicChecks();
    }

    private void startPeriodicChecks() {
        Logger.debug("Starting periodic license checks (interval: 1 minute)");

        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            String licenseKey = plugin.getConfig().getString("license-key", "");

            // Debug log for periodic check
            Logger.debug("=== Periodic License Check ===");
            Logger.debug("Time: " + new java.util.Date());
            Logger.debug("License Key: " + (licenseKey.isEmpty() ? "Not set" : "Present"));
            Logger.debug("Current Status: " + (isPremium ? "PREMIUM" : isTrialActive ? "TRIAL" : "FREE"));

            if (!licenseKey.isEmpty()) {
                Logger.debug("Verifying license key...");
                verifyLicense(licenseKey);
            } else if (System.currentTimeMillis() - lastTrialCheck > TRIAL_CHECK_INTERVAL) {
                Logger.debug("Checking trial status...");
                checkTrialStatus();
            }
            Logger.debug("===========================");

        }, LICENSE_CHECK_TICKS, LICENSE_CHECK_TICKS); // Run every minute (in ticks)
    }

    private static String getServerIp() {
        String ip = plugin.getServer().getIp();
        int port = plugin.getServer().getPort();

        // Log the values for debugging
        Logger.debug("Server IP from config: " + ip);
        Logger.debug("Server port from config: " + port);

        return ip + ":" + port;
    }

    public void verifyLicense(String licenseKey) {
        CompletableFuture.runAsync(() -> {
            try {
                Logger.debug("Starting license verification...");
                String serverIp = getServerIp();
                if (serverIp == null) {
                    Logger.error("Cannot verify license: Invalid server IP");
                    isPremium = false;
                    checkTrialStatus();
                    return;
                }
                Logger.debug("Server IP: " + serverIp);

                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("license_key", licenseKey);
                requestBody.addProperty("server_ip", serverIp);

                Logger.debug("Sending license verification request...");
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl + "/license/verify"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                Logger.debug("License API Response: " + response.body());

                JsonObject jsonResponse = new Gson().fromJson(response.body(), JsonObject.class);

                boolean wasPremium = isPremium;
                isPremium = jsonResponse.get("valid").getAsBoolean();
                String message = jsonResponse.get("message").getAsString();

                Logger.debug("License status: " + (isPremium ? "PREMIUM" : "FREE"));
                Logger.debug("API Message: " + message);

                if (isPremium) {
                    Logger.success("License verified successfully: " + message);
                    isTrialActive = false;
                    if (!wasPremium) {
                        Logger.debug("Updating plan type due to premium activation");
                        FeatureManager.updatePlanType();
                    }
                } else {
                    Logger.warning("License verification failed: " + message);
                    if (wasPremium) {
                        Logger.debug("Updating plan type due to premium deactivation");
                        FeatureManager.updatePlanType();
                    }
                    Logger.debug("Checking trial status after failed verification");
                    checkTrialStatus();
                }

            } catch (Exception e) {
                Logger.error("Failed to verify license", e);
                Logger.debug("Exception details: " + e.getMessage());
                isPremium = false;
                checkTrialStatus();
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

            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("server_ip", serverIp);
            requestBody.addProperty("plugin_name", "LuckyRabbit");

            Logger.debug("Sending trial check request...");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "/license/trial"))
                    .header("Content-Type", "application/json")
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

                if (!wasTrialActive) {
                    Logger.debug("Updating plan type due to trial activation");
                    FeatureManager.updatePlanType();
                }
            } else {
                Logger.warning("Trial status: " + message);
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

    public boolean hasFullAccess() {
        return isPremium || isTrialActive;
    }
}
