package br.com.m4trixdev;

import br.com.m4trixdev.command.EventoCommand;
import br.com.m4trixdev.config.ConfigManager;
import br.com.m4trixdev.data.DataManager;
import br.com.m4trixdev.listener.BlockListener;
import br.com.m4trixdev.listener.PlayerListener;
import br.com.m4trixdev.manager.CTFScoreboardManager;
import br.com.m4trixdev.manager.EventManager;
import br.com.m4trixdev.model.EventState;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private ConfigManager configManager;
    private DataManager dataManager;
    private CTFScoreboardManager scoreboardManager;
    private EventManager eventManager;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        if (!configManager.load()) {
            getLogger().severe("Falha ao carregar config.yml. Plugin desativado.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        dataManager = new DataManager(this);
        dataManager.setup();

        scoreboardManager = new CTFScoreboardManager(configManager);
        scoreboardManager.setup();

        eventManager = new EventManager(this, configManager, dataManager, scoreboardManager);

        EventoCommand eventoCommand = new EventoCommand(this, configManager, eventManager, dataManager);
        getCommand("evento").setExecutor(eventoCommand);
        getCommand("evento").setTabCompleter(eventoCommand);

        getServer().getPluginManager().registerEvents(
                new PlayerListener(this, eventManager, configManager), this);
        getServer().getPluginManager().registerEvents(
                new BlockListener(eventManager, configManager), this);

        getLogger().info("CaptureTheFlag v" + getDescription().getVersion() + " ativado com sucesso.");
    }

    @Override
    public void onDisable() {
        if (eventManager != null && eventManager.getState() != EventState.WAITING) {
            eventManager.stopEvent();
        }
        getLogger().info("CaptureTheFlag desativado.");
    }

    public ConfigManager getConfigManager() { return configManager; }
    public DataManager getDataManager() { return dataManager; }
    public EventManager getEventManager() { return eventManager; }
    public CTFScoreboardManager getScoreboardManager() { return scoreboardManager; }
}
