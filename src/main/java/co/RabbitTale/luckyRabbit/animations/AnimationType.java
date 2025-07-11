package co.RabbitTale.luckyRabbit.animations;

import co.RabbitTale.luckyRabbit.LuckyRabbit;
import co.RabbitTale.luckyRabbit.utils.Logger;
import org.jetbrains.annotations.NotNull;

public enum AnimationType {
    HORIZONTAL("Horizontal Spin", true),
    CIRCLE("Circle Spin", false),
    THREE_IN_ROW("Three In Row", false),
    PIN_POINT("Pin Point", false),
    CASCADE("Cascade", false);

    private final String displayName;
    private final boolean isDefault;

    AnimationType(String displayName, boolean isDefault) {
        this.displayName = displayName;
        this.isDefault = isDefault;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public boolean isAvailable() {
        if (isDefault) {
            return true;
        }

        AnimationManager manager = LuckyRabbit.getInstance().getAnimationManager();
        return manager != null && manager.isAnimationAvailable(name());
    }

    @NotNull
    public static AnimationType getDefault() {
        return HORIZONTAL;
    }

    public static AnimationType fromString(String name) {
        try {
            AnimationType type = valueOf(name.toUpperCase());
            if (!type.isAvailable()) {
                Logger.warning("Animation " + name + " is not available, falling back to default");
                return getDefault();
            }
            return type;
        } catch (IllegalArgumentException e) {
            Logger.warning("Unknown animation type: " + name + ", falling back to default");
            return getDefault();
        }
    }
}
