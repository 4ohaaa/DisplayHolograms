package me.coha.displayholograms;

import me.coha.displayholograms.api.DisplayHologramsAPI;
import me.coha.displayholograms.command.HologramCommand;
import me.coha.displayholograms.managers.HologramManager;
import me.coha.displayholograms.managers.MessageManager;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.Objects;

public class DisplayHologramsPlugin extends JavaPlugin {

    private static DisplayHologramsAPI api;
    private HologramManager hologramManager;
    private MessageManager messageManager;

    @Override
    public void onEnable() {
        messageManager = new MessageManager(this);

        hologramManager = new HologramManager(this, messageManager);
        api = hologramManager;

        HologramCommand command = new HologramCommand(hologramManager, messageManager);
        Objects.requireNonNull(getCommand("dish")).setExecutor(command);
        Objects.requireNonNull(getCommand("dish")).setTabCompleter(command);

        logBanner(true);
    }

    @Override
    public void onDisable() {
        if (hologramManager != null) {
            hologramManager.removeAllEntities();
            hologramManager.saveHolograms();
        }
        logBanner(false);
    }

    private void logBanner(boolean enabled) {
        String status = enabled ? "<green>Enable</green>" : "<red>Disable</red>";
        String color = enabled ? "<white>" : "<gray>";
        
        getComponentLogger().info(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(
            "\n" +
            color + "<blue>██████╗ ██╗███████╗██████╗ ██╗      █████╗ ██╗   ██╗</blue>\n" +
            color + "<blue>██╔══██╗██║██╔════╝██╔══██╗██║     ██╔══██╗╚██╗ ██╔╝</blue>\n" +
            color + "<blue>██║  ██║██║███████╗██████╔╝██║     ███████║ ╚████╔╝</blue> \n" +
            color + "<blue>██║  ██║██║╚════██║██╔═══╝ ██║     ██╔══██║  ╚██╔╝</blue>  \n" +
            color + "<blue>██████╔╝██║███████║██║     ███████╗██║  ██║   ██║</blue>   \n" +
            color + "<blue>╚═════╝ ╚═╝╚══════╝╚═╝     ╚══════╝╚═╝  ╚═╝   ╚═╝</blue>   \n" +
            color + "<green>██╗  ██╗ ██████╗ ██╗      ██████╗  ██████╗ ██████╗  █████╗ ███╗   ███╗███████╗</green>\n" +
            color + "<green>██║  ██║██╔═══██╗██║     ██╔═══██╗██╔════╝ ██╔══██╗██╔══██╗████╗ ████║██╔════╝</green>\n" +
            color + "<green>███████║██║   ██║██║     ██║   ██║██║  ███╗██████╔╝███████║██╔████╔██║███████╗</green>\n" +
            color + "<green>██╔══██║██║   ██║██║     ██║   ██║██║   ██║██╔══██╗██╔══██║██║╚██╔╝██║╚════██║</green>\n" +
            color + "<green>██║  ██║╚██████╔╝███████╗╚██████╔╝╚██████╔╝██║  ██║██║  ██║██║ ╚═╝ ██║███████║</green>\n" +
            color + "<green>╚═╝  ╚═╝ ╚═════╝ ╚══════╝ ╚═════╝  ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝╚═╝     ╚═╝╚══════╝</green>\n" +
            "                        <gray>Author: </gray><white>4oha</white>\n" +
            "                        <gray>Status: </gray>" + status + "\n"
        ));
    }


    public static DisplayHologramsAPI getApi() {
        return api;
    }

}
