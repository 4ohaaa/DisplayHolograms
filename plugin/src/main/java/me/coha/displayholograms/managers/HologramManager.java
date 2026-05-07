package me.coha.displayholograms.managers;

import me.coha.displayholograms.api.DisplayHologramsAPI;
import me.coha.displayholograms.data.HologramData;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.TextDisplay.TextAlignment;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class HologramManager implements DisplayHologramsAPI, Listener {

    private final JavaPlugin plugin;
    private final Map<String, HologramData> holograms = new HashMap<>();
    private final File configFile;
    private final NamespacedKey hologramKey;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private boolean loadedSuccessfully = false;
    private final MessageManager messageManager;

    public HologramManager(JavaPlugin plugin, MessageManager messageManager) {
        this.plugin = plugin;
        this.messageManager = messageManager;
        this.configFile = new File(plugin.getDataFolder(), "holograms.yml");
        this.hologramKey = new NamespacedKey(plugin, "hologram_id");
        
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        cleanupOrphanEntities();
        loadHolograms();
        
        Bukkit.getScheduler().runTaskLater(plugin, this::spawnAllHolograms, 20L);
        startHealthCheck();
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        spawnAllHolograms();
    }

    private void spawnAllHolograms() {
        for (HologramData data : holograms.values()) {
            if (getEntity(data) == null) {
                respawnHologram(data);
            }
        }
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Entity entity : event.getEntities()) {
                if (entity instanceof TextDisplay display) {
                    if (display.getPersistentDataContainer().has(hologramKey, PersistentDataType.STRING)) {
                        boolean isManaged = holograms.values().stream()
                            .anyMatch(data -> data.entityId().equals(display.getUniqueId()));

                        if (!isManaged) {
                            display.remove();
                        }
                    }
                }
            }
        });
    }

    private void cleanupOrphanEntities() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntitiesByClass(TextDisplay.class)) {
                if (entity.getPersistentDataContainer().has(hologramKey, PersistentDataType.STRING)) {
                    entity.remove();
                }
            }
        }
    }

    public void removeAllEntities() {
        for (HologramData data : holograms.values()) {
            TextDisplay display = getEntity(data);
            if (display != null) {
                display.remove();
            }
        }
    }

    private void startHealthCheck() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Iterator<Map.Entry<String, HologramData>> it = holograms.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, HologramData> entry = it.next();
                HologramData data = entry.getValue();

                if (data.isExpired()) {
                    TextDisplay display = getEntity(data);
                    if (display != null) display.remove();
                    it.remove();
                    continue;
                }

                TextDisplay display = getEntity(data);
                if (display == null || !display.isValid()) {
                    respawnHologram(data);
                }
            }
        }, 100L, 100L);
    }

    private void respawnHologram(HologramData data) {
        Location loc = data.location();
        if (loc.getWorld() == null) {
            World world = Bukkit.getWorld(data.worldName());
            if (world == null) return;
            loc.setWorld(world);
        }

        TextDisplay display = (TextDisplay) loc.getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);
        configureDisplay(display, data.id(), data.text());

        HologramData newData = new HologramData(data.id(), display.getUniqueId(), data.worldName(), loc, data.text(), data.expiryTime());
        holograms.put(data.id(), newData);
    }

    private void configureDisplay(TextDisplay display, String id, String text) {
        display.text(miniMessage.deserialize(processText(text)));
        display.setBillboard(Billboard.CENTER);
        display.setAlignment(TextAlignment.CENTER);
        display.setShadowed(true);
        display.setBackgroundColor(Color.fromARGB(128, 0, 0, 0));
        display.getPersistentDataContainer().set(hologramKey, PersistentDataType.STRING, id);
        display.setPersistent(false);
    }

    @Override
    public void createHologram(String id, Location location, String text) {
        createHologram(id, location, text, -1L);
    }

    @Override
    public void createHologram(String id, Location location, String text, long durationTicks) {
        if (holograms.containsKey(id)) {
            removeHologram(id);
        }

        long expiryTime;
        if (durationTicks > 0) {
            expiryTime = System.currentTimeMillis() + (durationTicks * 50);
        } else {
            expiryTime = durationTicks;
        }

        TextDisplay display = (TextDisplay) location.getWorld().spawnEntity(location, EntityType.TEXT_DISPLAY);
        configureDisplay(display, id, text);

        HologramData data = new HologramData(id, display.getUniqueId(), location.getWorld().getName(), location, text, expiryTime);
        holograms.put(id, data);
        
        if (data.isPermanent()) {
            saveHolograms();
        }
    }

    @Override
    public void createHologramDelayed(String id, Location location, String text, long delayTicks, long durationTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> createHologram(id, location, text, durationTicks), delayTicks);
    }

    @Override
    public void updateHologram(String id, String newText) {
        HologramData data = holograms.get(id);
        if (data == null) return;

        TextDisplay display = getEntity(data);
        if (display != null) {
            display.text(miniMessage.deserialize(processText(newText)));
            HologramData newData = new HologramData(id, data.entityId(), data.worldName(), data.location(), newText, data.expiryTime());
            holograms.put(id, newData);
            if (data.isPermanent()) {
                saveHolograms();
            }
        } else {
            createHologram(id, data.location(), newText, data.expiryTime() == -1 ? -1 : (data.expiryTime() - System.currentTimeMillis()) / 50);
        }
    }

    @Override
    public void moveHologram(String id, Location newLocation) {
        HologramData data = holograms.get(id);
        if (data == null) return;

        TextDisplay display = getEntity(data);
        if (display != null) {
            display.teleport(newLocation);
            HologramData newData = new HologramData(id, data.entityId(), data.worldName(), newLocation, data.text(), data.expiryTime());
            holograms.put(id, newData);
            if (data.isPermanent()) {
                saveHolograms();
            }
        }
    }

    @Override
    public boolean hasHologram(String id) {
        return holograms.containsKey(id);
    }

    @Override
    public Collection<String> getHologramIds() {
        return holograms.keySet();
    }

    @Override
    public boolean reload() {
        return reloadHolograms();
    }

    public boolean reloadHolograms() {
        messageManager.load();
        for (HologramData data : holograms.values()) {
            TextDisplay display = getEntity(data);
            if (display != null) {
                display.remove();
            }
        }
        holograms.clear();
        loadHolograms();
        for (HologramData data : holograms.values()) {
            respawnHologram(data);
        }
        return loadedSuccessfully;
    }

    private String processText(String text) {
        String replaced = text.replace("\\n", "\n");
        String[] lines = replaced.split("\n");
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        for (String line : lines) {
            sb.append("   ").append(line).append("   \n");
        }
        return sb.toString();
    }

    @Override
    public void removeHologram(String id) {
        HologramData data = holograms.remove(id);
        if (data != null) {
            TextDisplay display = getEntity(data);
            if (display != null) {
                display.remove();
            }
            if (data.isPermanent()) {
                saveHolograms();
            }
        }
    }

    private TextDisplay getEntity(HologramData data) {
        return (TextDisplay) Bukkit.getEntity(data.entityId());
    }

    public void loadHolograms() {
        if (!configFile.exists()) {
            loadedSuccessfully = true;
            return;
        }

        holograms.clear();
        YamlConfiguration config;
        try {
            config = YamlConfiguration.loadConfiguration(configFile);
        } catch (Exception e) {
            loadedSuccessfully = false;
            return;
        }

        if (config.getConfigurationSection("holograms") == null) {
            loadedSuccessfully = true;
            return;
        }

        for (String id : config.getConfigurationSection("holograms").getKeys(false)) {
            String path = "holograms." + id;
            String uuidStr = config.getString(path + ".uuid");
            if (uuidStr == null) continue;
            
            UUID entityId = UUID.fromString(uuidStr);
            String text = config.getString(path + ".text");
            
            HologramData data = null;
            if (config.isConfigurationSection(path + ".location")) {
                data = loadSafeHologramData(id, entityId, text, config.getConfigurationSection(path + ".location"));
            } else {
                try {
                    Location loc = config.getLocation(path + ".location");
                    if (loc != null) {
                        data = new HologramData(id, entityId, loc.getWorld() != null ? loc.getWorld().getName() : "world", loc, text, -1L);
                    }
                } catch (Exception e) {}
            }

            if (data != null) {
                holograms.put(id, data);
            }
        }
        loadedSuccessfully = true;
    }

    private HologramData loadSafeHologramData(String id, UUID entityId, String text, org.bukkit.configuration.ConfigurationSection section) {
        String worldName = section.getString("world");
        if (worldName == null) return null;
        World world = Bukkit.getWorld(worldName);
        
        Location loc = new Location(
            world,
            section.getDouble("x"),
            section.getDouble("y"),
            section.getDouble("z"),
            (float) section.getDouble("yaw"),
            (float) section.getDouble("pitch")
        );
        
        return new HologramData(id, entityId, worldName, loc, text, -1L);
    }

    public void saveHolograms() {
        if (!loadedSuccessfully) return;

        YamlConfiguration config = new YamlConfiguration();
        for (HologramData data : holograms.values()) {
            if (!data.isPermanent()) continue;
            
            String path = "holograms." + data.id();
            config.set(path + ".uuid", data.entityId().toString());
            
            Map<String, Object> locMap = new HashMap<>();
            locMap.put("world", data.worldName());
            locMap.put("x", data.location().getX());
            locMap.put("y", data.location().getY());
            locMap.put("z", data.location().getZ());
            locMap.put("yaw", (double) data.location().getYaw());
            locMap.put("pitch", (double) data.location().getPitch());
            config.set(path + ".location", locMap);
            
            config.set(path + ".text", data.text());
        }

        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getComponentLogger().error("Ошибка сохранения holograms.yml", e);
        }
    }

    public HologramData getHologram(String id) {
        return holograms.get(id);
    }
}
