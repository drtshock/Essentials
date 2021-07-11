package com.earth2me.essentials;

import net.ess3.api.IUser;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class CommandFilters implements IConf {

    private final IEssentials essentials;
    private final EssentialsConf config;
    private ConfigurationSection filters;
    private Map<CommandFilter.Type, List<CommandFilter>> commandFilters;

    public CommandFilters(final IEssentials essentials) {
        this.essentials = essentials;
        config = new EssentialsConf(new File(essentials.getDataFolder(), "command-filters.yml"));
        config.setTemplateName("/command-filters.yml");

        reloadConfig();
    }

    @Override
    public void reloadConfig() {
        config.load();
        filters = _getCommandFilterSection();
        commandFilters = _getCommandFilters();
    }

    private ConfigurationSection _getCommandFilterSection() {
        if (config.isConfigurationSection("filters")) {
            final ConfigurationSection section = config.getConfigurationSection("filters");
            final ConfigurationSection newSection = new MemoryConfiguration();
            for (final String filterItem : section.getKeys(false)) {
                if (section.isConfigurationSection(filterItem)) {
                    newSection.set(filterItem.toLowerCase(Locale.ENGLISH), section.getConfigurationSection(filterItem));
                }
            }
            return newSection;
        }
        return null;
    }

    private Map<CommandFilter.Type, List<CommandFilter>> _getCommandFilters() {
        final Map<CommandFilter.Type, List<CommandFilter>> commandFilters = new EnumMap<>(CommandFilter.Type.class);
        for (final String name : filters.getKeys(false)) {
            if (!filters.isConfigurationSection(name)) {
                EssentialsConf.LOGGER.warning("Invalid command filter '" + name + "'");
                continue;
            }

            final ConfigurationSection section = Objects.requireNonNull(filters.getConfigurationSection(name));
            final Pattern pattern = section.isString("pattern") ? compileRegex(section.getString("pattern")) : null;
            final String command = section.getString("command");

            if (pattern == null && command == null) {
                EssentialsConf.LOGGER.warning("Invalid command filter '" + name + "', filter must either define 'pattern' or 'command'!");
                continue;
            }

            if (pattern != null && command != null) {
                EssentialsConf.LOGGER.warning("Invalid command filter '" + name + "', filter can't have both 'pattern' and 'command'!");
                continue;
            }

            Integer cooldown = section.getInt("cooldown", -1);
            if (cooldown < 0) {
                cooldown = null;
            } else {
                cooldown *= 1000; // Convert to milliseconds
            }

            final boolean persistentCooldown = section.getBoolean("persistent-cooldown", true);
            final BigDecimal cost = EssentialsConf.toBigDecimal(section.getString("cost"), null);

            final String lowerName = name.toLowerCase(Locale.ENGLISH);

            if (pattern == null) {
                commandFilters.computeIfAbsent(CommandFilter.Type.ESS, k -> new ArrayList<>()).add(new EssCommandFilter(lowerName, command, compileRegex(command), cooldown, persistentCooldown, cost));
            } else {
                commandFilters.computeIfAbsent(CommandFilter.Type.REGEX, k -> new ArrayList<>()).add(new RegexCommandFilter(lowerName, pattern, cooldown, persistentCooldown, cost));
            }
        }
        config.save();
        return commandFilters;
    }

    private Pattern compileRegex(String regex) {
        if (regex.startsWith("^")) {
            try {
                return Pattern.compile(regex.substring(1));
            } catch (final PatternSyntaxException e) {
                essentials.getLogger().warning("Command cooldown error: " + e.getMessage());
                return null;
            }
        } else {
            // Escape above Regex
            if (regex.startsWith("\\^")) {
                regex = regex.substring(1);
            }
            final String cmd = regex.replaceAll("\\*", ".*"); // Wildcards are accepted as asterisk * as known universally.
            return Pattern.compile(cmd + "( .*)?"); // This matches arguments, if present, to "ignore" them from the feature.
        }
    }

    public EssentialsConf getConfig() {
        return config;
    }

    public CommandFilter getCommandCooldown(final IUser user, final String label, CommandFilter.Type type) {
        if (user.isAuthorized("essentials.commandcooldowns.bypass")) return null;
        return getFilter(label, type, filter -> filter.hasCooldown() && !user.isAuthorized("essentials.commandcooldowns.bypass." + filter.getName()));
    }

    public CommandFilter getCommandCost(final IUser user, final String label, CommandFilter.Type type) {
        if (user.isAuthorized("essentials.nocommandcost.all")) return null;
        return getFilter(label, type, filter -> filter.hasCost() && !user.isAuthorized("essentials.nocommandcost." + filter.getName()));
    }

    private CommandFilter getFilter(final String label, CommandFilter.Type type, Predicate<CommandFilter> filterPredicate) {
        for (CommandFilter filter : commandFilters.get(type)) {
            if (!filterPredicate.test(filter)) continue;

            final boolean matches = filter.getPattern().matcher(label).matches();
            if (essentials.getSettings().isDebug()) {
                essentials.getLogger().info(String.format("Checking command '%s' against filter '%s': %s", label, filter.getName(), matches));
            }

            if (matches) {
                return filter;
            }
        }
        return null;
    }
}
