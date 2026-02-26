package br.com.m4trixdev.config;

import br.com.m4trixdev.Main;
import br.com.m4trixdev.util.ColorUtil;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.logging.Level;

public class ConfigManager {

    private final Main plugin;

    private int duration;
    private int reviveTime;
    private int minPlayers;
    private int maxPlayers;
    private boolean deathZoneEnabled;
    private boolean glowEnabled;

    private String prefix;
    private String team1Name;
    private String team2Name;

    private String scoreboardTitle;
    private String scoreboardFlagAlive;
    private String scoreboardFlagDead;
    private String scoreboardFlagCarried;
    private String scoreboardDeathZoneLabel;
    private String scoreboardStartingLabel;
    private List<String> scoreboardLines;

    private String msgEventStart;
    private String msgEventStop;
    private String msgFlagPickedUp;
    private String msgFlagDropped;
    private String msgFlagReturned;
    private String msgFlagCaptured;
    private String msgTeamWin;
    private String msgReviveCountdown;
    private String msgReviveTitle;
    private String msgReviveSubtitle;
    private String msgDeathZoneStart;
    private String msgOutsideArea;
    private String msgCountdown;

    public ConfigManager(Main plugin) {
        this.plugin = plugin;
    }

    public boolean load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        try {
            duration = config.getInt("event.duration", 35);
            reviveTime = config.getInt("event.revive-time", 5);
            minPlayers = config.getInt("event.min-players", 2);
            maxPlayers = config.getInt("event.max-players", 100);
            deathZoneEnabled = config.getBoolean("death-zone.enabled", true);
            glowEnabled = config.getBoolean("glow.enabled", true);

            prefix = config.getString("messages.prefix", "&6[Evento]&r ");
            team1Name = config.getString("teams.team1.name", "&cTime 1");
            team2Name = config.getString("teams.team2.name", "&9Time 2");

            scoreboardTitle = config.getString("scoreboard.title", "&b&lCapture a Bandeira");
            scoreboardFlagAlive = config.getString("scoreboard.flag-alive", "&a[VIVA]");
            scoreboardFlagDead = config.getString("scoreboard.flag-dead", "&c[DESTRUIDA]");
            scoreboardFlagCarried = config.getString("scoreboard.flag-carried", "&e[ROUBADA]");
            scoreboardDeathZoneLabel = config.getString("scoreboard.death-zone-label", "&c&lDEATH ZONE");
            scoreboardStartingLabel = config.getString("scoreboard.starting-label", "&eINICIANDO...");
            scoreboardLines = config.getStringList("scoreboard.lines");

            msgEventStart = config.getString("messages.event-start", "&aO evento comecou!");
            msgEventStop = config.getString("messages.event-stop", "&cO evento foi encerrado.");
            msgFlagPickedUp = config.getString("messages.flag-picked-up", "%player% &cpegou a bandeira do %team%&c!");
            msgFlagDropped = config.getString("messages.flag-dropped", "&eA bandeira do %team% &efoi dropada no chao!");
            msgFlagReturned = config.getString("messages.flag-returned", "&aA bandeira do %team% &afoi retornada ao lugar!");
            msgFlagCaptured = config.getString("messages.flag-captured", "%player% &acapturou a bandeira do %team%&a!");
            msgTeamWin = config.getString("messages.team-win", "%team% &avenceu o evento!");
            msgReviveCountdown = config.getString("messages.revive-countdown", "&eRenascendo em &f%time%s");
            msgReviveTitle = config.getString("messages.revive-title", "&aRENASCEU!");
            msgReviveSubtitle = config.getString("messages.revive-subtitle", "&7Bem-vindo de volta");
            msgDeathZoneStart = config.getString("messages.death-zone-start", "&cDeath Zone ativado! Sobreviva!");
            msgOutsideArea = config.getString("messages.outside-area", "&cVoce nao pode sair da area do evento!");
            msgCountdown = config.getString("messages.countdown", "&a%time%");

            return true;

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao carregar config.yml. Restaurando padrao.", e);
            plugin.saveResource("config.yml", true);
            return false;
        }
    }

    public String format(String message) {
        if (ColorUtil.isEmpty(message)) return "";
        return ColorUtil.translate(prefix + message);
    }

    public String formatRaw(String message) {
        if (ColorUtil.isEmpty(message)) return "";
        return ColorUtil.translate(message);
    }

    public int getDuration() { return duration; }
    public int getReviveTime() { return reviveTime; }
    public int getMinPlayers() { return minPlayers; }
    public int getMaxPlayers() { return maxPlayers; }
    public boolean isDeathZoneEnabled() { return deathZoneEnabled; }
    public boolean isGlowEnabled() { return glowEnabled; }
    public String getTeam1Name() { return team1Name; }
    public String getTeam2Name() { return team2Name; }
    public String getScoreboardTitle() { return scoreboardTitle; }
    public String getScoreboardFlagAlive() { return scoreboardFlagAlive; }
    public String getScoreboardFlagDead() { return scoreboardFlagDead; }
    public String getScoreboardFlagCarried() { return scoreboardFlagCarried; }
    public String getScoreboardDeathZoneLabel() { return scoreboardDeathZoneLabel; }
    public String getScoreboardStartingLabel() { return scoreboardStartingLabel; }
    public List<String> getScoreboardLines() { return scoreboardLines; }
    public String getMsgEventStart() { return msgEventStart; }
    public String getMsgEventStop() { return msgEventStop; }
    public String getMsgFlagPickedUp() { return msgFlagPickedUp; }
    public String getMsgFlagDropped() { return msgFlagDropped; }
    public String getMsgFlagReturned() { return msgFlagReturned; }
    public String getMsgFlagCaptured() { return msgFlagCaptured; }
    public String getMsgTeamWin() { return msgTeamWin; }
    public String getMsgReviveCountdown() { return msgReviveCountdown; }
    public String getMsgReviveTitle() { return msgReviveTitle; }
    public String getMsgReviveSubtitle() { return msgReviveSubtitle; }
    public String getMsgDeathZoneStart() { return msgDeathZoneStart; }
    public String getMsgOutsideArea() { return msgOutsideArea; }
    public String getMsgCountdown() { return msgCountdown; }
}
