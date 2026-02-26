package br.com.m4trixdev.data;

import br.com.m4trixdev.Main;
import br.com.m4trixdev.model.EventArea;
import br.com.m4trixdev.model.TeamData;
import br.com.m4trixdev.util.LocationUtil;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class DataManager {

    private final Main plugin;
    private File dataFile;
    private FileConfiguration data;

    public DataManager(Main plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Nao foi possivel criar data.yml", e);
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void save() {
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao salvar data.yml", e);
        }
    }

    public void saveArea(EventArea area) {
        if (area.getPos1() != null) LocationUtil.save(data, "area.pos1", area.getPos1());
        if (area.getPos2() != null) LocationUtil.save(data, "area.pos2", area.getPos2());
        save();
    }

    public void loadArea(EventArea area) {
        if (data.contains("area.pos1")) area.setPos1(LocationUtil.load(data, "area.pos1"));
        if (data.contains("area.pos2")) area.setPos2(LocationUtil.load(data, "area.pos2"));
    }

    public void saveTeam(TeamData team) {
        String base = "teams." + team.getId();

        if (team.getSpawn() != null) LocationUtil.save(data, base + ".spawn", team.getSpawn());
        if (team.getFlagLocation() != null) LocationUtil.save(data, base + ".flag.location", team.getFlagLocation());
        if (team.getFlagMaterial() != null) data.set(base + ".flag.material", team.getFlagMaterial().name());

        if (team.getKit() != null) {
            for (int i = 0; i < team.getKit().length; i++) {
                data.set(base + ".kit." + i, team.getKit()[i]);
            }
        }

        EventArea dz = team.getDeliveryZone();
        if (dz.getPos1() != null) LocationUtil.save(data, base + ".delivery.pos1", dz.getPos1());
        if (dz.getPos2() != null) LocationUtil.save(data, base + ".delivery.pos2", dz.getPos2());

        save();
    }

    public void loadTeam(TeamData team) {
        String base = "teams." + team.getId();

        if (data.contains(base + ".spawn")) team.setSpawn(LocationUtil.load(data, base + ".spawn"));
        if (data.contains(base + ".flag.location")) team.setFlagLocation(LocationUtil.load(data, base + ".flag.location"));

        String matName = data.getString(base + ".flag.material");
        if (matName != null) {
            try {
                team.setFlagMaterial(Material.valueOf(matName));
            } catch (IllegalArgumentException ignored) {}
        }

        if (data.contains(base + ".kit")) {
            ItemStack[] kit = new ItemStack[41];
            for (int i = 0; i < 41; i++) {
                kit[i] = data.getItemStack(base + ".kit." + i);
            }
            team.setKit(kit);
        }

        EventArea dz = team.getDeliveryZone();
        if (data.contains(base + ".delivery.pos1")) dz.setPos1(LocationUtil.load(data, base + ".delivery.pos1"));
        if (data.contains(base + ".delivery.pos2")) dz.setPos2(LocationUtil.load(data, base + ".delivery.pos2"));
    }
}
