package co.RabbitTale.luckyRabbitFoot.utils;

import org.jetbrains.annotations.NotNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.awt.Color;

public class ColorFormat {

    /**
     * Convert RGB color values to a Minecraft-compatible hex color format (e.g.,
     * §x§R§R§G§G§B§B)
     */
    public static @NotNull String getHexColor(int red, int green, int blue) {
        return String.format("§x§%1$s§%1$s§%2$s§%2$s§%3$s§%3$s",
                Integer.toHexString(red & 0xF),
                Integer.toHexString(green & 0xF),
                Integer.toHexString(blue & 0xF));
    }

    /**
     * Generates a gradient Component from `startColor` to `endColor` over the
     * length of the `text`.
     */
    public static @NotNull Component generateGradientComponent(@NotNull String text, @NotNull Color startColor,
                                                               @NotNull Color endColor) {
        Component gradientText = Component.empty();
        int textLength = text.length();

        for (int i = 0; i < textLength; i++) {
            float ratio = (float) i / (textLength - 1);
            Color interpolatedColor = interpolateColor(startColor, endColor, ratio);

            Component coloredChar = Component.text(String.valueOf(text.charAt(i)))
                    .color(
                            TextColor.color(interpolatedColor.getRed(), interpolatedColor.getGreen(), interpolatedColor.getBlue()));

            gradientText = gradientText.append(coloredChar);
        }

        return gradientText;
    }

    /**
     * Interpolates between two colors based on a ratio.
     */
    public static @NotNull Color interpolateColor(@NotNull Color startColor, @NotNull Color endColor, float ratio) {
        int red = (int) (startColor.getRed() * (1 - ratio) + endColor.getRed() * ratio);
        int green = (int) (startColor.getGreen() * (1 - ratio) + endColor.getGreen() * ratio);
        int blue = (int) (startColor.getBlue() * (1 - ratio) + endColor.getBlue() * ratio);

        return new Color(red, green, blue);
    }
}
