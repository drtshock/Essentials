package com.earth2me.essentials;

import com.earth2me.essentials.commands.Commandfireball;
import com.earth2me.essentials.textreader.IText;
import com.earth2me.essentials.textreader.KeywordReplacer;
import com.earth2me.essentials.textreader.TextInput;
import com.earth2me.essentials.textreader.TextPager;
import com.earth2me.essentials.utils.DateUtil;
import com.earth2me.essentials.utils.FormatUtil;
import com.earth2me.essentials.utils.LocationUtil;
import com.earth2me.essentials.utils.MaterialUtil;
import com.earth2me.essentials.utils.VersionUtil;
import io.papermc.lib.PaperLib;
import net.ess3.api.IEssentials;
import net.ess3.api.events.AfkStatusChangeEvent;
import net.essentialsx.api.v2.events.AsyncUserDataLoadEvent;
import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.FormattedCommandAlias;
import org.bukkit.command.PluginCommand;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static com.earth2me.essentials.I18n.tl;

public class EssentialsPlayerListener implements Listener {
    private static final Logger LOGGER = Logger.getLogger("Essentials");
    private final transient IEssentials ess;

    public EssentialsPlayerListener(final IEssentials parent) {
        this.ess = parent;
    }

    private static boolean isEntityPickupEvent() {
        try {
            Class.forName("org.bukkit.event.entity.EntityPickupItemEvent");
            return true;
        } catch (final ClassNotFoundException ignored) {
            return false;
        }
    }

    private static boolean isCommandSendEvent() {
        try {
            Class.forName("org.bukkit.event.player.PlayerCommandSendEvent");
            return true;
        } catch (final ClassNotFoundException ignored) {
            return false;
        }
    }

    private static boolean isArrowPickupEvent() {
        try {
            Class.forName("org.bukkit.event.player.PlayerPickupArrowEvent");
            return true;
        } catch (final ClassNotFoundException ignored) {
            return false;
        }
    }

