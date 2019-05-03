package com.earth2me.essentials.commands;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.PlayerList;
import com.earth2me.essentials.IEssentialsModule;
import com.earth2me.essentials.Trade;
import com.earth2me.essentials.User;
import com.earth2me.essentials.utils.FormatUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.ess3.api.IEssentials;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;


public abstract class EssentialsCommand implements IEssentialsCommand {
    private final transient String name;
    protected transient IEssentials ess;
    protected transient IEssentialsModule module;
    protected static final Logger logger = Logger.getLogger("Essentials");

    protected EssentialsCommand(final String name) {
        this.name = name;
    }

    @Override
    public void setEssentials(final IEssentials ess) {
        this.ess = ess;
    }

    @Override
    public void setEssentialsModule(final IEssentialsModule module) {
        this.module = module;
    }

    @Override
    public String getName() {
        return name;
    }

    // Get online players - only show vanished if source has permission
    protected User getPlayer(final Server server, final CommandSource sender, final String[] args, final int pos) throws PlayerNotFoundException, NotEnoughArgumentsException {
        if (sender.isPlayer()) {
            User user = ess.getUser(sender.getPlayer());
            return getPlayer(server, user, args, pos);
        }
        return getPlayer(server, args, pos, true, false);
    }

    // Get online players - only show vanished if source has permission
    protected User getPlayer(final Server server, final CommandSource sender, final String searchTerm) throws PlayerNotFoundException, NotEnoughArgumentsException {
        if (sender.isPlayer()) {
            User user = ess.getUser(sender.getPlayer());
            return getPlayer(server, user, searchTerm, user.canInteractVanished(), false);
        }
        return getPlayer(server, searchTerm, true, false);
    }

    // Get online players - only show vanished if source has permission
    protected User getPlayer(final Server server, final User user, final String[] args, final int pos) throws PlayerNotFoundException, NotEnoughArgumentsException {
        return getPlayer(server, user, args, pos, user.canInteractVanished(), false);
    }

    // Get online or offline players, this method allows for raw access
    protected User getPlayer(final Server server, final String[] args, final int pos, boolean getHidden, final boolean getOffline) throws PlayerNotFoundException, NotEnoughArgumentsException {
        return getPlayer(server, null, args, pos, getHidden, getOffline);
    }

    private User getPlayer(final Server server, final User sourceUser, final String[] args, final int pos, boolean getHidden, final boolean getOffline) throws PlayerNotFoundException, NotEnoughArgumentsException {
        if (args.length <= pos) {
            throw new NotEnoughArgumentsException();
        }
        if (args[pos].isEmpty()) {
            throw new PlayerNotFoundException();
        }
        return getPlayer(server, sourceUser, args[pos], getHidden, getOffline);
    }

    // Get online or offline players, this method allows for raw access
    protected User getPlayer(final Server server, final String searchTerm, boolean getHidden, final boolean getOffline) throws PlayerNotFoundException {
        return getPlayer(server, null, searchTerm, getHidden, getOffline);
    }

    private User getPlayer(final Server server, final User sourceUser, final String searchTerm, boolean getHidden, final boolean getOffline) throws PlayerNotFoundException {
        final User user;
        Player exPlayer;

        try {
            exPlayer = server.getPlayer(UUID.fromString(searchTerm));
        } catch (IllegalArgumentException ex) {
            if (getOffline) {
                exPlayer = server.getPlayerExact(searchTerm);
            } else {
                exPlayer = server.getPlayer(searchTerm);
            }
        }

        if (exPlayer != null) {
            user = ess.getUser(exPlayer);
        } else {
            user = ess.getUser(searchTerm);
        }

        if (user != null) {
            if (!getOffline && !user.getBase().isOnline()) {
                throw new PlayerNotFoundException();
            }

            if (getHidden || canInteractWith(sourceUser, user)) {
                return user;
            }
            throw new PlayerNotFoundException();
        }
        final List<Player> matches = server.matchPlayer(searchTerm);

        if (matches.isEmpty()) {
            final String matchText = searchTerm.toLowerCase(Locale.ENGLISH);
            for (User userMatch : ess.getOnlineUsers()) {
                if (getHidden || canInteractWith(sourceUser, userMatch)) {
                    final String displayName = FormatUtil.stripFormat(userMatch.getDisplayName()).toLowerCase(Locale.ENGLISH);
                    if (displayName.contains(matchText)) {
                        return userMatch;
                    }
                }
            }
        } else {
            for (Player player : matches) {
                final User userMatch = ess.getUser(player);
                if (userMatch.getDisplayName().startsWith(searchTerm) && (getHidden || canInteractWith(sourceUser, userMatch))) {
                    return userMatch;
                }
            }
            final User userMatch = ess.getUser(matches.get(0));
            if (getHidden || canInteractWith(sourceUser, userMatch)) {
                return userMatch;
            }
        }
        throw new PlayerNotFoundException();
    }

    @Override
    public final void run(final Server server, final User user, final String commandLabel, final Command cmd, final String[] args) throws Exception {
        final Trade charge = new Trade(this.getName(), ess);
        charge.isAffordableFor(user);
        run(server, user, commandLabel, args);
        charge.charge(user);
    }

