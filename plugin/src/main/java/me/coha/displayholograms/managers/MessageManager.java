package me.coha.displayholograms.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MessageManager {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, String> messages = new HashMap<>();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private String prefix = "";

    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "messages.yml");
        load();
    }

    public void load() {
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        java.io.InputStream defStream = plugin.getResource("messages.yml");
        YamlConfiguration defConfig = null;
        if (defStream != null) {
            defConfig = YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(defStream));
        }

        messages.clear();

        for (String key : config.getKeys(true)) {
            if (config.isConfigurationSection(key)) continue;
            String value = config.getString(key);
            if (value != null) {
                if (key.equals("prefix")) prefix = value;
                else messages.put(key, value);
            }
        }

        if (defConfig != null) {
            for (String key : defConfig.getKeys(true)) {
                if (defConfig.isConfigurationSection(key)) continue;
                if (!messages.containsKey(key) && !key.equals("prefix")) {
                    String value = defConfig.getString(key);
                    if (value != null) messages.put(key, value);
                }
            }
        }
        
        plugin.getComponentLogger().info("Loaded " + messages.size() + " messages.");
    }

    public Component getMessage(String key, TagResolver... placeholders) {
        String message = messages.getOrDefault(key, key);
        if (message.isEmpty()) return Component.empty();
        
        return miniMessage.deserialize(prefix + message, placeholders);
    }

    public Component getMessageRaw(String key, TagResolver... placeholders) {
        String message = messages.getOrDefault(key, key);
        if (message.isEmpty()) return Component.empty();
        
        return miniMessage.deserialize(message, placeholders);
    }
}
