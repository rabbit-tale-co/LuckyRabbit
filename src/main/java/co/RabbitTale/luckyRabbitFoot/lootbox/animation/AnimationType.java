package co.RabbitTale.luckyRabbitFoot.lootbox.animation;

import lombok.Getter;

public enum AnimationType {
    HORIZONTAL("Classic horizontal spinning animation"),
    PIN_POINT("Items appear one by one in a fixed spot"),
    CIRCLE("Items spin in a circle pattern"),
    CASCADE("Items cascade across the screen"),
    THREE_IN_ROW("Three items spinning in a row");

    @Getter
    private final String description;

    AnimationType(String description) {
        this.description = description;
    }
}
