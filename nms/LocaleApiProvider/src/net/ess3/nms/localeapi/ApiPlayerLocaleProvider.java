package net.ess3.nms.localeapi;

import net.ess3.nms.PlayerLocaleProvider;
import org.bukkit.entity.Player;

public class ApiPlayerLocaleProvider extends PlayerLocaleProvider {
    @Override
    public String getLocale(Player player) {
        return player.getLocale();
    }

    @Override
    public boolean tryProvider() {
        try {
            Player.class.getMethod("getLocale");
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    @Override
    public String getHumanName() {
        return "player locale api provider";
    }
}
