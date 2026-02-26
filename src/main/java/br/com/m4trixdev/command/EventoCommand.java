package br.com.m4trixdev.command;

import br.com.m4trixdev.Main;
import br.com.m4trixdev.config.ConfigManager;
import br.com.m4trixdev.data.DataManager;
import br.com.m4trixdev.manager.EventManager;
import br.com.m4trixdev.model.EventArea;
import br.com.m4trixdev.model.EventState;
import br.com.m4trixdev.model.TeamData;
import br.com.m4trixdev.util.ColorUtil;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EventoCommand implements CommandExecutor, TabCompleter {

    private final ConfigManager config;
    private final EventManager eventManager;
    private final DataManager dataManager;

    public EventoCommand(Main plugin, ConfigManager config, EventManager eventManager, DataManager dataManager) {
        this.config = config;
        this.eventManager = eventManager;
        this.dataManager = dataManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(config.format("&cApenas jogadores podem usar este comando."));
            return true;
        }

        if (!player.hasPermission("captureflag.admin")) {
            player.sendMessage(config.format("&cVoce nao tem permissao."));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "iniciar" -> {
                if (!player.hasPermission("captureflag.start")) {
                    player.sendMessage(config.format("&cSem permissao para iniciar o evento."));
                    return true;
                }
                eventManager.startEvent(player);
            }
            case "parar" -> {
                if (!player.hasPermission("captureflag.stop")) {
                    player.sendMessage(config.format("&cSem permissao para parar o evento."));
                    return true;
                }
                if (eventManager.getState() == EventState.WAITING) {
                    player.sendMessage(config.format("&cNenhum evento em andamento."));
                } else {
                    eventManager.stopEvent();
                }
            }
            case "time1" -> handleTeamCommand(player, eventManager.getTeam1(), args);
            case "time2" -> handleTeamCommand(player, eventManager.getTeam2(), args);
            case "area" -> handleAreaCommand(player, args);
            default -> sendHelp(player);
        }

        return true;
    }

    private void handleTeamCommand(Player player, TeamData team, String[] args) {
        if (!player.hasPermission("captureflag.set")) {
            player.sendMessage(config.format("&cSem permissao para configurar times."));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(config.format("&eUso: /evento " + args[0].toLowerCase() + " <bandeira|spawn|inventory|zona>"));
            return;
        }

        switch (args[1].toLowerCase()) {
            case "bandeira" -> {
                RayTraceResult result = player.rayTraceBlocks(5.0);
                if (result == null || result.getHitBlock() == null) {
                    player.sendMessage(config.format("&cOlhe para um bloco."));
                    return;
                }
                Block block = result.getHitBlock();
                team.setFlagLocation(block.getLocation());
                team.setFlagMaterial(block.getType());
                dataManager.saveTeam(team);
                player.sendMessage(config.format("&aBandeira do " + ColorUtil.translate(team.getDisplayName())
                        + " &adefinida como &e" + block.getType().name() + "&a."));
            }
            case "spawn" -> {
                team.setSpawn(player.getLocation());
                dataManager.saveTeam(team);
                player.sendMessage(config.format("&aSpawn do " + ColorUtil.translate(team.getDisplayName()) + " &adefinido."));
            }
            case "inventory" -> {
                team.captureKit(player);
                dataManager.saveTeam(team);
                player.sendMessage(config.format("&aInventario do " + ColorUtil.translate(team.getDisplayName()) + " &asalvo."));
            }
            case "zona" -> handleDeliveryZoneCommand(player, team, args);
            default -> player.sendMessage(config.format("&eUso: /evento " + args[0].toLowerCase() + " <bandeira|spawn|inventory|zona>"));
        }
    }

    private void handleDeliveryZoneCommand(Player player, TeamData team, String[] args) {
        if (args.length < 3) {
            player.sendMessage(config.format("&eUso: /evento " + args[0].toLowerCase() + " zona <pos1|pos2>"));
            return;
        }

        EventArea dz = team.getDeliveryZone();
        switch (args[2].toLowerCase()) {
            case "pos1" -> {
                dz.setPos1(player.getLocation());
                dataManager.saveTeam(team);
                player.sendMessage(config.format("&aZona de entrega (pos1) do "
                        + ColorUtil.translate(team.getDisplayName()) + " &adefinida em " + formatLoc(player.getLocation())));
            }
            case "pos2" -> {
                dz.setPos2(player.getLocation());
                dataManager.saveTeam(team);
                player.sendMessage(config.format("&aZona de entrega (pos2) do "
                        + ColorUtil.translate(team.getDisplayName()) + " &adefinida em " + formatLoc(player.getLocation())));
            }
            default -> player.sendMessage(config.format("&eUso: /evento " + args[0].toLowerCase() + " zona <pos1|pos2>"));
        }
    }

    private void handleAreaCommand(Player player, String[] args) {
        if (!player.hasPermission("captureflag.set")) {
            player.sendMessage(config.format("&cSem permissao para definir a area."));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(config.format("&eUso: /evento area <pos1|pos2>"));
            return;
        }

        EventArea area = eventManager.getArea();
        switch (args[1].toLowerCase()) {
            case "pos1" -> {
                area.setPos1(player.getLocation());
                dataManager.saveArea(area);
                player.sendMessage(config.format("&aPosicao 1 da area definida em " + formatLoc(player.getLocation())));
            }
            case "pos2" -> {
                area.setPos2(player.getLocation());
                dataManager.saveArea(area);
                player.sendMessage(config.format("&aPosicao 2 da area definida em " + formatLoc(player.getLocation())));
            }
            default -> player.sendMessage(config.format("&eUso: /evento area <pos1|pos2>"));
        }
    }

    private String formatLoc(org.bukkit.Location loc) {
        return "&7(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
    }

    private void sendHelp(Player player) {
        player.sendMessage(ColorUtil.translate("&6&l--- CaptureTheFlag ---"));
        player.sendMessage(ColorUtil.translate("&e/evento iniciar            &8- &7Inicia o evento"));
        player.sendMessage(ColorUtil.translate("&e/evento parar              &8- &7Para o evento"));
        player.sendMessage(ColorUtil.translate("&e/evento time1 bandeira     &8- &7Define o bloco bandeira"));
        player.sendMessage(ColorUtil.translate("&e/evento time1 spawn        &8- &7Define o spawn"));
        player.sendMessage(ColorUtil.translate("&e/evento time1 inventory    &8- &7Salva o kit"));
        player.sendMessage(ColorUtil.translate("&e/evento time1 zona pos1    &8- &7Zona de entrega pos1"));
        player.sendMessage(ColorUtil.translate("&e/evento time1 zona pos2    &8- &7Zona de entrega pos2"));
        player.sendMessage(ColorUtil.translate("&e/evento time2 ...          &8- &7(mesmos subcomandos)"));
        player.sendMessage(ColorUtil.translate("&e/evento area pos1/pos2     &8- &7Area global do evento"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("iniciar", "parar", "time1", "time2", "area"));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "time1", "time2" -> completions.addAll(Arrays.asList("bandeira", "spawn", "inventory", "zona"));
                case "area" -> completions.addAll(Arrays.asList("pos1", "pos2"));
            }
        } else if (args.length == 3) {
            if ((args[0].equalsIgnoreCase("time1") || args[0].equalsIgnoreCase("time2"))
                    && args[1].equalsIgnoreCase("zona")) {
                completions.addAll(Arrays.asList("pos1", "pos2"));
            }
        }

        String input = args[args.length - 1].toLowerCase();
        completions.removeIf(s -> !s.startsWith(input));
        return completions;
    }
}
