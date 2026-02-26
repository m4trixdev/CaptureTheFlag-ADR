package br.com.m4trixdev.util;

import br.com.m4trixdev.model.TeamData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class FlagUtil {

    public static final NamespacedKey FLAG_KEY = new NamespacedKey("captureflag", "flag_team_id");

    private static final LegacyComponentSerializer SERIALIZER =
            LegacyComponentSerializer.builder()
                    .character('&')
                    .hexColors()
                    .build();

    private FlagUtil() {}

    public static ItemStack createFlagItem(TeamData team) {
        ItemStack item = new ItemStack(team.getFlagMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        Component displayName = SERIALIZER.deserialize(team.getDisplayName() + " &r&7- Bandeira");
        meta.displayName(displayName);

        List<Component> lore = new ArrayList<>();
        lore.add(SERIALIZER.deserialize("&7Entregue na zona do seu time para vencer!"));
        meta.lore(lore);

        meta.getPersistentDataContainer().set(FLAG_KEY, PersistentDataType.STRING, team.getId());
        item.setItemMeta(meta);
        return item;
    }

    public static String getFlagTeamId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(FLAG_KEY, PersistentDataType.STRING);
    }

    public static boolean isFlag(ItemStack item) {
        return getFlagTeamId(item) != null;
    }

    public static boolean hasFlag(org.bukkit.entity.Player player, TeamData team) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            if (team.getId().equals(getFlagTeamId(item))) return true;
        }
        return false;
    }

    public static void removeFlagFromInventory(org.bukkit.entity.Player player, TeamData team) {
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] == null) continue;
            if (team.getId().equals(getFlagTeamId(contents[i]))) {
                inv.setItem(i, null);
            }
        }
    }
}
