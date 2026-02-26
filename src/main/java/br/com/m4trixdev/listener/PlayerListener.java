package br.com.m4trixdev.listener;

import br.com.m4trixdev.Main;
import br.com.m4trixdev.config.ConfigManager;
import br.com.m4trixdev.manager.EventManager;
import br.com.m4trixdev.model.EventState;
import br.com.m4trixdev.model.TeamData;
import br.com.m4trixdev.util.FlagUtil;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerListener implements Listener {

    private final Main plugin;
    private final EventManager eventManager;
    private final ConfigManager config;

    public PlayerListener(Main plugin, EventManager eventManager, ConfigManager config) {
        this.plugin = plugin;
        this.eventManager = eventManager;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!eventManager.isInEvent(player)) return;

        event.getDrops().clear();
        event.setKeepInventory(false);
        event.deathMessage(null);

        eventManager.handlePlayerDeath(player);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!eventManager.isPendingRespawn(player)) return;

        eventManager.removePendingRespawn(player);

        TeamData team = eventManager.getTeam(player);
        if (team != null && team.getSpawn() != null) {
            event.setRespawnLocation(team.getSpawn());
        }

        eventManager.handlePostRespawn(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!eventManager.isInEvent(player)) return;
        if (player.getGameMode() == GameMode.SPECTATOR) return;

        EventState state = eventManager.getState();
        if (state == EventState.WAITING || state == EventState.ENDING) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        boolean movedBlock = from.getBlockX() != to.getBlockX()
                || from.getBlockZ() != to.getBlockZ();
        if (!movedBlock) return;

        if (state == EventState.STARTING) {
            eventManager.pushBackToLastValid(player);
            return;
        }

        eventManager.handlePlayerMove(player, from, to);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!eventManager.isInEvent(player)) return;
        if (eventManager.getState() == EventState.STARTING) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        Item item = event.getItem();
        if (!FlagUtil.isFlag(item.getItemStack())) return;

        event.setCancelled(true);

        if (!eventManager.isInEvent(player)) return;
        if (player.getGameMode() == GameMode.SPECTATOR) return;

        EventState state = eventManager.getState();
        if (state != EventState.RUNNING) return;

        if (!eventManager.isDroppedFlagEntity(item)) return;

        eventManager.handleFlagPickup(player, item);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!eventManager.isInEvent(player)) return;
        if (event.getCurrentItem() == null) return;
        if (FlagUtil.isFlag(event.getCurrentItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        eventManager.handlePlayerQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!eventManager.isInEvent(player)) return;
        if (player.hasPermission("captureflag.admin")) return;

        String cmd = event.getMessage().toLowerCase().split(" ")[0];
        if (cmd.equals("/spawn") || cmd.equals("/home") || cmd.equals("/tp")
                || cmd.equals("/tpa") || cmd.equals("/tpahere") || cmd.equals("/warp")
                || cmd.equals("/back") || cmd.equals("/rtp")) {
            event.setCancelled(true);
            player.sendMessage(config.format("&cVoce nao pode usar esse comando durante o evento."));
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!eventManager.isInEvent(player)) return;

        if (player.getGameMode() == GameMode.SPECTATOR) {
            event.setCancelled(true);
            return;
        }

        if (FlagUtil.isFlag(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPickup(PlayerAttemptPickupItemEvent event) {
        Player player = event.getPlayer();
        if (!eventManager.isInEvent(player)) return;
        if (player.getGameMode() == GameMode.SPECTATOR) {
            event.setCancelled(true);
        }
    }
}
