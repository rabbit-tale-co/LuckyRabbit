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
    private static final long TRIAL_CHECK_INTERVAL = TimeUnit.HOURS.toMillis(1);

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
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            String licenseKey = plugin.getConfig().getString("license-key", "");
            if (!licenseKey.isEmpty()) {
                verifyLicense(licenseKey);
            } else if (System.currentTimeMillis() - lastTrialCheck > TRIAL_CHECK_INTERVAL) {
                checkTrialStatus();
            }
        }, 20L * 60, 20L * 60 * 60); // Check every hour
    }

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
            } catch (Exception e) {
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

    public void verifyLicense(String licenseKey) {
        CompletableFuture.runAsync(() -> {
            try {
                String serverIp = getServerIp();
                if (serverIp == null) {
                    Logger.error("Cannot verify license: Invalid server IP");
                    isPremium = false;
                    checkTrialStatus();
                    return;
                }

                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("license_key", licenseKey);
                requestBody.addProperty("server_ip", serverIp);

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "/license/verify"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                JsonObject jsonResponse = new Gson().fromJson(response.body(), JsonObject.class);

                isPremium = jsonResponse.get("valid").getAsBoolean();

                if (isPremium) {
                    Logger.info("License verified successfully!");
                } else {
                    Logger.warning("Invalid license key. Running in free version.");
                    checkTrialStatus();
                }
            } catch (Exception e) {
                Logger.error("Failed to verify license", e);
                isPremium = false;
                checkTrialStatus();
            }
        });
    }

    public static void checkTrialStatus() {
        lastTrialCheck = System.currentTimeMillis();

        CompletableFuture.runAsync(() -> {
            try {
                String serverIp = getServerIp();
                if (serverIp == null) {
                    Logger.error("Cannot check trial: Invalid server IP");
                    isTrialActive = false;
                    return;
                }

                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("server_ip", serverIp);
                requestBody.addProperty("plugin_name", "LuckyRabbit");

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "/license/trial"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                JsonObject jsonResponse = new Gson().fromJson(response.body(), JsonObject.class);

                isTrialActive = jsonResponse.get("valid").getAsBoolean();

                if (isTrialActive) {
                    Logger.info("Trial period active!");
                } else {
                    Logger.warning("Trial period expired. Some features will be limited.");
                }
            } catch (Exception e) {
                Logger.error("Failed to check trial status", e);
                isTrialActive = true;
                Logger.info("Defaulting to trial mode due to connection error");
            }
        }).join(); // Wait for the check to complete before continuing
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
