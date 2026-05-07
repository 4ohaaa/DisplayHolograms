package me.coha.displayholograms.command;

import me.coha.displayholograms.data.HologramData;
import me.coha.displayholograms.managers.HologramManager;
import me.coha.displayholograms.managers.MessageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class HologramCommand implements CommandExecutor, TabCompleter {

    private final HologramManager manager;
    private final MessageManager messages;
    private static final int ITEMS_PER_PAGE = 10;

    public HologramCommand(HologramManager manager, MessageManager messages) {
        this.manager = manager;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String action = args[0].toLowerCase();

        if (action.equals("reload")) {
            boolean success = manager.reloadHolograms();
            if (success) {
                sender.sendMessage(messages.getMessage("reload-success"));
            } else {
                sender.sendMessage(messages.getMessageRaw("<red>Ошибка при перезагрузке плагина!</red>"));
            }
            return true;
        }

        if (action.equals("list")) {
            handleList(sender, args);
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.getMessageRaw("player-only"));
            return true;
        }

        String id = args.length > 1 ? args[1] : null;

        switch (action) {
            case "create" -> {
                if (id == null || args.length < 3) {
                    player.sendMessage(messages.getMessage("create-usage"));
                    return true;
                }
                String text = String.join(" ", extractText(args, 2));
                manager.createHologram(id, player.getLocation(), text);
                player.sendMessage(messages.getMessage("create-success", Placeholder.unparsed("id", id)));
            }
            case "edit" -> {
                if (id == null || args.length < 3) {
                    player.sendMessage(messages.getMessage("edit-usage"));
                    return true;
                }
                if (!manager.hasHologram(id)) {
                    player.sendMessage(messages.getMessage("hologram-not-found", Placeholder.unparsed("id", id)));
                    return true;
                }
                String text = String.join(" ", extractText(args, 2));
                manager.updateHologram(id, text);
                player.sendMessage(messages.getMessage("edit-success", Placeholder.unparsed("id", id)));
            }
            case "remove", "delete" -> {
                if (id == null) {
                    player.sendMessage(messages.getMessage("remove-usage"));
                    return true;
                }
                if (!manager.hasHologram(id)) {
                    player.sendMessage(messages.getMessage("hologram-not-found", Placeholder.unparsed("id", id)));
                    return true;
                }
                manager.removeHologram(id);
                player.sendMessage(messages.getMessage("remove-success", Placeholder.unparsed("id", id)));
            }
            case "move" -> {
                if (id == null || args.length < 5) {
                    player.sendMessage(messages.getMessage("move-usage"));
                    return true;
                }
                if (!manager.hasHologram(id)) {
                    player.sendMessage(messages.getMessage("hologram-not-found", Placeholder.unparsed("id", id)));
                    return true;
                }
                try {
                    double x = parseCoordinate(args[2], player.getLocation().getX());
                    double y = parseCoordinate(args[3], player.getLocation().getY());
                    double z = parseCoordinate(args[4], player.getLocation().getZ());
                    manager.moveHologram(id, new org.bukkit.Location(player.getWorld(), x, y, z));
                    player.sendMessage(messages.getMessage("move-success", Placeholder.unparsed("id", id)));
                } catch (NumberFormatException e) {
                    player.sendMessage(messages.getMessage("move-invalid-coords"));
                }
            }
            case "movehere" -> {
                if (id == null) {
                    player.sendMessage(messages.getMessage("movehere-usage"));
                    return true;
                }
                if (!manager.hasHologram(id)) {
                    player.sendMessage(messages.getMessage("hologram-not-found", Placeholder.unparsed("id", id)));
                    return true;
                }
                manager.moveHologram(id, player.getLocation());
                player.sendMessage(messages.getMessage("movehere-success", Placeholder.unparsed("id", id)));
            }
            case "teleport", "tp" -> {
                if (id == null) {
                    player.sendMessage(messages.getMessage("teleport-usage"));
                    return true;
                }
                HologramData data = manager.getHologram(id);
                if (data == null) {
                    player.sendMessage(messages.getMessage("hologram-not-found", Placeholder.unparsed("id", id)));
                    return true;
                }
                player.teleport(data.location());
                player.sendMessage(messages.getMessage("teleport-success", Placeholder.unparsed("id", id)));
            }
            default -> sendUsage(player);
        }

        return true;
    }

    private void handleList(CommandSender sender, String[] args) {
        List<String> ids = new ArrayList<>(manager.getHologramIds());
        Collections.sort(ids);

        if (ids.isEmpty()) {
            sender.sendMessage(messages.getMessage("list-empty"));
            return;
        }

        int totalPages = (int) Math.ceil((double) ids.size() / ITEMS_PER_PAGE);
        int page = 1;

        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(messages.getMessage("list-invalid-page"));
                return;
            }
        }

        if (page < 1 || page > totalPages) {
            sender.sendMessage(messages.getMessage("list-invalid-page"));
            return;
        }

        sender.sendMessage(messages.getMessageRaw("list-header", 
            Placeholder.unparsed("page", String.valueOf(page)),
            Placeholder.unparsed("max_pages", String.valueOf(totalPages))
        ));

        int start = (page - 1) * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, ids.size());

        for (int i = start; i < end; i++) {
            String fullId = ids.get(i);
            String displayId = fullId.length() > 25 ? fullId.substring(0, 22) + "..." : fullId;

            // Интерактивный ID (поддерживаем оба варианта плейсхолдера для совместимости)
            net.kyori.adventure.text.Component idComponent = Component.text(displayId)
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(Component.text("ID: " + fullId)));
            
            net.kyori.adventure.text.minimessage.tag.resolver.TagResolver idTag1 = 
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component("display_id", idComponent);
            net.kyori.adventure.text.minimessage.tag.resolver.TagResolver idTag2 = 
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component("id_display", idComponent);

            // Интерактивная кнопка ТП (используем основную команду /dish)
            net.kyori.adventure.text.minimessage.tag.resolver.TagResolver tpTag = 
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component("tp_btn", 
                    Component.text("ТП")
                        .color(net.kyori.adventure.text.format.NamedTextColor.GOLD)
                        .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD)
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/dish teleport " + fullId))
                        .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(Component.text("Телепорт к " + fullId)))
                );

            sender.sendMessage(messages.getMessageRaw("list-item", idTag1, idTag2, tpTag));
        }

        if (totalPages > 1) {
            Component nav = Component.empty();
            if (page > 1) {
                Component prevBtn = Component.text("« Назад")
                    .color(net.kyori.adventure.text.format.NamedTextColor.GRAY)
                    .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/dish list " + (page - 1)))
                    .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(Component.text("Перейти на страницу " + (page - 1))));
                nav = nav.append(prevBtn);
            }
            
            if (page > 1 && page < totalPages) {
                nav = nav.append(Component.text(" | ").color(net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY));
            }
            
            if (page < totalPages) {
                Component nextBtn = Component.text("Вперед »")
                    .color(net.kyori.adventure.text.format.NamedTextColor.GRAY)
                    .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/dish list " + (page + 1)))
                    .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(Component.text("Перейти на страницу " + (page + 1))));
                nav = nav.append(nextBtn);
            }
            sender.sendMessage(nav);
        }
        sender.sendMessage(messages.getMessageRaw("list-footer"));
    }

    private String[] extractText(String[] args, int start) {
        String[] text = new String[args.length - start];
        System.arraycopy(args, start, text, 0, args.length - start);
        return text;
    }

    private double parseCoordinate(String arg, double current) throws NumberFormatException {
        if (arg.startsWith("~")) {
            if (arg.length() == 1) return current;
            return current + Double.parseDouble(arg.substring(1));
        }
        return Double.parseDouble(arg);
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(messages.getMessage("invalid-usage"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            // Убираем дубликаты (delete, tp) из подсказок
            return List.of("create", "edit", "remove", "move", "movehere", "teleport", "list", "reload").stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        String action = args[0].toLowerCase();
        
        if (args.length == 2) {
            if (action.equals("list")) {
                return args[1].isEmpty() ? List.of("[Страница]") : List.of("1");
            }
            if (action.equals("create")) {
                return args[1].isEmpty() ? List.of("[ID]") : Collections.emptyList();
            }
            if (action.equals("reload")) {
                return Collections.emptyList();
            }
            // Для остальных команд подсказываем существующие ID
            return manager.getHologramIds().stream()
                .filter(s -> s.startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 3) {
            switch (action) {
                case "create", "edit" -> {
                    return args[2].isEmpty() ? List.of("[Текст]") : Collections.emptyList();
                }
                case "move" -> {
                    return args[2].isEmpty() ? List.of("[X]") : Collections.emptyList();
                }
            }
        }
        
        if (args.length == 4 && action.equals("move")) {
            return args[3].isEmpty() ? List.of("[Y]") : Collections.emptyList();
        }
        
        if (args.length == 5 && action.equals("move")) {
            return args[4].isEmpty() ? List.of("[Z]") : Collections.emptyList();
        }

        return Collections.emptyList();
    }
}
