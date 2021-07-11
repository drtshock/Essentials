package com.earth2me.essentials;

import org.bukkit.command.Command;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AlternativeCommandsHandler {
    private static final Logger LOGGER = Logger.getLogger("Essentials");
    private final transient Map<String, List<Command>> altcommands = new HashMap<>();
    private final transient Map<String, String> disabledList = new HashMap<>();
    private final transient IEssentials ess;

    public AlternativeCommandsHandler(final IEssentials ess) {
        this.ess = ess;
        for (final Plugin plugin : ess.getServer().getPluginManager().getPlugins()) {
            if (plugin.isEnabled()) {
                addPlugin(plugin);
            }
        }
    }

    public final void addPlugin(final Plugin plugin) {
        if (plugin.getDescription().getMain().contains("com.earth2me.essentials") || plugin.getDescription().getMain().contains("net.essentialsx")) {
            return;
        }
        for (final Map.Entry<String, Command> entry : getPluginCommands(plugin).entrySet()) {
            final String commandName = entry.getKey().contains(":") ? entry.getKey().split(":")[1] : entry.getKey();
            final Command command = entry.getValue();

            final List<Command> pluginCommands = altcommands.computeIfAbsent(commandName.toLowerCase(Locale.ENGLISH), k -> new ArrayList<>());
            boolean found = false;
            for (final Command pc2 : pluginCommands) {
                // Safe cast, everything that's added comes from getPluginCommands which already performs the cast check.
                if (((PluginIdentifiableCommand) pc2).getPlugin().equals(plugin)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                pluginCommands.add(command);
            }
        }
    }

    private Map<String, Command> getPluginCommands(Plugin plugin) {
        final Map<String, Command> commands = new HashMap<>();
        for (final Map.Entry<String, Command> entry : ess.getKnownCommandsProvider().getKnownCommands().entrySet()) {
            if (entry.getValue() instanceof PluginIdentifiableCommand && ((PluginIdentifiableCommand) entry.getValue()).getPlugin().equals(plugin)) {
                commands.put(entry.getKey(), entry.getValue());
            }
        }
        return commands;
    }

    public void removePlugin(final Plugin plugin) {
        final Iterator<Map.Entry<String, List<Command>>> iterator = altcommands.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<String, List<Command>> entry = iterator.next();
            entry.getValue().removeIf(pc -> !(pc instanceof PluginIdentifiableCommand) || ((PluginIdentifiableCommand) pc).getPlugin().equals(plugin));
            if (entry.getValue().isEmpty()) {
                iterator.remove();
            }
        }
    }

    public Command getAlternative(final String label) {
        final List<Command> commands = altcommands.get(label);
        if (commands == null || commands.isEmpty()) {
            return null;
        }
        if (commands.size() == 1) {
            return commands.get(0);
        }
        // return the first command that is not an alias
        for (final Command command : commands) {
            if (command.getName().equalsIgnoreCase(label)) {
                return command;
            }
        }
        // return the first alias
        return commands.get(0);
    }

    public void executed(final String label, final Command pc) {
        if (pc instanceof PluginIdentifiableCommand) {
            final String altString = ((PluginIdentifiableCommand) pc).getPlugin().getName() + ":" + pc.getName();
            if (ess.getSettings().isDebug()) {
                LOGGER.log(Level.INFO, "Essentials: Alternative command " + label + " found, using " + altString);
            }
            disabledList.put(label, altString);
        }
    }

    public Map<String, String> disabledCommands() {
        return disabledList;
    }
}
