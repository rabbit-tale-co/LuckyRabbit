package co.RabbitTale.luckyRabbit.lootbox.animation;

import lombok.Getter;

@Getter
public enum AnimationType {
    /**
     * Classic horizontal spinning animation.
     * Default animation for free version.
     */
    HORIZONTAL("Classic horizontal spinning animation"),

    /**
     * Items appear one by one in a fixed spot.
     * Premium only.
     */
    PIN_POINT("Items appear one by one in a fixed spot"),

    /**
     * Items spin in a circle pattern.
     * Premium only.
     */
    CIRCLE("Items spin in a circle pattern"),

    /**
     * Items cascade across the screen.
     * Premium only.
     */
    CASCADE("Items cascade across the screen"),

    /**
     * Three items spinning in a row.
     * Premium only.
     */
    THREE_IN_ROW("Three items spinning in a row");

    private final String description;

    /**
     * Creates a new animation type.
     *
     * @param description Human-readable description
     */
    AnimationType(String description) {
        this.description = description;
    }
}
