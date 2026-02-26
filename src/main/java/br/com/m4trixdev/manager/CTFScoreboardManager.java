package br.com.m4trixdev.manager;

import br.com.m4trixdev.config.ConfigManager;
import br.com.m4trixdev.model.EventState;
import br.com.m4trixdev.model.TeamData;
import br.com.m4trixdev.util.ColorUtil;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CTFScoreboardManager {

    private final ConfigManager config;
    private Scoreboard board;
    private Objective objective;
    private Team glowTeam1;
    private Team glowTeam2;

    public CTFScoreboardManager(ConfigManager config) {
        this.config = config;
    }

    public void setup() {
        board = Bukkit.getScoreboardManager().getNewScoreboard();

        objective = board.registerNewObjective(
                "ctf_main",
                Criteria.DUMMY,
                LegacyComponentSerializer.legacyAmpersand().deserialize(config.getScoreboardTitle())
        );
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        glowTeam1 = board.registerNewTeam("ctf_t1");
        glowTeam1.color(NamedTextColor.RED);

        glowTeam2 = board.registerNewTeam("ctf_t2");
        glowTeam2.color(NamedTextColor.BLUE);
    }

    public void update(EventState state, int remainingSeconds,
                       TeamData t1, int alive1, boolean flag1Carried,
                       TeamData t2, int alive2, boolean flag2Carried) {

        new HashSet<>(board.getEntries()).forEach(board::resetScores);

        List<String> lines = config.getScoreboardLines();
        if (lines.isEmpty()) return;

        String timeDisplay;
        switch (state) {
            case DEATH_ZONE -> timeDisplay = ColorUtil.translate(config.getScoreboardDeathZoneLabel());
            case STARTING   -> timeDisplay = ColorUtil.translate(config.getScoreboardStartingLabel());
            case WAITING, ENDING -> timeDisplay = "--:--";
            default -> {
                int mm = remainingSeconds / 60;
                int ss = remainingSeconds % 60;
                timeDisplay = String.format("%02d:%02d", mm, ss);
            }
        }

        String stateDisplay;
        switch (state) {
            case STARTING   -> stateDisplay = ColorUtil.translate(config.getScoreboardStartingLabel());
            case RUNNING    -> stateDisplay = ColorUtil.translate("&aRODANDO");
            case DEATH_ZONE -> stateDisplay = ColorUtil.translate(config.getScoreboardDeathZoneLabel());
            case ENDING     -> stateDisplay = ColorUtil.translate("&7ENCERRANDO");
            default         -> stateDisplay = ColorUtil.translate("&7AGUARDANDO");
        }

        String flag1Status = resolveFlagStatus(t1, flag1Carried);
        String flag2Status = resolveFlagStatus(t2, flag2Carried);

        List<String> resolved = new ArrayList<>();
        for (String line : lines) {
            String out = line
                    .replace("%state%", stateDisplay)
                    .replace("%time%", timeDisplay)
                    .replace("%team1%", ColorUtil.translate(t1.getDisplayName()))
                    .replace("%team2%", ColorUtil.translate(t2.getDisplayName()))
                    .replace("%alive1%", String.valueOf(alive1))
                    .replace("%alive2%", String.valueOf(alive2))
                    .replace("%flag1%", flag1Status)
                    .replace("%flag2%", flag2Status);
            resolved.add(ColorUtil.translate(out));
        }

        Set<String> seen = new HashSet<>();
        List<String> unique = new ArrayList<>();
        for (String entry : resolved) {
            String candidate = entry;
            int pad = 0;
            while (seen.contains(candidate)) {
                StringBuilder sb = new StringBuilder(entry);
                for (int i = 0; i <= pad; i++) sb.append(ChatColor.RESET);
                pad++;
                candidate = sb.toString();
            }
            seen.add(candidate);
            unique.add(candidate);
        }

        int score = unique.size();
        for (String entry : unique) {
            objective.getScore(entry).setScore(score--);
        }
    }

    private String resolveFlagStatus(TeamData team, boolean carried) {
        if (!team.isFlagAlive()) {
            return carried
                    ? ColorUtil.translate(config.getScoreboardFlagCarried())
                    : ColorUtil.translate(config.getScoreboardFlagDead());
        }
        return ColorUtil.translate(config.getScoreboardFlagAlive());
    }

    public void addToGlowTeam(Player player, int teamIndex) {
        if (teamIndex == 1) {
            glowTeam1.addPlayer(player);
        } else {
            glowTeam2.addPlayer(player);
        }
        player.setGlowing(true);
    }

    public void removeFromGlowTeams(Player player) {
        glowTeam1.removePlayer(player);
        glowTeam2.removePlayer(player);
        player.setGlowing(false);
    }

    public void showToPlayer(Player player) {
        player.setScoreboard(board);
    }

    public void hideFromPlayer(Player player) {
        removeFromGlowTeams(player);
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }
}
