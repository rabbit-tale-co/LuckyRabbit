package co.RabbitTale.luckyRabbit.api;

import java.net.http.HttpClient;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import co.RabbitTale.luckyRabbit.commands.LootboxCommand;
import lombok.Getter;
import net.kyori.adventure.text.Component;

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
    private static boolean isPremium = true;
    private static boolean isTrialActive = false;
    //private static long lastTrialCheck = 0;
    //private static final long TRIAL_CHECK_INTERVAL = TimeUnit.MINUTES.toMillis(10); // 10 minutes
    //private static String accessKey = null;
    @Getter
    private static boolean periodicChecksEnabled = false;
    @Getter
    private static int taskId = -1;
    private static boolean isVerifying = false;
    //private static String previousStatus = "PREMIUM";

    //private static final TextColor DESCRIPTION_COLOR = TextColor.color(180, 180, 180);
    public LicenseManager(LuckyRabbit plugin) {
        LicenseManager.plugin = plugin;

        // Disable license verification and simply grant premium
        isPremium = true;
        isTrialActive = false;
        periodicChecksEnabled = false;

        // Optionally, skip loading .env values and license key verification:
        // Commented out the original license verification code:
        /*
        Properties envProps = new Properties();
        try {
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

        String licenseKey = plugin.getConfig().getString("license-key", "");
        if (!licenseKey.isEmpty()) {
            verifyLicense(licenseKey, false);
        } else {
            checkTrialStatus();
        }
         */
        // Inform admins that license verification is deactivated
        plugin.getServer().getOnlinePlayers().forEach(player -> {
            if (player.hasPermission("luckyrabbit.admin")) {
                player.sendMessage(Component.text("License verification deactivated. All features enabled.",
                        LootboxCommand.SUCCESS_COLOR));
            }
        });
    }

    /**
     * Gets the server's public IP address. Prevents usage of localhost or
     * invalid IPs.
     *
     * @return The server's IP address and port, or null if invalid
     */
    // private static String getServerIp() {
    //     String ip = plugin.getServer().getIp();
    //     int port = plugin.getServer().getPort();
    //     // Block localhost and invalid IPs
    //     if (ip.isEmpty() || ip.equals("0.0.0.0") || ip.equals("127.0.0.1") || ip.equals("localhost")) {
    //         try {
    //             // Try to get the server's public IP
    //             HttpRequest request = HttpRequest.newBuilder()
    //                     .uri(URI.create("https://api.ipify.org"))
    //                     .timeout(Duration.ofSeconds(5))
    //                     .GET()
    //                     .build();
    //             HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    //             if (response.statusCode() == 200) {
    //                 String publicIp = response.body().trim();
    //                 // Verify it's not a localhost IP
    //                 if (!publicIp.startsWith("127.") && !publicIp.equals("0.0.0.0")) {
    //                     ip = publicIp;
    //                 } else {
    //                     Logger.error("Invalid public IP detected: " + publicIp);
    //                     return null;
    //                 }
    //             } else {
    //                 Logger.error("Failed to get public IP, status code: " + response.statusCode());
    //                 return null;
    //             }
    //         } catch (IOException | InterruptedException e) {
    //             Logger.error("Failed to get server IP", e);
    //             return null;
    //         }
    //     }
    //     // Additional validation
    //     if (ip.startsWith("127.") || ip.equals("localhost")) {
    //         Logger.error("Invalid server IP detected: " + ip);
    //         return null;
    //     }
    //     return ip + ":" + port;
    // }
    // Remove license verification calls in verifyLicense - not needed when running in premium mode
    public void verifyLicense(String licenseKey, boolean isFromCommand) {
        // No-op: license checking is deactivated
    }

    public static void checkTrialStatus() {
        // No trial checks needed in premium mode
        isTrialActive = false;
    }

    public static boolean isPremium() {
        return true; // Force true so that plugin always runs as PREMIUM
    }

    public static boolean isTrialActive() {
        return false;
    }

    // private JsonObject parseJsonResponse(String responseBody) {
    //     try {
    //         return new Gson().fromJson(responseBody, JsonObject.class);
    //     } catch (Exception e) {
    //         Logger.error("Failed to parse JSON response", e);
    //         return null;
    //     }
    // }
    public static boolean isVerifying() {
        return isVerifying;
    }
}
