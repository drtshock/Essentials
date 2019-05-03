package net.ess3.nms;

import net.ess3.providers.Provider;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public abstract class PlayerLocaleProvider implements Provider {
    public abstract String getLocale(Player player);
}
