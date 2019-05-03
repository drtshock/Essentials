/*
 * Essentials - a bukkit plugin
 * Copyright (C) 2011  Essentials Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.earth2me.essentials;

import com.earth2me.essentials.commands.*;
import com.earth2me.essentials.items.AbstractItemDb;
import com.earth2me.essentials.items.FlatItemDb;
import com.earth2me.essentials.items.LegacyItemDb;
import com.earth2me.essentials.metrics.Metrics;
import com.earth2me.essentials.perm.PermissionsHandler;
import com.earth2me.essentials.register.payment.Methods;
import com.earth2me.essentials.signs.SignBlockListener;
import com.earth2me.essentials.signs.SignEntityListener;
import com.earth2me.essentials.signs.SignPlayerListener;
import com.earth2me.essentials.textreader.IText;
import com.earth2me.essentials.textreader.KeywordReplacer;
import com.earth2me.essentials.textreader.SimpleTextInput;
import com.earth2me.essentials.utils.DateUtil;
import com.earth2me.essentials.utils.VersionUtil;
import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import net.ess3.api.*;
import net.ess3.api.IEssentials;
import net.ess3.api.ISettings;
import net.ess3.nms.PlayerLocaleProvider;
import net.ess3.nms.PotionMetaProvider;
import net.ess3.nms.SpawnEggProvider;
import net.ess3.nms.SpawnerProvider;
import net.ess3.nms.flattened.FlatSpawnEggProvider;
import net.ess3.nms.legacy.LegacyPotionMetaProvider;
import net.ess3.nms.legacy.LegacySpawnEggProvider;
import net.ess3.nms.legacy.LegacySpawnerProvider;
import net.ess3.nms.localeapi.ApiPlayerLocaleProvider;
import net.ess3.nms.refl.ReflPlayerLocaleProvider;
import net.ess3.nms.refl.ReflSpawnEggProvider;
import net.ess3.nms.updatedmeta.BasePotionDataProvider;
import net.ess3.nms.updatedmeta.BlockMetaSpawnerProvider;
import net.ess3.nms.v1_8_R1.v1_8_R1SpawnerProvider;
import net.ess3.nms.v1_8_R2.v1_8_R2SpawnerProvider;
import net.ess3.providers.ProviderFactory;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.earth2me.essentials.I18n.tl;


public class Essentials extends JavaPlugin implements net.ess3.api.IEssentials {
    private static final Logger LOGGER = Logger.getLogger("Essentials");
    private transient ISettings settings;
    private final transient TNTExplodeListener tntListener = new TNTExplodeListener(this);
    private transient Jails jails;
    private transient Warps warps;
    private transient Worth worth;
    private transient List<IConf> confList;
    private transient Backup backup;
    private transient AbstractItemDb itemDb;
    private transient final Methods paymentMethod = new Methods();
    private transient PermissionsHandler permissionsHandler;
    private transient AlternativeCommandsHandler alternativeCommandsHandler;
    private transient UserMap userMap;
    private transient ExecuteTimer execTimer;
    private transient I18n i18n;
    private transient Metrics metrics;
    private transient EssentialsTimer timer;
    private final transient Set<String> vanishedPlayers = new LinkedHashSet<>();
    private transient Method oldGetOnlinePlayers;
    private transient SpawnerProvider spawnerProvider;
    private transient SpawnEggProvider spawnEggProvider;
    private transient PotionMetaProvider potionMetaProvider;
    private transient PlayerLocaleProvider playerLocaleProvider;
    private transient Kits kits;

    public Essentials() {
    }

    public Essentials(final Server server) {
        super(new JavaPluginLoader(server), new PluginDescriptionFile("Essentials", "", "com.earth2me.essentials.Essentials"), null, null);
    }

    @SuppressWarnings("unused")
    public void forceLoadClasses() {
        try {
            Class.forName(OfflinePlayer.class.getName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ISettings getSettings() {
        return settings;
    }

    public void setupForTesting(final Server server) throws IOException, InvalidDescriptionException {
        final File dataFolder = File.createTempFile("essentialstest", "");
        if (!dataFolder.delete()) {
            throw new IOException();
        }
        if (!dataFolder.mkdir()) {
            throw new IOException();
        }
        i18n = new I18n(this);
        i18n.onEnable();
        i18n.updateLocale("en");
        Console.setInstance(this);
        
        LOGGER.log(Level.INFO, tl("usingTempFolderForTesting"));
        LOGGER.log(Level.INFO, dataFolder.toString());
        settings = new Settings(this);
        userMap = new UserMap(this);
        permissionsHandler = new PermissionsHandler(this, false);
        Economy.setEss(this);
        confList = new ArrayList<>();
        jails = new Jails(this);
        registerListeners(server.getPluginManager());
        kits = new Kits(this);
    }

    @Override
    public void onEnable() {
        try {
            if (LOGGER != this.getLogger()) {
                LOGGER.setParent(this.getLogger());
            }
            execTimer = new ExecuteTimer();
            execTimer.start();
            i18n = new I18n(this);
            i18n.onEnable();
            execTimer.mark("I18n1");
            
            Console.setInstance(this);

            if (!VersionUtil.isServerSupported()) {
                getLogger().severe(tl("serverUnsupported"));
            }

            final PluginManager pm = getServer().getPluginManager();
            for (Plugin plugin : pm.getPlugins()) {
                if (plugin.getDescription().getName().startsWith("Essentials") && !plugin.getDescription().getVersion().equals(this.getDescription().getVersion()) && !plugin.getDescription().getName().equals("EssentialsAntiCheat")) {
                    getLogger().warning(tl("versionMismatch", plugin.getDescription().getName()));
                }
            }

            for (Method method : Server.class.getDeclaredMethods()) {
                if (method.getName().endsWith("getOnlinePlayers") && method.getReturnType() == Player[].class) {
                    oldGetOnlinePlayers = method;
                    break;
                }
            }

            forceLoadClasses();

            try {
                final EssentialsUpgrade upgrade = new EssentialsUpgrade(this);
                upgrade.beforeSettings();
                execTimer.mark("Upgrade");

                confList = new ArrayList<>();
                settings = new Settings(this);
                confList.add(settings);
                execTimer.mark("Settings");

                userMap = new UserMap(this);
                confList.add(userMap);
                execTimer.mark("Init(Usermap)");

                kits = new Kits(this);
                confList.add(kits);
                upgrade.convertKits();
                execTimer.mark("Kits");

                upgrade.afterSettings();
                execTimer.mark("Upgrade2");

                warps = new Warps(getServer(), this.getDataFolder());
                confList.add(warps);
                execTimer.mark("Init(Spawn/Warp)");

                worth = new Worth(this.getDataFolder());
                confList.add(worth);
                itemDb = getItemDbFromConfig();
                confList.add(itemDb);
                execTimer.mark("Init(Worth/ItemDB)");

                jails = new Jails(this);
                confList.add(jails);
                execTimer.mark("Init(Jails)");

                spawnerProvider = new ProviderFactory<>(getLogger(),
                        Arrays.asList(
                                BlockMetaSpawnerProvider.class,
                                v1_8_R2SpawnerProvider.class,
                                v1_8_R1SpawnerProvider.class,
                                LegacySpawnerProvider.class
                        ), "mob spawner").getProvider();
                spawnEggProvider = new ProviderFactory<>(getLogger(),
                        Arrays.asList(
                                FlatSpawnEggProvider.class,
                                ReflSpawnEggProvider.class,
                                LegacySpawnEggProvider.class
                        ), "spawn egg").getProvider();
                potionMetaProvider = new ProviderFactory<>(getLogger(),
                        Arrays.asList(
                                BasePotionDataProvider.class,
                                LegacyPotionMetaProvider.class
                        ), "potion meta").getProvider();
                playerLocaleProvider = new ProviderFactory<>(getLogger(),
                        Arrays.asList(
                                ApiPlayerLocaleProvider.class,
                                ReflPlayerLocaleProvider.class
                        )
                        , "player locale").getProvider();
                execTimer.mark("Init(Providers)");
                reload();

                // The item spawn blacklist is loaded with all other settings, before the item
                // DB, but it depends on the item DB, so we need to reload it again here:
                ((Settings) settings)._lateLoadItemSpawnBlacklist();
            } catch (YAMLException exception) {
                if (pm.getPlugin("EssentialsUpdate") != null) {
                    LOGGER.log(Level.SEVERE, tl("essentialsHelp2"));
                } else {
                    LOGGER.log(Level.SEVERE, tl("essentialsHelp1"));
                }
                handleCrash(exception);
                return;
            }
            backup = new Backup(this);
            permissionsHandler = new PermissionsHandler(this, settings.useBukkitPermissions());
            alternativeCommandsHandler = new AlternativeCommandsHandler(this);

            timer = new EssentialsTimer(this);
            scheduleSyncRepeatingTask(timer, 1000, 50);

            Economy.setEss(this);
            execTimer.mark("RegHandler");

            for (World w : Bukkit.getWorlds())
                addDefaultBackPermissionsToWorld(w);

            metrics = new Metrics(this);
            if (metrics.isEnabled()) {
                getLogger().info("Starting Metrics. Opt-out using the global bStats config.");
            } else {
                getLogger().info("Metrics disabled per bStats config.");
            }

            final String timeroutput = execTimer.end();
            if (getSettings().isDebug()) {
                LOGGER.log(Level.INFO, "Essentials load {0}", timeroutput);
            }
        } catch (NumberFormatException ex) {
            handleCrash(ex);
        } catch (Error ex) {
            handleCrash(ex);
            throw ex;
        }
    }

    @Override
    public void saveConfig() {
        // We don't use any of the bukkit config writing, as this breaks our config file formatting.
    }

    private void registerListeners(PluginManager pm) {
        HandlerList.unregisterAll(this);

        if (getSettings().isDebug()) {
            LOGGER.log(Level.INFO, "Registering Listeners");
        }

        final EssentialsPluginListener pluginListener = new EssentialsPluginListener(this);
        pm.registerEvents(pluginListener, this);
        confList.add(pluginListener);

        final EssentialsPlayerListener playerListener = new EssentialsPlayerListener(this);
        playerListener.registerEvents();

        final EssentialsBlockListener blockListener = new EssentialsBlockListener(this);
        pm.registerEvents(blockListener, this);

        final SignBlockListener signBlockListener = new SignBlockListener(this);
        pm.registerEvents(signBlockListener, this);

        final SignPlayerListener signPlayerListener = new SignPlayerListener(this);
        pm.registerEvents(signPlayerListener, this);

        final SignEntityListener signEntityListener = new SignEntityListener(this);
        pm.registerEvents(signEntityListener, this);

        final EssentialsEntityListener entityListener = new EssentialsEntityListener(this);
        pm.registerEvents(entityListener, this);

        final EssentialsWorldListener worldListener = new EssentialsWorldListener(this);
        pm.registerEvents(worldListener, this);

        final EssentialsServerListener serverListener = new EssentialsServerListener(this);
        pm.registerEvents(serverListener, this);

        pm.registerEvents(tntListener, this);

        jails.resetListener();
    }

    @Override
    public void onDisable() {
        for (User user : getOnlineUsers()) {
            if (user.isVanished()) {
                user.setVanished(false);
                user.sendTl("unvanishedReload");
            }
            user.stopTransaction();
        }
        cleanupOpenInventories();
        if (i18n != null) {
            i18n.onDisable();
        }
        if (backup != null) {
            backup.stopTask();
        }
        Economy.setEss(null);
        Trade.closeLog();
        getUserMap().getUUIDMap().shutdown();

        HandlerList.unregisterAll(this);
    }

    @Override
    public void reload() {
        Trade.closeLog();

        for (IConf iConf : confList) {
            iConf.reloadConfig();
            execTimer.mark("Reload(" + iConf.getClass().getSimpleName() + ")");
        }

        i18n.updateLocale(settings.getLocale());

        final PluginManager pm = getServer().getPluginManager();
        registerListeners(pm);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String commandLabel, String[] args) {
        return onTabCompleteEssentials(sender, command, commandLabel, args, Essentials.class.getClassLoader(),
            "com.earth2me.essentials.commands.Command", "essentials.", null);
    }

    @Override
    public List<String> onTabCompleteEssentials(final CommandSender cSender, final Command command, final String commandLabel, final String[] args,
                                                final ClassLoader classLoader, final String commandPath, final String permissionPrefix,
                                                final IEssentialsModule module) {
        if (!getSettings().isCommandOverridden(command.getName()) && (!commandLabel.startsWith("e") || commandLabel.equalsIgnoreCase(command.getName()))) {
            final PluginCommand pc = alternativeCommandsHandler.getAlternative(commandLabel);
            if (pc != null) {
                try {
                    TabCompleter completer = pc.getTabCompleter();
                    if (completer != null) {
                        return completer.onTabComplete(cSender, command, commandLabel, args);
                    }
                } catch (final Exception ex) {
                    Bukkit.getLogger().log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
        }

        // Note: The tab completer is always a player, even when tab-completing in a command block
        User user = null;
        if (cSender instanceof Player) {
            user = getUser((Player) cSender);
        }

        Locale locale = user != null ? user.getApplicableLocale() : getI18n().getCurrentLocale();

        try {
            CommandSource sender = new CommandSource(cSender);

            // Check for disabled commands
            if (getSettings().isCommandDisabled(commandLabel)) {
                return Collections.emptyList();
            }

            IEssentialsCommand cmd;
            try {
                cmd = (IEssentialsCommand) classLoader.loadClass(commandPath + command.getName()).newInstance();
                cmd.setEssentials(this);
                cmd.setEssentialsModule(module);
            } catch (Exception ex) {
                sender.sendMessage(tl(locale, "commandNotLoaded", commandLabel));
                LOGGER.log(Level.SEVERE, tl(locale, "commandNotLoaded", commandLabel), ex);
                return Collections.emptyList();
            }

            // Check authorization
            if (user != null && !user.isAuthorized(cmd, permissionPrefix)) {
                return Collections.emptyList();
            }

            if (user != null && user.isJailed() && !user.isAuthorized(cmd, "essentials.jail.allow.")) {
                return Collections.emptyList();
            }

            // Run the command
            try {
                if (user == null) {
                    return cmd.tabComplete(getServer(), sender, commandLabel, command, args);
                } else {
                    return cmd.tabComplete(getServer(), user, commandLabel, command, args);
                }
            } catch (Exception ex) {
                showError(sender, ex, commandLabel);
                // Tab completion shouldn't fail
                LOGGER.log(Level.SEVERE, tl(locale, "commandFailed", commandLabel), ex);
                return Collections.emptyList();
            }
        } catch (Throwable ex) {
            LOGGER.log(Level.SEVERE, tl(locale, "commandFailed", commandLabel), ex);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String commandLabel, final String[] args) {
        return onCommandEssentials(sender, command, commandLabel, args, Essentials.class.getClassLoader(), "com.earth2me.essentials.commands.Command", "essentials.", null);
    }

    @Override
    public boolean onCommandEssentials(final CommandSender cSender, final Command command, final String commandLabel, final String[] args, final ClassLoader classLoader, final String commandPath, final String permissionPrefix, final IEssentialsModule module) {
        // Allow plugins to override the command via onCommand
        if (!getSettings().isCommandOverridden(command.getName()) && (!commandLabel.startsWith("e") || commandLabel.equalsIgnoreCase(command.getName()))) {
            final PluginCommand pc = alternativeCommandsHandler.getAlternative(commandLabel);
            if (pc != null) {
                alternativeCommandsHandler.executed(commandLabel, pc);
                try {
                    return pc.execute(cSender, commandLabel, args);
                } catch (final Exception ex) {
                    Bukkit.getLogger().log(Level.SEVERE, ex.getMessage(), ex);
                    cSender.sendMessage(tl("internalError"));
                    return true;
                }
            }
        }

        try {
            Block bSenderBlock = null;
            User user = null;
            CommandSource sender = new CommandSource(cSender);
            if (cSender instanceof Player) {
                user = getUser((Player) cSender);
                sender.setUser(user);
            } else if (cSender instanceof BlockCommandSender) {
                BlockCommandSender bsender = (BlockCommandSender) cSender;
                bSenderBlock = bsender.getBlock();
            }

            if (bSenderBlock != null) {
                Bukkit.getLogger().log(Level.INFO, "CommandBlock at {0},{1},{2} issued server command: /{3} {4}", new Object[]{bSenderBlock.getX(), bSenderBlock.getY(), bSenderBlock.getZ(), commandLabel, EssentialsCommand.getFinalArg(args, 0)});
            } else if (user == null) {
                Bukkit.getLogger().log(Level.INFO, "{0} issued server command: /{1} {2}", new Object[]{cSender.getName(), commandLabel, EssentialsCommand.getFinalArg(args, 0)});
            }

            // New mail notification
            if (user != null && !getSettings().isCommandDisabled("mail") && !command.getName().equals("mail") && user.isAuthorized("essentials.mail")) {
                user.notifyOfMail();
            }

            //Print version even if admin command is not available #easteregg
            if (commandLabel.equalsIgnoreCase("essversion")) {
                sender.sendMessage("This server is running Essentials " + getDescription().getVersion());
                return true;
            }

            // Check for disabled commands
            if (getSettings().isCommandDisabled(commandLabel)) {
                return true;
            }

            IEssentialsCommand cmd;
            try {
                cmd = (IEssentialsCommand) classLoader.loadClass(commandPath + command.getName()).newInstance();
                cmd.setEssentials(this);
                cmd.setEssentialsModule(module);
            } catch (Exception ex) {
                sender.sendTl("commandNotLoaded", commandLabel);
                LOGGER.log(Level.SEVERE, tl("commandNotLoaded", commandLabel), ex);
                return true;
            }

            // Check authorization
            if (user != null && !user.isAuthorized(cmd, permissionPrefix)) {
                LOGGER.log(Level.INFO, tl("deniedAccessCommand", user.getName()));
                sender.sendTl("noAccessCommand");
                return true;
            }

            if (user != null && user.isJailed() && !user.isAuthorized(cmd, "essentials.jail.allow.")) {
                if (user.getJailTimeout() > 0) {
                    sender.sendTl("playerJailedFor", user.getName(), DateUtil.formatDateDiff(sender, user.getJailTimeout()));
                } else {
                    sender.sendTl("jailMessage");
                }
                return true;
            }

            // Run the command
            try {
                if (user == null) {
                    cmd.run(getServer(), sender, commandLabel, command, args);
                } else {
                    cmd.run(getServer(), user, commandLabel, command, args);
                }
                return true;
            } catch (NoChargeException ex) {
                return true;
            } catch (QuietAbortException ex) {
                return true;
            } catch (NotEnoughArgumentsException ex) {
                sender.sendMessage(command.getDescription());
                sender.sendMessage(command.getUsage().replaceAll("<command>", commandLabel));
                if (!ex.getMessage().isEmpty()) {
                    sender.sendMessage(ex.getMessage());
                }
                return true;
            } catch (Exception ex) {
                showError(sender, ex, commandLabel);
                if (settings.isDebug()) {
                    ex.printStackTrace();
                }
                return true;
            }
        } catch (Throwable ex) {
            LOGGER.log(Level.SEVERE, tl("commandFailed", commandLabel), ex);
            return true;
        }
    }

    public void cleanupOpenInventories() {
        for (User user : getOnlineUsers()) {
            if (user.isRecipeSee()) {
                user.getBase().getOpenInventory().getTopInventory().clear();
                user.getBase().getOpenInventory().close();
                user.setRecipeSee(false);
            }
            if (user.isInvSee() || user.isEnderSee()) {
                user.getBase().getOpenInventory().close();
                user.setInvSee(false);
                user.setEnderSee(false);
            }
        }
    }

    @Override
    public void showError(final CommandSource sender, final Throwable exception, final String commandLabel) {
        sender.sendTl("errorWithMessage", exception.getMessage());
        if (getSettings().isDebug()) {
            LOGGER.log(Level.INFO, tl("errorCallingCommand", commandLabel), exception);
        }
    }

    @Override
    public BukkitScheduler getScheduler() {
        return this.getServer().getScheduler();
    }

    @Override
    public IJails getJails() {
        return jails;
    }

    @Override
    public Warps getWarps() {
        return warps;
    }

    @Override
    public Worth getWorth() {
        return worth;
    }

    @Override
    public Backup getBackup() {
        return backup;
    }

    @Override
    public Kits getKits() {
        return kits;
    }

    @Override
    public Metrics getMetrics() {
        return metrics;
    }

    @Override
    public void setMetrics(Metrics metrics) {
        this.metrics = metrics;
    }

    @Deprecated
    @Override
    public User getUser(final Object base) {
        if (base instanceof Player) {
            return getUser((Player) base);
        }
        if (base instanceof org.bukkit.OfflinePlayer) {
            return getUser(((org.bukkit.OfflinePlayer) base).getUniqueId());
        }
        if (base instanceof UUID) {
            return getUser((UUID) base);
        }
        if (base instanceof String) {
            return getOfflineUser((String) base);
        }
        return null;
    }

    //This will return null if there is not a match.
    @Override
    public User getUser(final String base) {
        return getOfflineUser(base);
    }

    //This will return null if there is not a match.
    @Override
    public User getUser(final UUID base) {
        return userMap.getUser(base);
    }

    //This will return null if there is not a match.
    @Override
    public User getOfflineUser(final String name) {
        final User user = userMap.getUser(name);
        if (user != null && user.getBase() instanceof OfflinePlayer) {
            //This code should attempt to use the last known name of a user, if Bukkit returns name as null.
            final String lastName = user.getLastAccountName();
            if (lastName != null) {
                ((OfflinePlayer) user.getBase()).setName(lastName);
            } else {
                ((OfflinePlayer) user.getBase()).setName(name);
            }
        }
        return user;
    }

    //This will create a new user if there is not a match.
    @Override
    public User getUser(final Player base) {
        if (base == null) {
            return null;
        }

        if (userMap == null) {
            LOGGER.log(Level.WARNING, "Essentials userMap not initialized");
            return null;
        }

        User user = userMap.getUser(base.getUniqueId());

        if (user == null) {
            if (getSettings().isDebug()) {
                LOGGER.log(Level.INFO, "Constructing new userfile from base player {0}", base.getName());
            }
            user = new User(base, this);
        } else {
            user.update(base);
        }
        return user;
    }

    private void handleCrash(Throwable exception) {
        final PluginManager pm = getServer().getPluginManager();
        LOGGER.log(Level.SEVERE, exception.toString());
        exception.printStackTrace();
        pm.registerEvents(new Listener() {
            @EventHandler(priority = EventPriority.LOW)
            public void onPlayerJoin(final PlayerJoinEvent event) {
                event.getPlayer().sendMessage("Essentials failed to load, read the log file.");
            }
        }, this);
        for (Player player : getOnlinePlayers()) {
            player.sendMessage("Essentials failed to load, read the log file.");
        }
        this.setEnabled(false);
    }

    @Override
    public World getWorld(final String name) {
        if (name.matches("[0-9]+")) {
            final int worldId = Integer.parseInt(name);
            if (worldId < getServer().getWorlds().size()) {
                return getServer().getWorlds().get(worldId);
            }
        }
        return getServer().getWorld(name);
    }

    @Override
    public void addReloadListener(final IConf listener) {
        confList.add(listener);
    }

    @Override
    public Methods getPaymentMethod() {
        return paymentMethod;
    }

    @Override
    public int broadcastMessage(final String message) {
        return broadcastMessage(null, null, message, true);
    }

    @Override
    public int broadcastMessage(final IUser sender, final String message) {
        return broadcastMessage(sender, null, message, false);
    }

    @Override
    public int broadcastMessage(final String permission, final String message) {
        return broadcastMessage(null, permission, message, false);
    }

    private int broadcastMessage(final IUser sender, final String permission, final String message, final boolean keywords) {
        if (sender != null && sender.isHidden()) {
            return 0;
        }

        IText broadcast = new SimpleTextInput(message);

        final Collection<Player> players = getOnlinePlayers();

        for (Player player : players) {
            sendBroadcast(sender != null ? sender.getSource() : null, permission, keywords, getUser(player), broadcast);
        }

        return players.size();
    }

    @Override
    public int broadcastTl(final String string, final Object... objects) {
        return broadcastTl(null, null, string, true, objects);
    }

    @Override
    public int broadcastTl(final IUser sender, final String string, final Object... objects) {
        return broadcastTl(sender.getSource(), null, string, false, objects);
    }

    @Override
    public int broadcastTl(final CommandSource sender, final String string, final Object... objects) {
        return broadcastTl(sender, null, string, false, objects);
    }

    @Override
    public int broadcastTl(final String permission, final String string, final Object... objects) {
        return broadcastTl(null, permission, string, false, objects);
    }

    private int broadcastTl(final CommandSource sender, final String permission, final String string, final boolean keywords, final Object... objects) {
        if (sender != null && sender.getUser() != null && sender.getUser().isHidden()) {
            return 0;
        }


        final Collection<Player> players = getOnlinePlayers();

        for (Player player : players) {
            final User user = getUser(player);
            sendBroadcast(sender, permission, keywords, user, new SimpleTextInput(user.tl(string, objects)));
        }

        return players.size();
    }

    private void sendBroadcast(final CommandSource sender, final String permission, final boolean keywords, final User user, IText broadcast) {
        if ((permission == null && (sender == null || sender.getUser() == null || !user.isIgnoredPlayer(sender.getUser()))) || (permission != null && user.isAuthorized(permission))) {
            if (keywords) {
                broadcast = new KeywordReplacer(broadcast, user.getSource(), this, false);
            }
            for (String messageText : broadcast.getLines()) {
                user.sendMessage(messageText);
            }
        }
    }

    @Override
    public BukkitTask runTaskAsynchronously(final Runnable run) {
        return this.getScheduler().runTaskAsynchronously(this, run);
    }

    @Override
    public BukkitTask runTaskLaterAsynchronously(final Runnable run, final long delay) {
        return this.getScheduler().runTaskLaterAsynchronously(this, run, delay);
    }

    @Override
    public BukkitTask runTaskTimerAsynchronously(final Runnable run, final long delay, final long period) {
        return this.getScheduler().runTaskTimerAsynchronously(this, run, delay, period);
    }

    @Override
    public int scheduleSyncDelayedTask(final Runnable run) {
        return this.getScheduler().scheduleSyncDelayedTask(this, run);
    }

    @Override
    public int scheduleSyncDelayedTask(final Runnable run, final long delay) {
        return this.getScheduler().scheduleSyncDelayedTask(this, run, delay);
    }

    @Override
    public int scheduleSyncRepeatingTask(final Runnable run, final long delay, final long period) {
        return this.getScheduler().scheduleSyncRepeatingTask(this, run, delay, period);
    }

    @Override
    public TNTExplodeListener getTNTListener() {
        return tntListener;
    }

    @Override
    public PermissionsHandler getPermissionsHandler() {
        return permissionsHandler;
    }

    @Override
    public AlternativeCommandsHandler getAlternativeCommandsHandler() {
        return alternativeCommandsHandler;
    }

    @Override
    public IItemDb getItemDb() {
        return itemDb;
    }

    @Override
    public UserMap getUserMap() {
        return userMap;
    }

    @Override
    public I18n getI18n() {
        return i18n;
    }

    @Override
    public EssentialsTimer getTimer() {
        return timer;
    }

    @Override
    public List<String> getVanishedPlayers() {
        return Collections.unmodifiableList(new ArrayList<>(vanishedPlayers));
    }

    @Override
    public Collection<String> getVanishedPlayersNew() {
        return vanishedPlayers;
    }

    @Override
    public Collection<Player> getOnlinePlayers() {
        try {
            return (Collection<Player>) getServer().getOnlinePlayers(); // Needed for sanity here, the Bukkit API is a bit broken in the sense it only allows subclasses of Player to this list
        } catch (NoSuchMethodError ex) {
            try {
                return Arrays.asList((Player[]) oldGetOnlinePlayers.invoke(getServer()));
            } catch (InvocationTargetException ex1) {
                throw Throwables.propagate(ex.getCause());
            } catch (IllegalAccessException ex1) {
                throw new RuntimeException("Error invoking oldGetOnlinePlayers", ex1);
            }
        }
    }

    @Override
    public Iterable<User> getOnlineUsers() {
        return Iterables.transform(getOnlinePlayers(), new Function<Player, User>() {

            @Override
            public User apply(Player player) {
                return getUser(player);
            }
        });
    }

    @Override
    public SpawnerProvider getSpawnerProvider() {
        return spawnerProvider;
    }

    @Override
    public SpawnEggProvider getSpawnEggProvider() {
        return spawnEggProvider;
    }

    @Override
    public PotionMetaProvider getPotionMetaProvider() {
        return potionMetaProvider;
    }

    @Override
    public PlayerLocaleProvider getPlayerLocaleProvider() {
        return playerLocaleProvider;
    }

    private static void addDefaultBackPermissionsToWorld(World w) {
        String permName = "essentials.back.into." + w.getName();

        Permission p = Bukkit.getPluginManager().getPermission(permName);
        if (p == null) {
            p = new Permission(permName,
                    "Allows access to /back when the destination location is within world " + w.getName(),
                    PermissionDefault.TRUE);
            Bukkit.getPluginManager().addPermission(p);
        }
    }

    private static class EssentialsWorldListener implements Listener, Runnable {
        private transient final IEssentials ess;

        public EssentialsWorldListener(final IEssentials ess) {
            this.ess = ess;
        }

        @EventHandler(priority = EventPriority.LOW)
        public void onWorldLoad(final WorldLoadEvent event) {
            addDefaultBackPermissionsToWorld(event.getWorld());

            ess.getJails().onReload();
            ess.getWarps().reloadConfig();
            for (IConf iConf : ((Essentials) ess).confList) {
                if (iConf instanceof IEssentialsModule) {
                    iConf.reloadConfig();
                }
            }
        }

        @EventHandler(priority = EventPriority.LOW)
        public void onWorldUnload(final WorldUnloadEvent event) {
            ess.getJails().onReload();
            ess.getWarps().reloadConfig();
            for (IConf iConf : ((Essentials) ess).confList) {
                if (iConf instanceof IEssentialsModule) {
                    iConf.reloadConfig();
                }
            }
        }

        @Override
        public void run() {
            ess.reload();
        }
    }

    private AbstractItemDb getItemDbFromConfig() {
        final String setting = settings.getItemDbType();

        if (setting.equalsIgnoreCase("json")) {
            return new FlatItemDb(this);
        } else if (setting.equalsIgnoreCase("csv")) {
            return new LegacyItemDb(this);
        } else {
            VersionUtil.BukkitVersion version = VersionUtil.getServerBukkitVersion();

            if (version.isHigherThanOrEqualTo(VersionUtil.v1_13_0_R01)) {
                return new FlatItemDb(this);
            } else {
                return new LegacyItemDb(this);
            }
        }
    }
}