    protected void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        run(server, user.getSource(), commandLabel, args);
    }

    @Override
    public final void run(final Server server, final CommandSource sender, final String commandLabel, final Command cmd, final String[] args) throws Exception {
        run(server, sender, commandLabel, args);
    }

    protected void run(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        throw new Exception(sender.tl("onlyPlayers", commandLabel));
    }

    @Override
    public final List<String> tabComplete(final Server server, final User user, final String commandLabel, final Command cmd, final String[] args) {
        if (args.length == 0) {
            // Shouldn't happen, but bail out early if it does so that args[0] can always be used
            return Collections.emptyList();
        }
        List<String> options = getTabCompleteOptions(server, user, commandLabel, args);
        if (options == null) {
            return null;
        }
        return StringUtil.copyPartialMatches(args[args.length - 1], options, Lists.<String>newArrayList());
    }

    // Doesn't need to do any starts-with checks
    protected List<String> getTabCompleteOptions(final Server server, final User user, final String commandLabel, final String[] args) {
        return getTabCompleteOptions(server, user.getSource(), commandLabel, args);
    }

    @Override
    public final List<String> tabComplete(final Server server, final CommandSource sender, final String commandLabel, final Command cmd, final String[] args) {
        if (args.length == 0) {
            // Shouldn't happen, but bail out early if it does so that args[0] can always be used
            return Collections.emptyList();
        }
        List<String> options = getTabCompleteOptions(server, sender, commandLabel, args);
        if (options == null) {
            return null;
        }
        return StringUtil.copyPartialMatches(args[args.length - 1], options, Lists.<String>newArrayList());
    }

    // Doesn't need to do any starts-with checks
    protected List<String> getTabCompleteOptions(final Server server, final CommandSource sender, final String commandLabel, final String[] args) {
        // No tab completion results
        return getPlayers(server, sender);
    }

    public static String getFinalArg(final String[] args, final int start) {
        final StringBuilder bldr = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i != start) {
                bldr.append(" ");
            }
            bldr.append(args[i]);
        }
        return bldr.toString();
    }

    boolean canInteractWith(CommandSource interactor, User interactee) {
        if (interactor == null) {
            return !interactee.isHidden();
        }

        if (interactor.isPlayer()) {
            return canInteractWith(ess.getUser(interactor.getPlayer()), interactee);
        }

        return true; // console
    }

    private static boolean canInteractWith(User interactor, User interactee) {
        if (interactor == null) {
            return !interactee.isHidden();
        }

        if (interactor.equals(interactee)) {
            return true;
        }

        return interactor.getBase().canSee(interactee.getBase());
    }

    /**
     * Gets a list of all player names that can be seen with by the given CommandSource,
     * for tab completion.
     */
    protected List<String> getPlayers(final Server server, final CommandSource interactor) {
        List<String> players = Lists.newArrayList();
        for (User user : ess.getOnlineUsers()) {
            if (canInteractWith(interactor, user)) {
                players.add(user.getName());
            }
        }
        return players;
    }

    /**
     * Gets a list of all player names that can be seen with by the given User,
     * for tab completion.
     */
    protected List<String> getPlayers(final Server server, final User interactor) {
        List<String> players = Lists.newArrayList();
        for (User user : ess.getOnlineUsers()) {
            if (canInteractWith(interactor, user)) {
                players.add(user.getName());
            }
        }
        return players;
    }

    /**
     * Returns a list of all online groups.
     */
    protected List<String> getGroups() {
        // TODO: A better way to do this
        return new ArrayList<>(PlayerList.getPlayerLists(ess, null, true).keySet());
    }

    /**
     * Gets a list of tab-completable items that start with the given name.
     * Due to the number of items, this may not return the entire list.
     */
    protected List<String> getItems() {
        return new ArrayList<>(ess.getItemDb().listNames());
    }

    /**
     * Gets a list of tab-completable items usable for "getMatching".
     */
    protected List<String> getMatchingItems(String arg) {
        List<String> items = Lists.newArrayList("hand", "inventory", "blocks");
        if (!arg.isEmpty()) {
            // Emphasize the other items if they haven't entered anything yet.
            items.addAll(getItems());
        }
        return items;
    }

    /**
     * Lists all commands.
     *
     * TODO: Use the real commandmap to do this automatically.
     */
    protected final List<String> getCommands(Server server) {
        List<String> commands = Lists.newArrayList();
        for (Plugin p : server.getPluginManager().getPlugins()) {
            final PluginDescriptionFile desc = p.getDescription();
            final Map<String, Map<String, Object>> cmds = desc.getCommands();
            if (cmds != null) {
                commands.addAll(cmds.keySet());
            }
        }
        return commands;
    }

    /**
     * Attempts to tab-complete a command or its arguments.
     */
    protected final List<String> tabCompleteCommand(CommandSource sender, Server server, String label, String[] args, int index) {
        // TODO: Pass this to the real commandmap
        Command command = server.getPluginCommand(label);
        if (command == null) {
            return Collections.emptyList();
        }

        int numArgs = args.length - index - 1;
        ess.getLogger().info(numArgs + " " + index + " " + Arrays.toString(args));
        String[] effectiveArgs = new String[numArgs];
        for (int i = 0; i < numArgs; i++) {
            effectiveArgs[i] = args[i + index];
        }
        if (effectiveArgs.length == 0) {
            effectiveArgs = new String[] { "" };
        }
        ess.getLogger().info(command + " -- " + Arrays.toString(effectiveArgs));

        return command.tabComplete(sender.getSender(), label, effectiveArgs);
    }

    /**
     * Common time durations (in seconds), for use in tab completion.
     */
    protected static final List<String> COMMON_DURATIONS = ImmutableList.of("1", "60", "600", "3600", "86400");
    /**
     * Common date diffs, for use in tab completion
     */
    protected static final List<String> COMMON_DATE_DIFFS = ImmutableList.of("1m", "15m", "1h", "3h", "12h", "1d", "1w", "1mo", "1y");
}
