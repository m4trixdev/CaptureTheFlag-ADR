package br.com.m4trixdev.listener;

import br.com.m4trixdev.config.ConfigManager;
import br.com.m4trixdev.manager.EventManager;
import br.com.m4trixdev.model.EventState;
import br.com.m4trixdev.model.TeamData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockListener implements Listener {

    private final EventManager eventManager;
    private final ConfigManager config;

    public BlockListener(EventManager eventManager, ConfigManager config) {
        this.eventManager = eventManager;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!eventManager.isInEvent(player)) return;

        EventState state = eventManager.getState();
        if (state != EventState.RUNNING && state != EventState.DEATH_ZONE) {
            event.setCancelled(true);
            return;
        }

        Location loc = event.getBlock().getLocation();

        if (!eventManager.isFlagBlock(loc)) return;

        event.setCancelled(true);
        event.setDropItems(false);

        if (state == EventState.DEATH_ZONE) return;

        TeamData owner = eventManager.getFlagOwner(loc);
        TeamData breaker = eventManager.getTeam(player);

        if (owner == null || breaker == null) return;

        if (owner == breaker) {
            player.sendMessage(config.format("&cVoce nao pode destruir a bandeira do seu proprio time!"));
            return;
        }

        if (!owner.isFlagAlive()) return;

        eventManager.handleFlagBreak(player, loc);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!eventManager.isInEvent(player)) return;

        EventState state = eventManager.getState();
        if (state != EventState.RUNNING && state != EventState.DEATH_ZONE) {
            event.setCancelled(true);
            return;
        }

        if (!eventManager.getArea().isInside(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }
}
