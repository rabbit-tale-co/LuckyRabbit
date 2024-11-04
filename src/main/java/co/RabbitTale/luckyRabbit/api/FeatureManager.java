package co.RabbitTale.luckyRabbit.api;

public class FeatureManager {
    private static LicenseManager licenseManager = null;

    public FeatureManager(LicenseManager licenseManager) {
        FeatureManager.licenseManager = licenseManager;
    }

    /**
     * Gets the maximum number of custom lootboxes allowed (excluding example lootboxes)
     * @return -1 for unlimited, otherwise the maximum number of custom lootboxes
     */
    public static int getMaxLootboxes() {
        if (LicenseManager.isPremium()) {
            return -1; // Unlimited
        }
        if (LicenseManager.isTrialActive()) {
            return 5; // Trial users can have 5 custom lootboxes (excluding examples)
        }
        return 2; // Free version limit (excluding examples)
    }

    /**
     * Checks if custom animations are allowed
     * @return true if custom animations are allowed
     */
    public boolean canUseCustomAnimations() {
        return LicenseManager.isPremium() || LicenseManager.isTrialActive();
    }

    /**
     * Checks if advanced features are allowed
     * @return true if advanced features are allowed
     */
    public boolean canUseAdvancedFeatures() {
        return LicenseManager.isPremium() || LicenseManager.isTrialActive();
    }

    /**
     * Checks if the user has full access to all features
     * @return true if the user has full access
     */
    public boolean hasFullAccess() {
        return licenseManager.hasFullAccess();
    }
}
