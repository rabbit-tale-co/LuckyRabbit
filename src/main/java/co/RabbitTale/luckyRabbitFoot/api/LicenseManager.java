package co.RabbitTale.luckyRabbitFoot.api;

import co.RabbitTale.luckyRabbitFoot.LuckyRabbitFoot;
import co.RabbitTale.luckyRabbitFoot.utils.Logger;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

public class LicenseManager {
    private final LuckyRabbitFoot plugin;
    private final String apiUrl;
    private final File licenseFile;
    private boolean isValidLicense = false;
    private boolean isTrialActive = false;
    private Instant trialEndTime;
    private final HttpClient httpClient;

    public LicenseManager(LuckyRabbitFoot plugin) {
        this.plugin = plugin;
        this.apiUrl = plugin.getConfig().getString("api.url", "https://api.rabbittale.co/lootbox");
        this.licenseFile = new File(plugin.getDataFolder(), "license.dat");
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(5000))
            .build();
        loadLicenseData();
    }

    public void verifyLicense(String authCode) {
        CompletableFuture.runAsync(() -> {
            try {
                JsonObject requestData = new JsonObject();
                requestData.addProperty("authCode", authCode);
                requestData.addProperty("serverIp", plugin.getServer().getIp());
                requestData.addProperty("port", plugin.getServer().getPort());

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "/verify"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestData.toString()))
                    .build();

                HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonObject jsonResponse = new Gson().fromJson(response.body(), JsonObject.class);
                    boolean isValid = jsonResponse.get("valid").getAsBoolean();

                    if (isValid) {
                        isValidLicense = true;
                        saveLicenseData();
                        Logger.info("License verified successfully!");
                    } else {
                        String error = jsonResponse.get("error").getAsString();
                        Logger.warning("License verification failed: " + error);
                        startTrial();
                    }
                } else {
                    Logger.error("Failed to verify license. Status code: " + response.statusCode());
                    startTrial();
                }
            } catch (Exception e) {
                Logger.error("Failed to verify license", e);
                startTrial();
            }
        });
    }

    private void startTrial() {
        if (!isTrialActive && !isValidLicense) {
            isTrialActive = true;
            int trialDays = plugin.getConfig().getInt("license.trial.duration", 3);
            trialEndTime = Instant.now().plusSeconds((long) trialDays * 24 * 60 * 60);
            saveLicenseData();
            Logger.warning("Starting trial period for " + trialDays + " days");
        }
    }

    private void loadLicenseData() {
        try {
            if (!licenseFile.exists()) {
                startTrial();
                return;
            }

            String data = Files.readString(licenseFile.toPath());
            JsonObject jsonData = new Gson().fromJson(data, JsonObject.class);

            isValidLicense = jsonData.get("isValidLicense").getAsBoolean();
            isTrialActive = jsonData.get("isTrialActive").getAsBoolean();
            if (isTrialActive) {
                trialEndTime = Instant.parse(jsonData.get("trialEndTime").getAsString());
                if (Instant.now().isAfter(trialEndTime)) {
                    isTrialActive = false;
                }
            }
        } catch (Exception e) {
            Logger.error("Failed to load license data", e);
            startTrial();
        }
    }

    private void saveLicenseData() {
        try {
            JsonObject data = new JsonObject();
            data.addProperty("isValidLicense", isValidLicense);
            data.addProperty("isTrialActive", isTrialActive);
            if (trialEndTime != null) {
                data.addProperty("trialEndTime", trialEndTime.toString());
            }

            Files.writeString(licenseFile.toPath(), new Gson().toJson(data));
        } catch (Exception e) {
            Logger.error("Failed to save license data", e);
        }
    }

    public boolean isValidLicense() {
        return isValidLicense || (isTrialActive && Instant.now().isBefore(trialEndTime));
    }

    public boolean isTrialActive() {
        return isTrialActive && Instant.now().isBefore(trialEndTime);
    }

    public Instant getTrialEndTime() {
        return trialEndTime;
    }
}
