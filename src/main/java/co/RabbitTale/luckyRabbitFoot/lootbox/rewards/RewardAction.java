package co.RabbitTale.luckyRabbitFoot.lootbox.rewards;

import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import co.RabbitTale.luckyRabbitFoot.utils.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class RewardAction {
    private final String type;
    private final Map<String, Object> data;

    public RewardAction(String type, Map<String, Object> data) {
        this.type = type;
        this.data = data;
    }

    public String getType() {
        return type;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void execute(Player player) {
        switch (type.toUpperCase()) {
            case "ECONOMY" -> {
                double amount = (double) data.get("amount");
                String command = "eco give " + player.getName() + " " + amount;
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                player.sendMessage(Component.text("You received " + amount + " coins!")
                    .color(NamedTextColor.GOLD));
            }
            case "PERMISSION", "ROLE" -> {
                String group = (String) data.get("group");
                String duration = (String) data.get("duration");
                String command = duration.equals("permanent")
                    ? "lp user " + player.getName() + " parent add " + group
                    : "lp user " + player.getName() + " parent addtemp " + group + " " + duration;
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                player.sendMessage(Component.text("You received the " + group.toUpperCase() + " rank!")
                    .color(NamedTextColor.GOLD));
            }
            case "COMMAND" -> {
                List<String> commands = (List<String>) data.get("commands");
                for (String command : commands) {
                    command = command.replace("{player}", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                }
            }
            case "XP_BOOST" -> {
                String duration = (String) data.get("duration");
                double multiplier = (double) data.get("multiplier");
                String command = "xpboost give " + player.getName() + " " + multiplier + " " + duration;
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                player.sendMessage(Component.text("You received " + multiplier + "x XP Boost for " + duration + "!")
                    .color(NamedTextColor.GREEN));
            }
            default -> {
                Logger.warning("Unknown reward type: " + type);
            }
        }
    }

    public static RewardAction fromConfig(ConfigurationSection config) {
        if (config == null) return null;
        return new RewardAction(
            config.getString("type"),
            config.getValues(true)
        );
    }
}
