package br.com.m4trixdev.model;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TeamData {

    private final String id;
    private String displayName;
    private Location spawn;
    private Location flagLocation;
    private Material flagMaterial;
    private ItemStack[] kit;
    private final Set<UUID> players = new HashSet<>();
    private boolean flagAlive = true;
    private UUID flagCarrierUUID = null;
    private final EventArea deliveryZone = new EventArea();

    public TeamData(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public void captureKit(Player player) {
        kit = new ItemStack[41];
        PlayerInventory inv = player.getInventory();

        ItemStack[] storage = inv.getStorageContents();
        for (int i = 0; i < 36 && i < storage.length; i++) {
            kit[i] = storage[i] != null ? storage[i].clone() : null;
        }

        ItemStack[] armor = inv.getArmorContents();
        for (int i = 0; i < 4 && i < armor.length; i++) {
            kit[36 + i] = armor[i] != null ? armor[i].clone() : null;
        }

        ItemStack offhand = inv.getItemInOffHand();
        kit[40] = offhand.getType() != Material.AIR ? offhand.clone() : null;
    }

    public void applyKit(Player player) {
        player.getInventory().clear();
        if (kit == null) return;

        PlayerInventory inv = player.getInventory();

        for (int i = 0; i < 36; i++) {
            if (kit[i] != null) inv.setItem(i, kit[i].clone());
        }

        ItemStack[] armor = new ItemStack[4];
        for (int i = 0; i < 4; i++) {
            armor[i] = kit[36 + i] != null ? kit[36 + i].clone() : null;
        }
        inv.setArmorContents(armor);

        if (kit[40] != null) inv.setItemInOffHand(kit[40].clone());
    }

    public boolean hasPlayer(UUID uuid) { return players.contains(uuid); }
    public boolean hasPlayer(Player player) { return players.contains(player.getUniqueId()); }

    public void addPlayer(UUID uuid) { players.add(uuid); }
    public void removePlayer(UUID uuid) { players.remove(uuid); }
    public Set<UUID> getPlayers() { return players; }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String name) { this.displayName = name; }

    public Location getSpawn() { return spawn; }
    public void setSpawn(Location spawn) { this.spawn = spawn != null ? spawn.clone() : null; }

    public Location getFlagLocation() { return flagLocation; }
    public void setFlagLocation(Location loc) { this.flagLocation = loc != null ? loc.clone() : null; }

    public Material getFlagMaterial() { return flagMaterial; }
    public void setFlagMaterial(Material mat) { this.flagMaterial = mat; }

    public ItemStack[] getKit() { return kit; }
    public void setKit(ItemStack[] kit) { this.kit = kit; }

    public boolean isFlagAlive() { return flagAlive; }
    public void setFlagAlive(boolean alive) { this.flagAlive = alive; }

    public UUID getFlagCarrierUUID() { return flagCarrierUUID; }
    public void setFlagCarrierUUID(UUID uuid) { this.flagCarrierUUID = uuid; }
    public boolean isFlagBeingCarried() { return flagCarrierUUID != null; }

    public EventArea getDeliveryZone() { return deliveryZone; }

    public boolean isReady() {
        return spawn != null && flagLocation != null && flagMaterial != null && kit != null;
    }
}