    public void registerEvents() {
        ess.getServer().getPluginManager().registerEvents(this, ess);

        if (isArrowPickupEvent()) {
            ess.getServer().getPluginManager().registerEvents(new ArrowPickupListener(), ess);
        }

        if (isEntityPickupEvent()) {
            ess.getServer().getPluginManager().registerEvents(new PickupListener1_12(), ess);
        } else {
            ess.getServer().getPluginManager().registerEvents(new PickupListenerPre1_12(), ess);
        }

        if (isCommandSendEvent()) {
            ess.getServer().getPluginManager().registerEvents(new CommandSendListener(), ess);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerRespawn(final PlayerRespawnEvent event) {
        final User user = ess.getUser(event.getPlayer());
        updateCompass(user);
        user.setDisplayNick();

        if (ess.getSettings().isTeleportInvulnerability()) {
            user.enableInvulnerabilityAfterTeleport();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(final AsyncPlayerChatEvent event) {
        final User user = ess.getUser(event.getPlayer());
        if (user.isMuted()) {
            event.setCancelled(true);

            final String dateDiff = user.getMuteTimeout() > 0 ? DateUtil.formatDateDiff(user.getMuteTimeout()) : null;
            if (dateDiff == null) {
                user.sendMessage(user.hasMuteReason() ? tl("voiceSilencedReason", user.getMuteReason()) : tl("voiceSilenced"));
            } else {
                user.sendMessage(user.hasMuteReason() ? tl("voiceSilencedReasonTime", dateDiff, user.getMuteReason()) : tl("voiceSilencedTime", dateDiff));
            }

            LOGGER.info(tl("mutedUserSpeaks", user.getName(), event.getMessage()));
        }
        try {
            final Iterator<Player> it = event.getRecipients().iterator();
            while (it.hasNext()) {
                final User u = ess.getUser(it.next());
                if (u.isIgnoredPlayer(user)) {
                    it.remove();
                }
            }
        } catch (final UnsupportedOperationException ex) {
            if (ess.getSettings().isDebug()) {
                ess.getLogger().log(Level.INFO, "Ignore could not block chat due to custom chat plugin event.", ex);
            } else {
                ess.getLogger().info("Ignore could not block chat due to custom chat plugin event.");
            }
        }

        user.updateActivityOnChat(true);
        user.setDisplayNick();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(final PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() && event.getFrom().getBlockZ() == event.getTo().getBlockZ() && event.getFrom().getBlockY() == event.getTo().getBlockY()) {
            return;
        }

        final User user = ess.getUser(event.getPlayer());

        if (user.isFreeze()) {
            final Location from = event.getFrom();
            final Location to = event.getTo().clone();
            to.setX(from.getX());
            to.setY(from.getY());
            to.setZ(from.getZ());
            try {
                event.setTo(LocationUtil.getSafeDestination(to));
            } catch (final Exception ex) {
                event.setTo(to);
            }
            return;
        }

        if (!ess.getSettings().cancelAfkOnMove() && !ess.getSettings().getFreezeAfkPlayers()) {
            return;
        }

        if (user.isAfk() && ess.getSettings().getFreezeAfkPlayers()) {
            final Location from = event.getFrom();
            final Location origTo = event.getTo();
            final Location to = origTo.clone();
            if (origTo.getY() >= from.getBlockY() + 1) {
                user.updateActivityOnMove(true);
                return;
            }
            to.setX(from.getX());
            to.setY(from.getY());
            to.setZ(from.getZ());
            try {
                event.setTo(LocationUtil.getSafeDestination(to));
            } catch (final Exception ex) {
                event.setTo(to);
            }
            return;
        }
        final Location afk = user.getAfkPosition();
        if (afk == null || !event.getTo().getWorld().equals(afk.getWorld()) || afk.distanceSquared(event.getTo()) > 9) {
            user.updateActivityOnMove(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        final User user = ess.getUser(event.getPlayer());

        if (hideJoinQuitMessages() || (ess.getSettings().allowSilentJoinQuit() && user.isAuthorized("essentials.silentquit"))) {
            event.setQuitMessage(null);
        } else if (ess.getSettings().isCustomQuitMessage() && event.getQuitMessage() != null) {
            final Player player = event.getPlayer();
            final String msg = ess.getSettings().getCustomQuitMessage()
                .replace("{PLAYER}", player.getDisplayName())
                .replace("{USERNAME}", player.getName())
                .replace("{ONLINE}", NumberFormat.getInstance().format(ess.getOnlinePlayers().size()))
                .replace("{UPTIME}", DateUtil.formatDateDiff(ManagementFactory.getRuntimeMXBean().getStartTime()))
                .replace("{PREFIX}", FormatUtil.replaceFormat(ess.getPermissionsHandler().getPrefix(player)))
                .replace("{SUFFIX}", FormatUtil.replaceFormat(ess.getPermissionsHandler().getSuffix(player)));

            event.setQuitMessage(msg.isEmpty() ? null : msg);
        }

        user.startTransaction();
        if (ess.getSettings().removeGodOnDisconnect() && user.isGodModeEnabled()) {
            user.setGodModeEnabled(false);
        }
        if (user.isVanished()) {
            user.setVanished(false);
        }
        user.setLogoutLocation();
        if (user.isRecipeSee()) {
            user.getBase().getOpenInventory().getTopInventory().clear();
        }

        final ArrayList<HumanEntity> viewers = new ArrayList<>(user.getBase().getInventory().getViewers());
        for (final HumanEntity viewer : viewers) {
            if (viewer instanceof Player) {
                final User uviewer = ess.getUser((Player) viewer);
                if (uviewer.isInvSee()) {
                    uviewer.getBase().closeInventory();
                }
            }
        }

        user.updateActivity(false, AfkStatusChangeEvent.Cause.QUIT);
        if (!user.isHidden()) {
            user.setLastLogout(System.currentTimeMillis());
        }
        user.stopTransaction();

        user.dispose();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final String joinMessage = event.getJoinMessage();
        ess.runTaskAsynchronously(() -> delayedJoin(event.getPlayer(), joinMessage));

        if (hideJoinQuitMessages() || ess.getSettings().allowSilentJoinQuit() || ess.getSettings().isCustomJoinMessage()) {
            event.setJoinMessage(null);
        }
    }

    private boolean hideJoinQuitMessages() {
        return ess.getSettings().hasJoinQuitMessagePlayerCount() && ess.getServer().getOnlinePlayers().size() > ess.getSettings().getJoinQuitMessagePlayerCount();
    }

    public void delayedJoin(final Player player, final String message) {
        if (!player.isOnline()) {
            return;
        }

        ess.getBackup().onPlayerJoin();
        final User dUser = ess.getUser(player);

        dUser.startTransaction();
        if (dUser.isNPC()) {
            dUser.setNPC(false);
        }

        final long currentTime = System.currentTimeMillis();
        dUser.checkMuteTimeout(currentTime);
        dUser.updateActivity(false, AfkStatusChangeEvent.Cause.JOIN);
        dUser.stopTransaction();

        class DelayJoinTask implements Runnable {
            @Override
            public void run() {
                final User user = ess.getUser(player);

                if (!user.getBase().isOnline()) {
                    return;
                }

                user.startTransaction();

                final String lastAccountName = user.getLastAccountName(); // For comparison
                user.setLastAccountName(user.getBase().getName());
                user.setLastLogin(currentTime);
                user.setDisplayNick();
                updateCompass(user);

                // Check for new username. If they don't want the message, let's just say it's false.
                final boolean newUsername = ess.getSettings().isCustomNewUsernameMessage() && lastAccountName != null && !lastAccountName.equals(user.getBase().getName());

                if (!ess.getVanishedPlayersNew().isEmpty() && !user.isAuthorized("essentials.vanish.see")) {
                    for (final String p : ess.getVanishedPlayersNew()) {
                        final Player toVanish = ess.getServer().getPlayerExact(p);
                        if (toVanish != null && toVanish.isOnline()) {
                            user.getBase().hidePlayer(toVanish);
                            if (ess.getSettings().isDebug()) {
                                ess.getLogger().info("Hiding vanished player: " + p);
                            }
                        }
                    }
                }

                if (user.isAuthorized("essentials.sleepingignored")) {
                    user.getBase().setSleepingIgnored(true);
                }

                final String effectiveMessage;
                if (ess.getSettings().allowSilentJoinQuit() && (user.isAuthorized("essentials.silentjoin") || user.isAuthorized("essentials.silentjoin.vanish"))) {
                    if (user.isAuthorized("essentials.silentjoin.vanish")) {
                        user.setVanished(true);
                    }
                    effectiveMessage = null;
                } else if (message == null || hideJoinQuitMessages()) {
                    effectiveMessage = null;
                } else if (ess.getSettings().isCustomJoinMessage()) {
                    final String msg = (newUsername ? ess.getSettings().getCustomNewUsernameMessage() : ess.getSettings().getCustomJoinMessage())
                        .replace("{PLAYER}", player.getDisplayName()).replace("{USERNAME}", player.getName())
                        .replace("{UNIQUE}", NumberFormat.getInstance().format(ess.getUserMap().getUniqueUsers()))
                        .replace("{ONLINE}", NumberFormat.getInstance().format(ess.getOnlinePlayers().size()))
                        .replace("{UPTIME}", DateUtil.formatDateDiff(ManagementFactory.getRuntimeMXBean().getStartTime()))
                        .replace("{PREFIX}", FormatUtil.replaceFormat(ess.getPermissionsHandler().getPrefix(player)))
                        .replace("{SUFFIX}", FormatUtil.replaceFormat(ess.getPermissionsHandler().getSuffix(player)))
                        .replace("{OLDUSERNAME}", lastAccountName == null ? "" : lastAccountName);
                    if (!msg.isEmpty()) {
                        ess.getServer().broadcastMessage(msg);
                    }
                    effectiveMessage = msg.isEmpty() ? null : msg;
                } else if (ess.getSettings().allowSilentJoinQuit()) {
                    ess.getServer().broadcastMessage(message);
                    effectiveMessage = message;
                } else {
                    effectiveMessage = message;
                }

                ess.runTaskAsynchronously(() -> ess.getServer().getPluginManager().callEvent(new AsyncUserDataLoadEvent(user, effectiveMessage)));

                final int motdDelay = ess.getSettings().getMotdDelay() / 50;
                final DelayMotdTask motdTask = new DelayMotdTask(user);
                if (motdDelay > 0) {
                    ess.scheduleSyncDelayedTask(motdTask, motdDelay);
                } else {
                    motdTask.run();
                }

                if (!ess.getSettings().isCommandDisabled("mail") && user.isAuthorized("essentials.mail")) {
                    if (user.getUnreadMailAmount() == 0) {
                        if (ess.getSettings().isNotifyNoNewMail()) {
                            user.sendMessage(tl("noNewMail")); // Only notify if they want us to.
                        }
                    } else {
                        user.notifyOfMail();
                    }
                }

                if (user.isAuthorized("essentials.fly.safelogin")) {
                    user.getBase().setFallDistance(0);
                    if (LocationUtil.shouldFly(user.getLocation())) {
                        user.getBase().setAllowFlight(true);
                        user.getBase().setFlying(true);
                        if (ess.getSettings().isSendFlyEnableOnJoin()) {
                            user.getBase().sendMessage(tl("flyMode", tl("enabled"), user.getDisplayName()));
                        }
                    }
                }

                if (!user.isAuthorized("essentials.speed")) {
                    user.getBase().setFlySpeed(0.1f);
                    user.getBase().setWalkSpeed(0.2f);
                }

                if (user.isSocialSpyEnabled() && !user.isAuthorized("essentials.socialspy")) {
                    user.setSocialSpyEnabled(false);
                    ess.getLogger().log(Level.INFO, "Set socialspy to false for {0} because they had it enabled without permission.", user.getName());
                }

                if (user.isGodModeEnabled() && !user.isAuthorized("essentials.god")) {
                    user.setGodModeEnabled(false);
                    ess.getLogger().log(Level.INFO, "Set god mode to false for {0} because they had it enabled without permission.", user.getName());
                }

                user.setConfirmingClearCommand(null);
                user.getConfirmingPayments().clear();

                user.stopTransaction();
            }

            class DelayMotdTask implements Runnable {
                private final User user;

                DelayMotdTask(final User user) {
                    this.user = user;
                }

                @Override
                public void run() {
                    IText tempInput = null;

                    if (!ess.getSettings().isCommandDisabled("motd")) {
                        try {
                            tempInput = new TextInput(user.getSource(), "motd", true, ess);
                        } catch (final IOException ex) {
                            if (ess.getSettings().isDebug()) {
                                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
                            } else {
                                LOGGER.log(Level.WARNING, ex.getMessage());
                            }
                        }
                    }

                    final IText input = tempInput;

                    if (input != null && !input.getLines().isEmpty() && user.isAuthorized("essentials.motd")) {
                        final IText output = new KeywordReplacer(input, user.getSource(), ess);
                        final TextPager pager = new TextPager(output, true);
                        pager.showPage("1", null, "motd", user.getSource());
                    }

                    if (user.isAuthorized("essentials.updatecheck")) {
                        ess.runTaskAsynchronously(() -> {
                            for (String str : ess.getUpdateChecker().getVersionMessages(false, false)) {
                                user.sendMessage(str);
                            }
                        });
                    }
                }
            }
        }

        ess.scheduleSyncDelayedTask(new DelayJoinTask());
    }

    // Makes the compass item ingame always point to the first essentials home.  #EasterEgg
    // EssentialsX: This can now optionally require a permission to enable, if set in the config.
    private void updateCompass(final User user) {
        if (ess.getSettings().isCompassTowardsHomePerm() && !user.isAuthorized("essentials.home.compass")) return;

        final Location loc = user.getHome(user.getLocation());
        if (loc == null) {
            PaperLib.getBedSpawnLocationAsync(user.getBase(), false).thenAccept(location -> {
                if (location != null) {
                    user.getBase().setCompassTarget(location);
                }
            });
            return;
        }
        user.getBase().setCompassTarget(loc);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerLoginBanned(final PlayerLoginEvent event) {
        if (event.getResult() == Result.KICK_BANNED) {
            BanEntry banEntry = ess.getServer().getBanList(BanList.Type.NAME).getBanEntry(event.getPlayer().getName());
            if (banEntry != null) {
                final Date banExpiry = banEntry.getExpiration();
                if (banExpiry != null) {
                    final String expiry = DateUtil.formatDateDiff(banExpiry.getTime());
                    event.setKickMessage(tl("tempbanJoin", expiry, banEntry.getReason()));
                } else {
                    event.setKickMessage(tl("banJoin", banEntry.getReason()));
                }
            } else {
                banEntry = ess.getServer().getBanList(BanList.Type.IP).getBanEntry(event.getAddress().getHostAddress());
                if (banEntry != null) {
                    event.setKickMessage(tl("banIpJoin", banEntry.getReason()));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerLogin(final PlayerLoginEvent event) {
        if (event.getResult() == Result.KICK_FULL) {
            final User kfuser = ess.getUser(event.getPlayer());
            if (kfuser.isAuthorized("essentials.joinfullserver")) {
                event.allow();
                return;
            }
            if (ess.getSettings().isCustomServerFullMessage()) {
                event.disallow(Result.KICK_FULL, tl("serverFull"));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerTeleport(final PlayerTeleportEvent event) {
        final boolean backListener = ess.getSettings().registerBackInListener();
        final boolean teleportInvulnerability = ess.getSettings().isTeleportInvulnerability();
        if (backListener || teleportInvulnerability) {
            final Player player = event.getPlayer();
            if (player.hasMetadata("NPC")) {
                return;
            }
            final User user = ess.getUser(player);
            //There is TeleportCause.COMMMAND but plugins have to actively pass the cause in on their teleports.
            if (user.isAuthorized("essentials.back.onteleport") && backListener && (event.getCause() == TeleportCause.PLUGIN || event.getCause() == TeleportCause.COMMAND)) {
                user.setLastLocation();
            }
            if (teleportInvulnerability && (event.getCause() == TeleportCause.PLUGIN || event.getCause() == TeleportCause.COMMAND)) {
                user.enableInvulnerabilityAfterTeleport();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerEggThrow(final PlayerEggThrowEvent event) {
        final User user = ess.getUser(event.getPlayer());
        final ItemStack stack = new ItemStack(Material.EGG, 1);
        if (user.hasUnlimited(stack)) {
            user.getBase().getInventory().addItem(stack);
            user.getBase().updateInventory();
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerBucketEmpty(final PlayerBucketEmptyEvent event) {
        final User user = ess.getUser(event.getPlayer());
        if (user.hasUnlimited(new ItemStack(event.getBucket()))) {
            event.getItemStack().setType(event.getBucket());
            ess.scheduleSyncDelayedTask(user.getBase()::updateInventory);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommandPreprocess(final PlayerCommandPreprocessEvent event) {
        final String cmd = event.getMessage().toLowerCase(Locale.ENGLISH).split(" ")[0].replace("/", "").toLowerCase(Locale.ENGLISH);
        final int argStartIndex = event.getMessage().indexOf(" ");
        final String args = argStartIndex == -1 ? "" // No arguments present
                : event.getMessage().substring(argStartIndex); // arguments start at argStartIndex; substring from there.

        // If the plugin command does not exist, check if it is an alias from commands.yml
        if (ess.getServer().getPluginCommand(cmd) == null) {
            final Command knownCommand = ess.getKnownCommandsProvider().getKnownCommands().get(cmd);
            if (knownCommand instanceof FormattedCommandAlias) {
                final FormattedCommandAlias command = (FormattedCommandAlias) knownCommand;
                for (String fullCommand : ess.getFormattedCommandAliasProvider().createCommands(command, event.getPlayer(), args.split(" "))) {
                    handlePlayerCommandPreprocess(event, fullCommand);
                }
                return;
            }
        }

        // Handle the command given from the event.
        handlePlayerCommandPreprocess(event, cmd + args);
    }

    public void handlePlayerCommandPreprocess(final PlayerCommandPreprocessEvent event, final String effectiveCommand) {
        final Player player = event.getPlayer();
        final String cmd = effectiveCommand.toLowerCase(Locale.ENGLISH).split(" ")[0].replace("/", "").toLowerCase(Locale.ENGLISH);
        final PluginCommand pluginCommand = ess.getServer().getPluginCommand(cmd);

        if (ess.getSettings().getSocialSpyCommands().contains(cmd) || ess.getSettings().getSocialSpyCommands().contains("*")) {
            if (pluginCommand == null
                || (!pluginCommand.getName().equals("msg") && !pluginCommand.getName().equals("r"))) { // /msg and /r are handled in SimpleMessageRecipient
                final User user = ess.getUser(player);
                if (!user.isAuthorized("essentials.chat.spy.exempt")) {
                    for (final User spyer : ess.getOnlineUsers()) {
                        if (spyer.isSocialSpyEnabled() && !player.equals(spyer.getBase())) {
                            if (user.isMuted() && ess.getSettings().getSocialSpyListenMutedPlayers()) {
                                spyer.sendMessage(tl("socialSpyMutedPrefix") + player.getDisplayName() + ": " + event.getMessage());
                            } else {
                                spyer.sendMessage(tl("socialSpyPrefix") + player.getDisplayName() + ": " + event.getMessage());
                            }
                        }
                    }
                }
            }
        }

        final User user = ess.getUser(player);
        if (user.isMuted() && (ess.getSettings().getMuteCommands().contains(cmd) || ess.getSettings().getMuteCommands().contains("*"))) {
            event.setCancelled(true);
            final String dateDiff = user.getMuteTimeout() > 0 ? DateUtil.formatDateDiff(user.getMuteTimeout()) : null;
            if (dateDiff == null) {
                player.sendMessage(user.hasMuteReason() ? tl("voiceSilencedReason", user.getMuteReason()) : tl("voiceSilenced"));
            } else {
                player.sendMessage(user.hasMuteReason() ? tl("voiceSilencedReasonTime", dateDiff, user.getMuteReason()) : tl("voiceSilencedTime", dateDiff));
            }
            LOGGER.info(tl("mutedUserSpeaks", player.getName(), event.getMessage()));
            return;
        }

        boolean broadcast = true; // whether to broadcast the updated activity
        boolean update = true; // Only modified when the command is afk

        if (pluginCommand != null) {
            // Switch case for commands that shouldn't broadcast afk activity.
            switch (pluginCommand.getName()) {
                case "afk":
                    update = false;
                    // fall through
                case "vanish":
                    broadcast = false;
                    break;
            }
        }

        if (update) {
            user.updateActivityOnInteract(broadcast);
        }

        if (ess.getSettings().isCommandCooldownsEnabled()
            && !user.isAuthorized("essentials.commandcooldowns.bypass")) {
            final int argStartIndex = effectiveCommand.indexOf(" ");
            final String args = argStartIndex == -1 ? "" // No arguments present
                : " " + effectiveCommand.substring(argStartIndex); // arguments start at argStartIndex; substring from there.
            final String fullCommand = pluginCommand == null ? effectiveCommand : pluginCommand.getName() + args;

            // Used to determine whether a user already has an existing cooldown
            // If so, no need to check for (and write) new ones.
            boolean cooldownFound = false;

            // Iterate over a copy of getCommandCooldowns in case of concurrent modifications
            for (final Entry<Pattern, Long> entry : new HashMap<>(user.getCommandCooldowns()).entrySet()) {
                // Remove any expired cooldowns
                if (entry.getValue() <= System.currentTimeMillis()) {
                    user.clearCommandCooldown(entry.getKey());
                    // Don't break in case there are other command cooldowns left to clear.
                } else if (entry.getKey().matcher(fullCommand).matches()) {
                    // User's current cooldown hasn't expired, inform and terminate cooldown code.
                    if (entry.getValue() > System.currentTimeMillis()) {
                        final String commandCooldownTime = DateUtil.formatDateDiff(entry.getValue());
                        user.sendMessage(tl("commandCooldown", commandCooldownTime));
                        cooldownFound = true;
                        event.setCancelled(true);
                        break;
                    }
                }
            }

            if (!cooldownFound) {
                final Entry<Pattern, Long> cooldownEntry = ess.getSettings().getCommandCooldownEntry(fullCommand);

                if (cooldownEntry != null) {
                    if (ess.getSettings().isDebug()) {
                        ess.getLogger().info("Applying " + cooldownEntry.getValue() + "ms cooldown on /" + fullCommand + " for" + user.getName() + ".");
                    }
                    final Date expiry = new Date(System.currentTimeMillis() + cooldownEntry.getValue());
                    user.addCommandCooldown(cooldownEntry.getKey(), expiry, ess.getSettings().isCommandCooldownPersistent(fullCommand));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerChangedWorldFlyReset(final PlayerChangedWorldEvent event) {
        final User user = ess.getUser(event.getPlayer());

        if (ess.getSettings().isWorldChangeFlyResetEnabled()) {
            if (user.getBase().getGameMode() != GameMode.CREATIVE
                // COMPAT: String compare for 1.7.10
                && !user.getBase().getGameMode().name().equals("SPECTATOR")
                && !user.isAuthorized("essentials.fly")) {
                user.getBase().setFallDistance(0f);
                user.getBase().setAllowFlight(false);
            }
        }

        if (ess.getSettings().isWorldChangeSpeedResetEnabled()) {
            if (!user.isAuthorized("essentials.speed")) {
                user.getBase().setFlySpeed(0.1f);
                user.getBase().setWalkSpeed(0.2f);
            } else {
                if (user.getBase().getFlySpeed() > ess.getSettings().getMaxFlySpeed() && !user.isAuthorized("essentials.speed.bypass")) {
                    user.getBase().setFlySpeed((float) ess.getSettings().getMaxFlySpeed());
                } else {
                    user.getBase().setFlySpeed(user.getBase().getFlySpeed() * 0.99999f);
                }

                if (user.getBase().getWalkSpeed() > ess.getSettings().getMaxWalkSpeed() && !user.isAuthorized("essentials.speed.bypass")) {
                    user.getBase().setWalkSpeed((float) ess.getSettings().getMaxWalkSpeed());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(final PlayerChangedWorldEvent event) {
        final User user = ess.getUser(event.getPlayer());
        final String newWorld = event.getPlayer().getLocation().getWorld().getName();
        user.setDisplayNick();
        updateCompass(user);
        if (ess.getSettings().getNoGodWorlds().contains(newWorld) && user.isGodModeEnabledRaw()) {
            // Player god mode is never disabled in order to retain it when changing worlds once more.
            // With that said, players will still take damage as per the result of User#isGodModeEnabled()
            user.sendMessage(tl("noGodWorldWarning"));
        }

        if (!user.getWorld().getName().equals(newWorld)) {
            user.sendMessage(tl("currentWorld", newWorld));
        }
        if (user.isVanished()) {
            user.setVanished(user.isAuthorized("essentials.vanish"));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        boolean updateActivity = true;

        switch (event.getAction()) {
            case RIGHT_CLICK_BLOCK:
                if (!event.isCancelled() && MaterialUtil.isBed(event.getClickedBlock().getType()) && ess.getSettings().getUpdateBedAtDaytime()) {
                    final User player = ess.getUser(event.getPlayer());
                    if (player.isAuthorized("essentials.sethome.bed") && player.getWorld().getEnvironment().equals(World.Environment.NORMAL)) {
                        player.getBase().setBedSpawnLocation(event.getClickedBlock().getLocation());
                        // In 1.15 and above, vanilla sends its own bed spawn message.
                        if (VersionUtil.getServerBukkitVersion().isLowerThan(VersionUtil.v1_15_R01)) {
                            player.sendMessage(tl("bedSet", player.getLocation().getWorld().getName(), player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ()));
                        }
                    }
                }
                break;
            case LEFT_CLICK_AIR:
                if (event.getPlayer().isFlying()) {
                    final User user = ess.getUser(event.getPlayer());
                    if (user.isFlyClickJump()) {
                        useFlyClickJump(user);
                        break;
                    }
                }
                // fall through
            case LEFT_CLICK_BLOCK:
                if (event.getItem() != null && event.getItem().getType() != Material.AIR) {
                    final User user = ess.getUser(event.getPlayer());
                    if (user.hasPowerTools() && user.arePowerToolsEnabled() && usePowertools(user, event.getItem().getType())) {
                        event.setCancelled(true);
                    }
                }
                break;
            case PHYSICAL:
                updateActivity = false;
                break;
            default:
                break;
        }

        if (updateActivity) {
            ess.getUser(event.getPlayer()).updateActivityOnInteract(true);
        }
    }

    // This method allows the /jump lock feature to work, allows teleporting while flying #EasterEgg
    private void useFlyClickJump(final User user) {
        try {
            final Location otarget = LocationUtil.getTarget(user.getBase());

            class DelayedClickJumpTask implements Runnable {
                @Override
                public void run() {
                    final Location loc = user.getLocation();
                    loc.setX(otarget.getX());
                    loc.setZ(otarget.getZ());
                    while (LocationUtil.isBlockDamaging(loc.getWorld(), loc.getBlockX(), loc.getBlockY() - 1, loc.getBlockZ())) {
                        loc.setY(loc.getY() + 1d);
                    }
                    PaperLib.teleportAsync(user.getBase(), loc, TeleportCause.PLUGIN);
                }
            }

            ess.scheduleSyncDelayedTask(new DelayedClickJumpTask());
        } catch (final Exception ex) {
            if (ess.getSettings().isDebug()) {
                LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            }
        }
    }

    private boolean usePowertools(final User user, final Material material) {
        final List<String> commandList = user.getPowertool(material);
        if (commandList == null || commandList.isEmpty()) {
            return false;
        }
        boolean used = false;
        // We need to loop through each command and execute
        for (final String command : commandList) {
            if (command.contains("{player}")) {
            } else if (command.startsWith("c:")) {
                used = true;
                user.getBase().chat(command.substring(2));
            } else {
                used = true;

                class PowerToolUseTask implements Runnable {
                    @Override
                    public void run() {
                        user.getBase().chat("/" + command);
                        LOGGER.log(Level.INFO, String.format("[PT] %s issued server command: /%s", user.getName(), command));
                    }
                }

                ess.scheduleSyncDelayedTask(new PowerToolUseTask());

            }
        }
        return used;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryClickEvent(final InventoryClickEvent event) {
        Player refreshPlayer = null;
        final Inventory top = event.getView().getTopInventory();
        final InventoryType type = top.getType();

        final Inventory clickedInventory;
        if (event.getRawSlot() < 0) {
            clickedInventory = null;
        } else {
            clickedInventory = event.getRawSlot() < top.getSize() ? top : event.getView().getBottomInventory();
        }

        final User user = ess.getUser((Player) event.getWhoClicked());
        if (type == InventoryType.PLAYER) {
            final InventoryHolder invHolder = top.getHolder();
            if (invHolder instanceof HumanEntity) {
                final User invOwner = ess.getUser((Player) invHolder);
                if (user.isInvSee() && (!user.isAuthorized("essentials.invsee.modify") || invOwner.isAuthorized("essentials.invsee.preventmodify") || !invOwner.getBase().isOnline())) {
                    event.setCancelled(true);
                    refreshPlayer = user.getBase();
                }
            }
        } else if (type == InventoryType.ENDER_CHEST) {
            if (user.isEnderSee() && !user.isAuthorized("essentials.enderchest.modify")) {
                event.setCancelled(true);
                refreshPlayer = user.getBase();
            }
        } else if (type == InventoryType.WORKBENCH) {
            if (user.isRecipeSee()) {
                event.setCancelled(true);
                refreshPlayer = user.getBase();
            }
        } else if (type == InventoryType.CHEST && top.getSize() == 9) {
            final InventoryHolder invHolder = top.getHolder();
            if (invHolder instanceof HumanEntity && user.isInvSee() && event.getClick() != ClickType.MIDDLE) {
                event.setCancelled(true);
                refreshPlayer = user.getBase();
            }
        } else if (clickedInventory != null && clickedInventory.getType() == InventoryType.PLAYER) {
            if (ess.getSettings().isDirectHatAllowed() && event.getClick() == ClickType.LEFT && event.getSlot() == 39
                && event.getCursor().getType() != Material.AIR && event.getCursor().getType().getMaxDurability() == 0
                && !MaterialUtil.isSkull(event.getCursor().getType())
                && user.isAuthorized("essentials.hat") && !user.isAuthorized("essentials.hat.prevent-type." + event.getCursor().getType().name().toLowerCase())
                && !isPreventBindingHat(user, (PlayerInventory) clickedInventory)) {
                event.setCancelled(true);
                final PlayerInventory inv = (PlayerInventory) clickedInventory;
                final ItemStack head = inv.getHelmet();
                inv.setHelmet(event.getCursor());
                event.setCursor(head);
            }
        }

        if (refreshPlayer != null) {
            ess.scheduleSyncDelayedTask(refreshPlayer::updateInventory, 1);
        }
    }

    private boolean isPreventBindingHat(User user, PlayerInventory inventory) {
        if (VersionUtil.getServerBukkitVersion().isHigherThan(VersionUtil.v1_9_4_R01)) {
            final ItemStack head = inventory.getHelmet();
            return head != null && head.getEnchantments().containsKey(Enchantment.BINDING_CURSE) && !user.isAuthorized("essentials.hat.ignore-binding");
        }
        return false;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryCloseEvent(final InventoryCloseEvent event) {
        Player refreshPlayer = null;
        final Inventory top = event.getView().getTopInventory();
        final InventoryType type = top.getType();
        if (type == InventoryType.PLAYER) {
            final User user = ess.getUser((Player) event.getPlayer());
            user.setInvSee(false);
            refreshPlayer = user.getBase();
        } else if (type == InventoryType.ENDER_CHEST) {
            final User user = ess.getUser((Player) event.getPlayer());
            user.setEnderSee(false);
            refreshPlayer = user.getBase();
        } else if (type == InventoryType.WORKBENCH) {
            final User user = ess.getUser((Player) event.getPlayer());
            if (user.isRecipeSee()) {
                user.setRecipeSee(false);
                event.getView().getTopInventory().clear();
                refreshPlayer = user.getBase();
            }
        } else if (type == InventoryType.CHEST && top.getSize() == 9) {
            final InventoryHolder invHolder = top.getHolder();
            if (invHolder instanceof HumanEntity) {
                final User user = ess.getUser((Player) event.getPlayer());
                user.setInvSee(false);
                refreshPlayer = user.getBase();
            }
        }

        if (refreshPlayer != null) {
            ess.scheduleSyncDelayedTask(refreshPlayer::updateInventory, 1);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerFishEvent(final PlayerFishEvent event) {
        final User user = ess.getUser(event.getPlayer());
        user.updateActivityOnInteract(true);
    }

    private static final class ArrowPickupListener implements Listener {
        @EventHandler(priority = EventPriority.LOW)
        public void onArrowPickup(final org.bukkit.event.player.PlayerPickupArrowEvent event) {
            if (event.getItem().hasMetadata(Commandfireball.FIREBALL_META_KEY)) {
                event.setCancelled(true);
            }
        }
    }

    private final class PickupListenerPre1_12 implements Listener {
        @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
        public void onPlayerPickupItem(final org.bukkit.event.player.PlayerPickupItemEvent event) {
            if (event.getItem().hasMetadata(Commandfireball.FIREBALL_META_KEY)) {
                event.setCancelled(true);
            } else if (ess.getSettings().getDisableItemPickupWhileAfk()) {
                if (ess.getUser(event.getPlayer()).isAfk()) {
                    event.setCancelled(true);
                }
            }
        }
    }

    private final class PickupListener1_12 implements Listener {
        @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
        public void onPlayerPickupItem(final org.bukkit.event.entity.EntityPickupItemEvent event) {
            if (ess.getSettings().getDisableItemPickupWhileAfk() && event.getEntity() instanceof Player) {
                if (ess.getUser((Player) event.getEntity()).isAfk()) {
                    event.setCancelled(true);
                }
            }
        }
    }

    private final class CommandSendListener implements Listener {
        @EventHandler(priority = EventPriority.NORMAL)
        public void onCommandSend(final PlayerCommandSendEvent event) {
            final User user = ess.getUser(event.getPlayer());

            final Set<PluginCommand> checked = new HashSet<>();
            final Set<PluginCommand> toRemove = new HashSet<>();

            event.getCommands().removeIf(label -> {
                if (isEssentialsCommand(label)) {
                    final PluginCommand command = ess.getServer().getPluginCommand(label);
                    if (!checked.contains(command)) {
                        checked.add(command);
                        if (!user.isAuthorized(command.getName().equals("r") ? "essentials.msg" : "essentials." + command.getName())) {
                            toRemove.add(command);
                        }
                    }
                    return toRemove.contains(command);
                }
                return false;
            });

            if (ess.getSettings().isDebug()) {
                ess.getLogger().info("Removed commands: " + toRemove.toString());
            }
        }

        /**
         * Returns true if all of the following are true:
         * - The command is a plugin command
         * - The plugin command is from a plugin in an essentials-controlled package
         * - There is no known alternative OR the alternative is overridden by Essentials
         */
        private boolean isEssentialsCommand(final String label) {
            final PluginCommand command = ess.getServer().getPluginCommand(label);

            return command != null
                && (command.getPlugin() == ess || command.getPlugin().getClass().getName().startsWith("com.earth2me.essentials"))
                && (ess.getSettings().isCommandOverridden(label) || (ess.getAlternativeCommandsHandler().getAlternative(label) == null));
        }
    }
}
