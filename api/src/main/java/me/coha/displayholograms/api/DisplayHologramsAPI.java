package me.coha.displayholograms.api;

import org.bukkit.Location;
import java.util.Collection;

public interface DisplayHologramsAPI {

    void createHologram(String id, Location location, String text);

    void createHologram(String id, Location location, String text, long durationTicks);

    void createHologramDelayed(String id, Location location, String text, long delayTicks, long durationTicks);

    void updateHologram(String id, String newText);

    void removeHologram(String id);

    void moveHologram(String id, Location newLocation);

    boolean hasHologram(String id);

    Collection<String> getHologramIds();

    boolean reload();
}
