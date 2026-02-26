package br.com.m4trixdev.manager;

import br.com.m4trixdev.Main;
import br.com.m4trixdev.config.ConfigManager;
import br.com.m4trixdev.data.DataManager;
import br.com.m4trixdev.model.EventArea;
import br.com.m4trixdev.model.EventState;
import br.com.m4trixdev.model.TeamData;
import br.com.m4trixdev.util.ColorUtil;
import br.com.m4trixdev.util.FlagUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class EventManager {

    private final Main plugin;
    private final ConfigManager config;
    private final DataManager dataManager;
    private final CTFScoreboardManager scoreboardManager;

    private EventState state = EventState.WAITING;
    private final TeamData team1;
    private final TeamData team2;
    private final EventArea area = new EventArea();

    private BukkitTask timerTask;
    private BukkitTask startingTask;
    private final Map<UUID, BukkitTask> reviveTasks = new HashMap<>();
    private final Set<UUID> pendingRespawn = new HashSet<>();
    private final Map<UUID, Boolean> reviveEligibility = new HashMap<>();
    private final Set<UUID> permanentSpectators = new HashSet<>();

    private int remainingSeconds;
    private final Map<UUID, Location> lastValidPosition = new HashMap<>();
    private final Set<UUID> teleportCooldown = new HashSet<>();
    private final Map<String, Item> droppedFlagEntities = new HashMap<>();

    public EventManager(Main plugin, ConfigManager config, DataManager dataManager, CTFScoreboardManager scoreboardManager) {
        this.plugin = plugin;
        this.config = config;
        this.dataManager = dataManager;
        this.scoreboardManager = scoreboardManager;

        team1 = new TeamData("time1", config.getTeam1Name());
        team2 = new TeamData("time2", config.getTeam2Name());

        dataManager.loadTeam(team1);
        dataManager.loadTeam(team2);
        dataManager.loadArea(area);
    }

    public void startEvent(Player sender) {
        if (state != EventState.WAITING) {
            sender.sendMessage(config.format("&cJa existe um evento em andamento."));
            return;
        }
        if (!team1.isReady() || !team2.isReady()) {
            sender.sendMessage(config.format("&cConfiguracao incompleta. Defina spawn, bandeira e inventory para ambos os times."));
            return;
        }
        if (!team1.getDeliveryZone().isComplete() || !team2.getDeliveryZone().isComplete()) {
            sender.sendMessage(config.format("&cDefina a zona de entrega de ambos os times com /evento time1 zona pos1/pos2."));
            return;
        }
        if (!area.isComplete()) {
            sender.sendMessage(config.format("&cA area do evento nao foi definida. Use /evento area pos1 e pos2."));
            return;
        }

        List<Player> online = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.hasPermission("captureflag.bypass")) online.add(p);
        }

        if (online.size() < config.getMinPlayers()) {
            sender.sendMessage(config.format("&cJogadores insuficientes. Minimo: " + config.getMinPlayers()));
            return;
        }

        Collections.shuffle(online);

        List<Player> eligible = new ArrayList<>();
        for (Player p : online) {
            if (eligible.size() >= config.getMaxPlayers()) break;
            eligible.add(p);
        }

        team1.getPlayers().clear();
        team2.getPlayers().clear();
        permanentSpectators.clear();
        reviveEligibility.clear();
        pendingRespawn.clear();
        clearDroppedFlags();

        for (int i = 0; i < eligible.size(); i++) {
            UUID uuid = eligible.get(i).getUniqueId();
            if (i % 2 == 0) team1.addPlayer(uuid);
            else team2.addPlayer(uuid);
        }

        team1.setFlagAlive(true);
        team1.setFlagCarrierUUID(null);
        team2.setFlagAlive(true);
        team2.setFlagCarrierUUID(null);

        placeFlagBlock(team1);
        placeFlagBlock(team2);

        state = EventState.STARTING;

        for (UUID uuid : team1.getPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.getInventory().clear();
                p.setGameMode(GameMode.SURVIVAL);
                p.teleport(team1.getSpawn());
                scoreboardManager.showToPlayer(p);
                lastValidPosition.put(uuid, team1.getSpawn().clone());
            }
        }
        for (UUID uuid : team2.getPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.getInventory().clear();
                p.setGameMode(GameMode.SURVIVAL);
                p.teleport(team2.getSpawn());
                scoreboardManager.showToPlayer(p);
                lastValidPosition.put(uuid, team2.getSpawn().clone());
            }
        }

        refreshScoreboard();
        runStartCountdown(5);
    }

    private void runStartCountdown(int seconds) {
        int[] counter = {seconds};
        BukkitTask[] holder = new BukkitTask[1];

        holder[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (state == EventState.WAITING || state == EventState.ENDING) {
                holder[0].cancel();
                return;
            }
            if (counter[0] <= 0) {
                holder[0].cancel();
                startingTask = null;
                beginBattle();
                return;
            }
            String raw = config.getMsgCountdown().replace("%time%", String.valueOf(counter[0]));
            if (!ColorUtil.isEmpty(raw)) {
                Title t = Title.title(
                        ColorUtil.component(raw),
                        Component.empty(),
                        Title.Times.times(Duration.ZERO, Duration.ofMillis(1100), Duration.ZERO)
                );
                broadcastTitle(t);
            }
            counter[0]--;
        }, 0L, 20L);

        startingTask = holder[0];
    }

    private void beginBattle() {
        state = EventState.RUNNING;

        placeFlagBlock(team1);
        placeFlagBlock(team2);

        for (UUID uuid : team1.getPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                team1.applyKit(p);
                p.setHealth(maxHealth(p));
                p.setFoodLevel(20);
            }
        }
        for (UUID uuid : team2.getPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                team2.applyKit(p);
                p.setHealth(maxHealth(p));
                p.setFoodLevel(20);
            }
        }

        String startMsg = config.getMsgEventStart();
        if (!ColorUtil.isEmpty(startMsg)) {
            Bukkit.broadcastMessage(config.format(startMsg));
        }

        remainingSeconds = config.getDuration() * 60;
        refreshScoreboard();
        startTimer();
    }

    private void startTimer() {
        timerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (state != EventState.RUNNING) {
                timerTask.cancel();
                timerTask = null;
                return;
            }

            if (remainingSeconds <= 0) {
                timerTask.cancel();
                timerTask = null;
                if (config.isDeathZoneEnabled()) {
                    activateDeathZone();
                } else {
                    endEvent(null);
                }
                return;
            }

            remainingSeconds--;
            refreshScoreboard();
        }, 20L, 20L);
    }

    private void activateDeathZone() {
        state = EventState.DEATH_ZONE;

        forceReturnFlag(team1);
        forceReturnFlag(team2);

        for (BukkitTask task : reviveTasks.values()) task.cancel();
        reviveTasks.clear();

        for (UUID uuid : getAllEventPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.getGameMode() == GameMode.SPECTATOR) {
                permanentSpectators.add(uuid);
            }
        }

        String msg = config.getMsgDeathZoneStart();
        if (!ColorUtil.isEmpty(msg)) {
            Bukkit.broadcastMessage(config.format(msg));
        }

        refreshScoreboard();
        checkTeamElimination();
    }

    public void stopEvent() {
        if (state == EventState.WAITING) return;

        cancelAllTasks();
        state = EventState.ENDING;

        forceReturnFlag(team1);
        forceReturnFlag(team2);
        clearDroppedFlags();
        cleanupPlayers();

        team1.getPlayers().clear();
        team2.getPlayers().clear();
        permanentSpectators.clear();
        reviveEligibility.clear();
        pendingRespawn.clear();

        String msg = config.getMsgEventStop();
        if (!ColorUtil.isEmpty(msg)) {
            Bukkit.broadcastMessage(config.format(msg));
        }

        state = EventState.WAITING;
    }

    private void endEvent(TeamData winner) {
        if (state == EventState.ENDING) return;
        state = EventState.ENDING;

        cancelAllTasks();
        forceReturnFlag(team1);
        forceReturnFlag(team2);
        clearDroppedFlags();

        if (winner != null) {
            String msg = config.getMsgTeamWin()
                    .replace("%team%", ColorUtil.translate(winner.getDisplayName()));
            if (!ColorUtil.isEmpty(msg)) {
                Bukkit.broadcastMessage(config.format(msg));
            }
        }

        cleanupPlayers();

        team1.getPlayers().clear();
        team2.getPlayers().clear();
        permanentSpectators.clear();
        reviveEligibility.clear();
        pendingRespawn.clear();

        state = EventState.WAITING;
    }

    private void cleanupPlayers() {
        for (UUID uuid : getAllEventPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                scoreboardManager.hideFromPlayer(p);
                p.setGameMode(GameMode.SURVIVAL);
                p.getInventory().clear();
                p.clearActivePotionEffects();
                p.setGlowing(false);
            }
            lastValidPosition.remove(uuid);
            teleportCooldown.remove(uuid);
        }
    }

    private void clearDroppedFlags() {
        for (Item item : droppedFlagEntities.values()) {
            if (item != null && !item.isDead()) item.remove();
        }
        droppedFlagEntities.clear();
    }

    private void forceReturnFlag(TeamData team) {
        if (team.isFlagBeingCarried()) {
            Player carrier = Bukkit.getPlayer(team.getFlagCarrierUUID());
            if (carrier != null) {
                FlagUtil.removeFlagFromInventory(carrier, team);
                if (config.isGlowEnabled()) {
                    scoreboardManager.removeFromGlowTeams(carrier);
                }
            }
            team.setFlagCarrierUUID(null);
        }

        Item dropped = droppedFlagEntities.remove(team.getId());
        if (dropped != null && !dropped.isDead()) dropped.remove();

        team.setFlagAlive(true);
        placeFlagBlock(team);
    }

    private void cancelAllTasks() {
        if (timerTask != null) { timerTask.cancel(); timerTask = null; }
        if (startingTask != null) { startingTask.cancel(); startingTask = null; }
        for (BukkitTask task : reviveTasks.values()) task.cancel();
        reviveTasks.clear();
    }

    public void handleFlagBreak(Player breaker, Location blockLoc) {
        TeamData flagTeam;
        if (isFlagLocation(team1, blockLoc)) flagTeam = team1;
        else if (isFlagLocation(team2, blockLoc)) flagTeam = team2;
        else return;

        if (!flagTeam.isFlagAlive()) return;

        flagTeam.setFlagAlive(false);
        flagTeam.getFlagLocation().getBlock().setType(Material.AIR);

        ItemStack flagItem = FlagUtil.createFlagItem(flagTeam);

        if (breaker.getInventory().firstEmpty() != -1) {
            breaker.getInventory().addItem(flagItem);
            flagTeam.setFlagCarrierUUID(breaker.getUniqueId());

            if (config.isGlowEnabled()) {
                int glowIndex = flagTeam == team1 ? 1 : 2;
                scoreboardManager.addToGlowTeam(breaker, glowIndex);
            }

            String msg = config.getMsgFlagPickedUp()
                    .replace("%player%", breaker.getName())
                    .replace("%team%", ColorUtil.translate(flagTeam.getDisplayName()));
            if (!ColorUtil.isEmpty(msg)) {
                Bukkit.broadcastMessage(config.format(msg));
            }
        } else {
            Location dropLoc = blockLoc.clone().add(0.5, 0.1, 0.5);
            Item entity = dropLoc.getWorld().dropItem(dropLoc, flagItem);
            entity.setUnlimitedLifetime(true);
            entity.setPickupDelay(0);
            entity.setVelocity(new Vector(0, 0, 0));
            droppedFlagEntities.put(flagTeam.getId(), entity);

            String msg = config.getMsgFlagDropped()
                    .replace("%team%", ColorUtil.translate(flagTeam.getDisplayName()));
            if (!ColorUtil.isEmpty(msg)) {
                Bukkit.broadcastMessage(config.format(msg));
            }
        }

        refreshScoreboard();
    }

    public void handleFlagPickup(Player player, Item itemEntity) {
        String teamId = getDroppedFlagTeamId(itemEntity);
        if (teamId == null) return;

        TeamData flagTeam = teamId.equals(team1.getId()) ? team1 : team2;
        TeamData playerTeam = getTeam(player);
        if (playerTeam == null) return;

        droppedFlagEntities.remove(teamId);
        itemEntity.remove();

        if (playerTeam == flagTeam) {
            flagTeam.setFlagAlive(true);
            flagTeam.setFlagCarrierUUID(null);
            placeFlagBlock(flagTeam);

            String msg = config.getMsgFlagReturned()
                    .replace("%team%", ColorUtil.translate(flagTeam.getDisplayName()));
            if (!ColorUtil.isEmpty(msg)) {
                Bukkit.broadcastMessage(config.format(msg));
            }
            refreshScoreboard();
            return;
        }

        ItemStack flagItem = FlagUtil.createFlagItem(flagTeam);
        player.getInventory().addItem(flagItem);
        flagTeam.setFlagCarrierUUID(player.getUniqueId());

        if (config.isGlowEnabled()) {
            int glowIndex = flagTeam == team1 ? 1 : 2;
            scoreboardManager.addToGlowTeam(player, glowIndex);
        }

        String msg = config.getMsgFlagPickedUp()
                .replace("%player%", player.getName())
                .replace("%team%", ColorUtil.translate(flagTeam.getDisplayName()));
        if (!ColorUtil.isEmpty(msg)) {
            Bukkit.broadcastMessage(config.format(msg));
        }

        refreshScoreboard();
    }

    public void handlePlayerMove(Player player, Location from, Location to) {
        if (!area.isInside(to)) {
            pushBackToLastValid(player);
            String msg = config.getMsgOutsideArea();
            if (!ColorUtil.isEmpty(msg)) {
                player.sendActionBar(ColorUtil.component(config.formatRaw(msg)));
            }
            return;
        }

        lastValidPosition.put(player.getUniqueId(), to.clone());

        TeamData playerTeam = getTeam(player);
        if (playerTeam == null) return;

        TeamData enemyTeam = playerTeam == team1 ? team2 : team1;

        boolean isCarrier = enemyTeam.isFlagBeingCarried()
                && player.getUniqueId().equals(enemyTeam.getFlagCarrierUUID());
        boolean hasInInventory = FlagUtil.hasFlag(player, enemyTeam);

        if (isCarrier && hasInInventory
                && playerTeam.getDeliveryZone().isComplete()
                && playerTeam.getDeliveryZone().isInside(to)) {
            handleFlagCapture(player, enemyTeam, playerTeam);
        }
    }

    private void handleFlagCapture(Player player, TeamData capturedFlagTeam, TeamData capturingTeam) {
        capturedFlagTeam.setFlagCarrierUUID(null);
        capturedFlagTeam.setFlagAlive(false);

        FlagUtil.removeFlagFromInventory(player, capturedFlagTeam);

        if (config.isGlowEnabled()) {
            scoreboardManager.removeFromGlowTeams(player);
        }

        String msg = config.getMsgFlagCaptured()
                .replace("%player%", player.getName())
                .replace("%team%", ColorUtil.translate(capturedFlagTeam.getDisplayName()));
        if (!ColorUtil.isEmpty(msg)) {
            Bukkit.broadcastMessage(config.format(msg));
        }

        endEvent(capturingTeam);
    }

    public void handlePlayerDeath(Player player) {
        TeamData team = getTeam(player);
        if (team == null) return;

        TeamData enemyTeam = team == team1 ? team2 : team1;

        if (enemyTeam.isFlagBeingCarried() && player.getUniqueId().equals(enemyTeam.getFlagCarrierUUID())) {
            enemyTeam.setFlagCarrierUUID(null);

            if (config.isGlowEnabled()) {
                scoreboardManager.removeFromGlowTeams(player);
            }

            FlagUtil.removeFlagFromInventory(player, enemyTeam);

            Item existing = droppedFlagEntities.remove(enemyTeam.getId());
            if (existing != null && !existing.isDead()) existing.remove();

            enemyTeam.setFlagAlive(true);
            placeFlagBlock(enemyTeam);

            String returnMsg = config.getMsgFlagReturned()
                    .replace("%team%", ColorUtil.translate(enemyTeam.getDisplayName()));
            if (!ColorUtil.isEmpty(returnMsg)) {
                Bukkit.broadcastMessage(config.format(returnMsg));
            }

            refreshScoreboard();
        }

        boolean canRevive = state == EventState.RUNNING && team.isFlagAlive();
        pendingRespawn.add(player.getUniqueId());
        reviveEligibility.put(player.getUniqueId(), canRevive);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && player.isDead()) {
                player.spigot().respawn();
            }
        }, 20L);
    }

    public void handlePostRespawn(Player player) {
        Boolean canRevive = reviveEligibility.remove(player.getUniqueId());
        if (canRevive == null) return;
        if (state == EventState.WAITING || state == EventState.ENDING) return;

        TeamData team = getTeam(player);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            player.setGameMode(GameMode.SPECTATOR);

            if (canRevive && team != null && team.isFlagAlive() && state == EventState.RUNNING) {
                scheduleRevive(player, team);
            } else {
                permanentSpectators.add(player.getUniqueId());
                refreshScoreboard();
                checkTeamElimination();
            }
        }, 1L);
    }

    private void scheduleRevive(Player player, TeamData team) {
        BukkitTask existing = reviveTasks.remove(player.getUniqueId());
        if (existing != null) existing.cancel();

        int reviveTime = config.getReviveTime();
        int[] counter = {reviveTime};
        BukkitTask[] holder = new BukkitTask[1];

        holder[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || state == EventState.WAITING || state == EventState.ENDING) {
                holder[0].cancel();
                reviveTasks.remove(player.getUniqueId());
                return;
            }
            if (counter[0] <= 0) {
                holder[0].cancel();
                reviveTasks.remove(player.getUniqueId());

                if (state == EventState.RUNNING && team.isFlagAlive()) {
                    player.setGameMode(GameMode.SURVIVAL);
                    player.teleport(team.getSpawn());
                    team.applyKit(player);
                    player.setHealth(maxHealth(player));
                    player.setFoodLevel(20);
                    lastValidPosition.put(player.getUniqueId(), team.getSpawn().clone());
                    showReviveTitle(player);
                } else {
                    permanentSpectators.add(player.getUniqueId());
                    refreshScoreboard();
                    checkTeamElimination();
                }
                return;
            }
            String msg = config.getMsgReviveCountdown().replace("%time%", String.valueOf(counter[0]));
            if (!ColorUtil.isEmpty(msg)) {
                player.sendActionBar(ColorUtil.component(msg));
            }
            counter[0]--;
        }, 0L, 20L);

        reviveTasks.put(player.getUniqueId(), holder[0]);
    }

    private void showReviveTitle(Player player) {
        String titleText = config.getMsgReviveTitle();
        String subtitleText = config.getMsgReviveSubtitle();

        Component titleComp = ColorUtil.isEmpty(titleText)
                ? Component.text("RENASCEU!").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD)
                : ColorUtil.component(titleText);

        Component subtitleComp = ColorUtil.isEmpty(subtitleText)
                ? Component.empty()
                : ColorUtil.component(subtitleText);

        Title t = Title.title(titleComp, subtitleComp,
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(2), Duration.ofMillis(500)));
        player.showTitle(t);
    }

    private void checkTeamElimination() {
        if (state != EventState.RUNNING && state != EventState.DEATH_ZONE) return;

        int alive1 = countAlivePlayers(team1);
        int alive2 = countAlivePlayers(team2);

        if (alive1 == 0 && alive2 == 0) {
            endEvent(null);
        } else if (alive1 == 0) {
            endEvent(team2);
        } else if (alive2 == 0) {
            endEvent(team1);
        }
    }

    public int countAlivePlayers(TeamData team) {
        int count = 0;
        for (UUID uuid : team.getPlayers()) {
            if (!permanentSpectators.contains(uuid) && Bukkit.getPlayer(uuid) != null) {
                count++;
            }
        }
        return count;
    }

    public void handlePlayerQuit(Player player) {
        if (!isInEvent(player)) return;

        BukkitTask task = reviveTasks.remove(player.getUniqueId());
        if (task != null) task.cancel();

        pendingRespawn.remove(player.getUniqueId());
        reviveEligibility.remove(player.getUniqueId());
        permanentSpectators.remove(player.getUniqueId());

        TeamData playerTeam = getTeam(player);
        TeamData enemyTeam = (playerTeam == team1) ? team2 : (playerTeam == team2 ? team1 : null);

        if (enemyTeam != null && enemyTeam.isFlagBeingCarried()
                && player.getUniqueId().equals(enemyTeam.getFlagCarrierUUID())) {
            enemyTeam.setFlagCarrierUUID(null);
            if (config.isGlowEnabled()) {
                scoreboardManager.removeFromGlowTeams(player);
            }

            Item dropped = droppedFlagEntities.remove(enemyTeam.getId());
            if (dropped != null && !dropped.isDead()) dropped.remove();

            enemyTeam.setFlagAlive(true);
            placeFlagBlock(enemyTeam);

            String returnMsg = config.getMsgFlagReturned()
                    .replace("%team%", ColorUtil.translate(enemyTeam.getDisplayName()));
            if (!ColorUtil.isEmpty(returnMsg)) {
                Bukkit.broadcastMessage(config.format(returnMsg));
            }
        }

        if (playerTeam != null) playerTeam.removePlayer(player.getUniqueId());

        lastValidPosition.remove(player.getUniqueId());
        teleportCooldown.remove(player.getUniqueId());
        scoreboardManager.hideFromPlayer(player);
        refreshScoreboard();
        checkTeamElimination();
    }

    public void pushBackToLastValid(Player player) {
        UUID uuid = player.getUniqueId();
        if (teleportCooldown.contains(uuid)) return;
        teleportCooldown.add(uuid);

        Location safe = lastValidPosition.getOrDefault(uuid, null);
        TeamData team = getTeam(player);
        if (safe == null && team != null) safe = team.getSpawn();
        if (safe == null) return;

        Location dest = safe.clone();
        dest.setYaw(player.getLocation().getYaw());
        dest.setPitch(player.getLocation().getPitch());
        player.teleport(dest);

        Bukkit.getScheduler().runTaskLater(plugin, () -> teleportCooldown.remove(uuid), 10L);
    }

    private void placeFlagBlock(TeamData team) {
        if (team.getFlagLocation() == null || team.getFlagMaterial() == null) return;
        Location loc = team.getFlagLocation();
        if (loc.getWorld() == null) return;
        if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
            loc.getWorld().loadChunk(loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
        }
        loc.getBlock().setType(team.getFlagMaterial());
    }

    private void refreshScoreboard() {
        scoreboardManager.update(
                state, remainingSeconds,
                team1, countAlivePlayers(team1), team1.isFlagBeingCarried(),
                team2, countAlivePlayers(team2), team2.isFlagBeingCarried()
        );
    }

    private void broadcastTitle(Title title) {
        for (UUID uuid : getAllEventPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.showTitle(title);
        }
    }

    private boolean isFlagLocation(TeamData team, Location loc) {
        if (team.getFlagLocation() == null) return false;
        Location flag = team.getFlagLocation();
        return flag.getWorld() != null
                && flag.getWorld().equals(loc.getWorld())
                && flag.getBlockX() == loc.getBlockX()
                && flag.getBlockY() == loc.getBlockY()
                && flag.getBlockZ() == loc.getBlockZ();
    }

    private String getDroppedFlagTeamId(Item item) {
        for (Map.Entry<String, Item> entry : droppedFlagEntities.entrySet()) {
            if (entry.getValue().getEntityId() == item.getEntityId()) return entry.getKey();
        }
        return null;
    }

    public boolean isFlagBlock(Location loc) {
        return isFlagLocation(team1, loc) || isFlagLocation(team2, loc);
    }

    public TeamData getFlagOwner(Location loc) {
        if (isFlagLocation(team1, loc)) return team1;
        if (isFlagLocation(team2, loc)) return team2;
        return null;
    }

    public boolean isDroppedFlagEntity(Item item) {
        return getDroppedFlagTeamId(item) != null;
    }

    public boolean isInEvent(Player player) {
        return team1.hasPlayer(player) || team2.hasPlayer(player);
    }

    public TeamData getTeam(Player player) {
        if (team1.hasPlayer(player)) return team1;
        if (team2.hasPlayer(player)) return team2;
        return null;
    }

    public boolean isPendingRespawn(Player player) {
        return pendingRespawn.contains(player.getUniqueId());
    }

    public void removePendingRespawn(Player player) {
        pendingRespawn.remove(player.getUniqueId());
    }

    private Set<UUID> getAllEventPlayers() {
        Set<UUID> all = new HashSet<>();
        all.addAll(team1.getPlayers());
        all.addAll(team2.getPlayers());
        return all;
    }

    private double maxHealth(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        return attr != null ? attr.getValue() : 20.0;
    }

    public EventState getState() { return state; }
    public TeamData getTeam1() { return team1; }
    public TeamData getTeam2() { return team2; }
    public EventArea getArea() { return area; }
    public int getRemainingSeconds() { return remainingSeconds; }
}
