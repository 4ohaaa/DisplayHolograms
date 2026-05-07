package me.coha.displayholograms.data;

import org.bukkit.Location;
import java.util.UUID;

/**
 * Данные о голограмме.
 * @param id          Уникальный ID
 * @param entityId    UUID сущности TextDisplay
 * @param worldName   Имя мира
 * @param location    Местоположение
 * @param text        Текст
 * @param expiryTime  Время удаления. -1 для вечных голограмм.
 */
public record HologramData(
    String id,
    UUID entityId,
    String worldName,
    Location location,
    String text,
    long expiryTime
) {
    public boolean isPermanent() {
        return expiryTime == -1;
    }

    public boolean isExpired() {
        return expiryTime > 0 && System.currentTimeMillis() > expiryTime;
    }
}
