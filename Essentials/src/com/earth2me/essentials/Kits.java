package com.earth2me.essentials;

import com.earth2me.essentials.utils.NumberUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.earth2me.essentials.I18n.capitalCase;
import static com.earth2me.essentials.I18n.tl;

public class Kits implements IConf {

    private final EssentialsConf config;
    private ConfigurationSection kits;

    public Kits(final IEssentials essentials) {
        config = new EssentialsConf(new File(essentials.getDataFolder(), "kits.yml"));
        config.setTemplateName("/kits.yml");

        reloadConfig();
    }

    @Override
    public void reloadConfig() {
        config.load();
        kits = _getKits();
    }

    private ConfigurationSection _getKits() {
        if (config.isConfigurationSection("kits")) {
            final ConfigurationSection section = config.getConfigurationSection("kits");
            final ConfigurationSection newSection = new MemoryConfiguration();
            for (String kitItem : section.getKeys(false)) {
                if (section.isConfigurationSection(kitItem)) {
                    newSection.set(kitItem.toLowerCase(Locale.ENGLISH), section.getConfigurationSection(kitItem));
                }
            }
            return newSection;
        }
        return null;
    }

    public EssentialsConf getConfig() {
        return config;
    }

    public ConfigurationSection getKits() {
        return kits;
    }

    public Map<String, Object> getKit(String name) {
        name = name.replace('.', '_').replace('/', '_');
        if (getKits() != null) {
            final ConfigurationSection kits = getKits();
            // For some reason, YAML doesn't sees keys as always lowercase even if they aren't defined like that.
            // Workaround is to toLowercase when getting from the config, but showing normally elsewhere.
            // ODDLY ENOUGH when you get the configuration section for ALL kits, it will return the proper
            // case of each kit. But when you check for each kit's configuration section, it won't return the kit
            // you just found if you don't toLowercase it.
            if (kits.isConfigurationSection(name.toLowerCase())) {
                return kits.getConfigurationSection(name.toLowerCase()).getValues(true);
            } else {
            }
        }

        return null;
    }

    public void addKit(String name, List<String> lines, long delay) {
        // Will overwrite but w/e
        config.set("kits." + name + ".delay", delay);
        config.set("kits." + name + ".items", lines);
        kits = _getKits();
        config.save();
    }

    public String listKits(final net.ess3.api.IEssentials ess, final User user) throws Exception {
        try {
            final ConfigurationSection kits = config.getConfigurationSection("kits");
            final StringBuilder list = new StringBuilder();
            for (String kitItem : kits.getKeys(false)) {
                if (user == null) {
                    list.append(" ").append(capitalCase(kitItem));
                } else if (user.isAuthorized("essentials.kits." + kitItem.toLowerCase(Locale.ENGLISH))) {
                    String cost = "";
                    String name = capitalCase(kitItem);
                    BigDecimal costPrice = new Trade("kit-" + kitItem.toLowerCase(Locale.ENGLISH), ess).getCommandCost(user);
                    if (costPrice.signum() > 0) {
                        cost = user.tl( "kitCost", NumberUtil.displayCurrency(costPrice, ess));
                    }

                    Kit kit = new Kit(kitItem, ess);
                    double nextUse = kit.getNextUse(user);
                    if (nextUse == -1 && ess.getSettings().isSkippingUsedOneTimeKitsFromKitList()) {
                        continue;
                    } else if (nextUse != 0) {
                        name = user.tl( "kitDelay", name);
                    }

                    list.append(" ").append(name).append(cost);
                }
            }
            return list.toString().trim();
        } catch (Exception ex) {
            throw new Exception(user.tl( "kitError"), ex);
        }

    }
}
