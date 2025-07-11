package co.RabbitTale.luckyRabbit.animations;

/**
 * Record representing animation data retrieved from the Supabase database
 *
 * @param validationKey The encrypted validation key for the animation
 * @param minApiVersion The minimum API version required for this animation
 */
public record AnimationRecord(
        String validationKey,
        String minApiVersion
        ) {

}
