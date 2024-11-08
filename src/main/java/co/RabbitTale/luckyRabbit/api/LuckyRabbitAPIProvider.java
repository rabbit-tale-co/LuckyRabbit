package co.RabbitTale.luckyRabbit.api;

/**
 * Provider class for accessing LuckyRabbit API
 */
public class LuckyRabbitAPIProvider {
    private static LuckyRabbitAPI api;

    /**
     * Get the API instance
     * @return LuckyRabbitAPI instance
     * @throws IllegalStateException if API is not initialized
     */
    public static LuckyRabbitAPI getAPI() {
        if (api == null) {
            throw new IllegalStateException("LuckyRabbit API is not initialized!");
        }
        return api;
    }

    /**
     * Set the API instance (internal use only)
     * @param apiInstance API implementation instance
     */
    public static void setAPI(LuckyRabbitAPI apiInstance) {
        if (api != null) {
            throw new IllegalStateException("API is already initialized!");
        }
        api = apiInstance;
    }
}
