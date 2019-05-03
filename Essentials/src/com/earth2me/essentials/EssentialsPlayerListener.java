package com.earth2me.essentials;

import com.earth2me.essentials.textreader.IText;
import com.earth2me.essentials.textreader.KeywordReplacer;
import com.earth2me.essentials.textreader.TextInput;
import com.earth2me.essentials.textreader.TextPager;
import com.earth2me.essentials.utils.DateUtil;
import com.earth2me.essentials.utils.LocationUtil;
import com.earth2me.essentials.utils.MaterialUtil;
import io.papermc.lib.PaperLib;
import net.ess3.api.IEssentials;
import org.bukkit.*;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static com.earth2me.essentials.I18n.capitalCase;


public class EssentialsPlayerListener implements Listener {
    private static final Logger LOGGER = Logger.getLogger("Essentials");
    private final transient IEssentials ess;

    public EssentialsPlayerListener(final IEssentials parent) {
        this.ess = parent;
    }

    public void registerEvents() {
        ess.getServer().getPluginManager().registerEvents(this, ess);

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

            user.sendTl(user.hasMuteReason() ? user.tl("voiceSilencedReason", user.getMuteReason()) : user.tl("voiceSilenced"));

            LOGGER.info(user.tl("mutedUserSpeaks", user.getName(), event.getMessage()));
        }
        try {
            final Iterator<Player> it = event.getRecipients().iterator();
            while (it.hasNext()) {
                final User u = ess.getUser(it.next());
                if (u.isIgnoredPlayer(user)) {
                    it.remove();
                }
            }
        } catch (UnsupportedOperationException ex) {
            if (ess.getSettings().isDebug()) {
                ess.getLogger().log(Level.INFO, "Ignore could not block chat due to custom chat plugin event.", ex);
            } else {
                ess.getLogger().info("Ignore could not block chat due to custom chat plugin event.");
            }
        }

        user.updateActivityOnInteract(true);
        user.setDisplayNick();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(final PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() && event.getFrom().getBlockZ() == event.getTo().getBlockZ() && event.getFrom().getBlockY() == event.getTo().getBlockY()) {
            return;
        }

        if (!ess.getSettings().cancelAfkOnMove() && !ess.getSettings().getFreezeAfkPlayers()) {
            event.getHandlers().unregister(this);

            if (ess.getSettings().isDebug()) {
                LOGGER.log(Level.INFO, "Unregistering move listener");
            }

            return;
        }

        final User user = ess.getUser(event.getPlayer());
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
            } catch (Exception ex) {
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

        if (ess.getSettings().allowSilentJoinQuit() && user.isAuthorized("essentials.silentquit")) {
            event.setQuitMessage(null);
        } else if (ess.getSettings().isCustomQuitMessage() && event.getQuitMessage() != null) {
            final Player player = event.getPlayer();
            final String msg = ess.getSettings().getCustomQuitMessage()
                    .replace("{PLAYER}", player.getDisplayName())
                    .replace("{USERNAME}", player.getName())
                    .replace("{ONLINE}", NumberFormat.getInstance().format(ess.getOnlinePlayers().size()));
            
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

        for (HumanEntity viewer : user.getBase().getInventory().getViewers()) {
            if (viewer instanceof Player) {
                User uviewer = ess.getUser((Player) viewer);
                if (uviewer.isInvSee()) {
                    uviewer.getBase().closeInventory();
                }
            }
        }

        user.updateActivity(false);
        if (!user.isHidden()) {
            user.setLastLogout(System.currentTimeMillis());
        }
        user.stopTransaction();

        user.dispose();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final String joinMessage = event.getJoinMessage();
        ess.runTaskAsynchronously(new Runnable() {
            @Override
            public void run() {
                delayedJoin(event.getPlayer(), joinMessage);
            }
        });
        if (ess.getSettings().allowSilentJoinQuit() || ess.getSettings().isCustomJoinMessage()) {
            event.setJoinMessage(null);
        }
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
        dUser.updateActivity(false);
        dUser.stopTransaction();

        class DelayJoinTask implements Runnable {
            @Override
            public void run() {
                final User user = ess.getUser(player);

                if (!user.getBase().isOnline()) {
                    return;
                }

                user.startTransaction();

                user.setLastAccountName(user.getBase().getName());
                user.setLastLogin(currentTime);
                user.setDisplayNick();
                updateCompass(user);

                if (!ess.getVanishedPlayersNew().isEmpty() && !user.isAuthorized("essentials.vanish.see")) {
                    for (String p : ess.getVanishedPlayersNew()) {
                        Player toVanish = ess.getServer().getPlayerExact(p);
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

                if (ess.getSettings().allowSilentJoinQuit() && (user.isAuthorized("essentials.silentjoin") || user.isAuthorized("essentials.silentjoin.vanish"))) {
                    if (user.isAuthorized("essentials.silentjoin.vanish")) {
                        user.setVanished(true);
                    }
                } else if (message == null) {
                    //NOOP
                } else if (ess.getSettings().isCustomJoinMessage()) {
                    String msg = ess.getSettings().getCustomJoinMessage()
                        .replace("{PLAYER}", player.getDisplayName()).replace("{USERNAME}", player.getName())
                        .replace("{UNIQUE}", NumberFormat.getInstance().format(ess.getUserMap().getUniqueUsers()))
                        .replace("{ONLINE}", NumberFormat.getInstance().format(ess.getOnlinePlayers().size()));
                    if (!msg.isEmpty()) {
                        ess.getServer().broadcastMessage(msg);
                    }
                } else if (ess.getSettings().allowSilentJoinQuit()) {
                    ess.getServer().broadcastMessage(message);
                }

                int motdDelay = ess.getSettings().getMotdDelay() / 50;
                DelayMotdTask motdTask = new DelayMotdTask(user);
                if (motdDelay > 0) {
                    ess.scheduleSyncDelayedTask(motdTask, motdDelay);
                } else {
                    motdTask.run();
                }

                if (!ess.getSettings().isCommandDisabled("mail") && user.isAuthorized("essentials.mail")) {
                    final List<String> mail = user.getMails();
                    if (mail.isEmpty()) {
                        if (ess.getSettings().isNotifyNoNewMail()) {
                            user.sendTl("noNewMail"); // Only notify if they want us to.
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
                            user.sendTl("flyMode", user.tl( "enabled"), user.getDisplayName());
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
                private User user;

                public DelayMotdTask(User user) {
                    this.user = user;
                }

                @Override
                public void run() {
                    IText tempInput = null;

                    if (!ess.getSettings().isCommandDisabled("motd")) {
                        try {
                            tempInput = new TextInput(user.getSource(), "motd", true, ess);
                        } catch (IOException ex) {
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
                }
            }
        }

        ess.scheduleSyncDelayedTask(new DelayJoinTask());
    }

    // Makes the compass item ingame always point to the first essentials home.  #EasterEgg
    // EssentialsX: This can now optionally require a permission to enable, if set in the config.
    private void updateCompass(final User user) {
        if (ess.getSettings().isCompassTowardsHomePerm() && !user.isAuthorized("essentials.home.compass")) return;

        Location loc = user.getHome(user.getLocation());
        if (loc == null) {
            loc = user.getBase().getBedSpawnLocation();
        }
        if (loc != null) {
            user.getBase().setCompassTarget(loc);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerLoginBanned(final PlayerLoginEvent event) {
        switch (event.getResult()) {
            case KICK_BANNED:
                BanEntry banEntry = ess.getServer().getBanList(BanList.Type.NAME).getBanEntry(event.getPlayer().getName());
                if (banEntry != null) {
                    final User buser = ess.getUser(event.getPlayer());
                    Date banExpiry = banEntry.getExpiration();
                    if (banExpiry != null) {
                        String expiry = DateUtil.formatDateDiff(banExpiry.getTime());
                        event.setKickMessage(buser.tl("tempbanJoin", expiry, banEntry.getReason()));
                    } else {
                        event.setKickMessage(buser.tl("banJoin", banEntry.getReason()));
                    }
                } else {
                    banEntry = ess.getServer().getBanList(BanList.Type.IP).getBanEntry(event.getAddress().getHostAddress());
                    if (banEntry != null) {
                        final User buser = ess.getUser(event.getPlayer());
                        event.setKickMessage(buser.tl("banIpJoin", banEntry.getReason()));
                    }
                }
                break;
            default:
                break;
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerLogin(final PlayerLoginEvent event) {
        switch (event.getResult()) {
            case KICK_FULL:
                final User kfuser = ess.getUser(event.getPlayer());
                if (kfuser.isAuthorized("essentials.joinfullserver")) {
                    event.allow();
                    return;
                }
                event.disallow(Result.KICK_FULL, kfuser.tl("serverFull"));
                break;
            default:
                break;
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerTeleport(final PlayerTeleportEvent event) {
        final boolean backListener = ess.getSettings().registerBackInListener();
        final boolean teleportInvulnerability = ess.getSettings().isTeleportInvulnerability();
        if (backListener || teleportInvulnerability) {
            final User user = ess.getUser(event.getPlayer());
            //There is TeleportCause.COMMMAND but plugins have to actively pass the cause in on their teleports.
            if (backListener && (event.getCause() == TeleportCause.PLUGIN || event.getCause() == TeleportCause.COMMAND)) {
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
            ess.scheduleSyncDelayedTask(new Runnable() {
                @Override
                public void run() {
                    user.getBase().updateInventory();
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommandPreprocess(final PlayerCommandPreprocessEvent event) {
        final Player player = event.getPlayer();
        final String cmd = event.getMessage().toLowerCase(Locale.ENGLISH).split(" ")[0].replace("/", "").toLowerCase(Locale.ENGLISH);

        PluginCommand pluginCommand = ess.getServer().getPluginCommand(cmd);

        if (ess.getSettings().getSocialSpyCommands().contains(cmd) || ess.getSettings().getSocialSpyCommands().contains("*")) {
            if (pluginCommand == null
                    || (!pluginCommand.getName().equals("msg") && !pluginCommand.getName().equals("r"))) { // /msg and /r are handled in SimpleMessageRecipient
                User user = ess.getUser(player);
                if (!user.isAuthorized("essentials.chat.spy.exempt")) {
                    for (User spyer : ess.getOnlineUsers()) {
                        if (spyer.isSocialSpyEnabled() && !player.equals(spyer.getBase())) {
                            if (user.isMuted() && ess.getSettings().getSocialSpyListenMutedPlayers()) {
                                spyer.sendMessage(spyer.tl( "socialSpyMutedPrefix") + player.getDisplayName() + ": " + event.getMessage());
                            } else {
                                spyer.sendMessage(spyer.tl( "socialSpyPrefix") + player.getDisplayName() + ": " + event.getMessage());
                            }
                        }
                    }
                }
            }
        }

        final User user = ess.getUser(player);
        if (user.isMuted() && (ess.getSettings().getMuteCommands().contains(cmd) || ess.getSettings().getMuteCommands().contains("*"))) {
            event.setCancelled(true);
            user.sendTl("voiceSilenced");
            LOGGER.info(user.tl("mutedUserSpeaks", player.getName(), event.getMessage()));
            return;
        }
        
        boolean broadcast = true; // whether to broadcast the updated activity
        boolean update = true; // Only modified when the command is afk

        if (pluginCommand != null) {
            // Switch case for commands that shouldn't broadcast afk activity.
            switch (pluginCommand.getName()) {
                case "afk":
                    update = false;
                case "vanish":
                    broadcast = false;
            }
        }
        if (update) {
            user.updateActivityOnInteract(broadcast);
        }

        if (ess.getSettings().isCommandCooldownsEnabled() && pluginCommand != null
            && !user.isAuthorized("essentials.commandcooldowns.bypass")) {
            int argStartIndex = event.getMessage().indexOf(" ");
            String args = argStartIndex == -1 ? "" // No arguments present 
                : " " + event.getMessage().substring(argStartIndex); // arguments start at argStartIndex; substring from there.
            String fullCommand = pluginCommand.getName() + args;

            // Used to determine whether a user already has an existing cooldown
            // If so, no need to check for (and write) new ones.
            boolean cooldownFound = false;
            
            // Iterate over a copy of getCommandCooldowns in case of concurrent modifications
            for (Entry<Pattern, Long> entry : new HashMap<>(user.getCommandCooldowns()).entrySet()) {
                // Remove any expired cooldowns
                if (entry.getValue() <= System.currentTimeMillis()) {
                    user.clearCommandCooldown(entry.getKey());
                    // Don't break in case there are other command cooldowns left to clear.
                } else if (entry.getKey().matcher(fullCommand).matches()) {
                    // User's current cooldown hasn't expired, inform and terminate cooldown code.
                    if (entry.getValue() > System.currentTimeMillis()) {
                        String commandCooldownTime = DateUtil.formatDateDiff(user.getSource(), entry.getValue());
                        user.sendTl("commandCooldown", commandCooldownTime);
                        cooldownFound = true;
                        event.setCancelled(true);
                        break;
                    }
                }
            }

            if (!cooldownFound) {
                Entry<Pattern, Long> cooldownEntry = ess.getSettings().getCommandCooldownEntry(fullCommand);

                if (cooldownEntry != null) {
                    if (ess.getSettings().isDebug()) {
                        ess.getLogger().info("Applying " + cooldownEntry.getValue() + "ms cooldown on /" + fullCommand + " for" + user.getName() + ".");
                    }
                    Date expiry = new Date(System.currentTimeMillis() + cooldownEntry.getValue());
                    user.addCommandCooldown(cooldownEntry.getKey(), expiry, ess.getSettings().isCommandCooldownPersistent(fullCommand));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerChangedWorldFlyReset(final PlayerChangedWorldEvent event) {
        final User user = ess.getUser(event.getPlayer());
        if (user.getBase().getGameMode() != GameMode.CREATIVE
                // COMPAT: String compare for 1.7.10
            && !user.getBase().getGameMode().name().equals("SPECTATOR")
            && !user.isAuthorized("essentials.fly")) {
            user.getBase().setFallDistance(0f);
            user.getBase().setAllowFlight(false);
        }
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
            } else {
                user.getBase().setWalkSpeed(user.getBase().getWalkSpeed() * 0.99999f);
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
            user.sendTl("noGodWorldWarning");
        }

        if (!user.getWorld().getName().equals(newWorld)) {
            user.sendTl("currentWorld", newWorld);
        }
        if (user.isVanished()) {
            user.setVanished(user.isAuthorized("essentials.vanish"));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(final PlayerInteractEvent event) {
        switch (event.getAction()) {
            case RIGHT_CLICK_BLOCK:
                if (!event.isCancelled() && MaterialUtil.isBed(event.getClickedBlock().getType()) && ess.getSettings().getUpdateBedAtDaytime()) {
                    User player = ess.getUser(event.getPlayer());
                    if (player.isAuthorized("essentials.sethome.bed") && player.getWorld().getEnvironment().equals(World.Environment.NORMAL)) {
                        player.getBase().setBedSpawnLocation(event.getClickedBlock().getLocation());
                        player.sendTl("bedSet", player.getLocation().getWorld().getName(), player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ());
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
            case LEFT_CLICK_BLOCK:
                if (event.getItem() != null && event.getItem().getType() != Material.AIR) {
                    final User user = ess.getUser(event.getPlayer());
                    if (user.hasPowerTools() && user.arePowerToolsEnabled() && usePowertools(user, event.getItem().getType())) {
                        event.setCancelled(true);
                    }
                }
                break;
        }
            ess.getUser(event.getPlayer()).updateActivityOnInteract(true);
    }

    // This method allows the /jump lock feature to work, allows teleporting while flying #EasterEgg
    private void useFlyClickJump(final User user) {
        try {
            final Location otarget = LocationUtil.getTarget(user.getBase());

            class DelayedClickJumpTask implements Runnable {
                @Override
                public void run() {
                    Location loc = user.getLocation();
                    loc.setX(otarget.getX());
                    loc.setZ(otarget.getZ());
                    while (LocationUtil.isBlockDamaging(loc.getWorld(), loc.getBlockX(), loc.getBlockY() - 1, loc.getBlockZ())) {
                        loc.setY(loc.getY() + 1d);
                    }
                    PaperLib.teleportAsync(user.getBase(), loc, TeleportCause.PLUGIN);
                }
            }
            ess.scheduleSyncDelayedTask(new DelayedClickJumpTask());
        } catch (Exception ex) {
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
                continue;
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

        if (type == InventoryType.PLAYER) {
            final User user = ess.getUser((Player) event.getWhoClicked());
            final InventoryHolder invHolder = top.getHolder();
            if (invHolder != null && invHolder instanceof HumanEntity) {
                final User invOwner = ess.getUser((Player) invHolder);
                if (user.isInvSee() && (!user.isAuthorized("essentials.invsee.modify") || invOwner.isAuthorized("essentials.invsee.preventmodify") || !invOwner.getBase().isOnline())) {
                    event.setCancelled(true);
                    refreshPlayer = user.getBase();
                }
            }
        } else if (type == InventoryType.ENDER_CHEST) {
            final User user = ess.getUser((Player) event.getWhoClicked());
            if (user.isEnderSee() && (!user.isAuthorized("essentials.enderchest.modify"))) {
                event.setCancelled(true);
                refreshPlayer = user.getBase();
            }
        } else if (type == InventoryType.WORKBENCH) {
            User user = ess.getUser((Player) event.getWhoClicked());
            if (user.isRecipeSee()) {
                event.setCancelled(true);
                refreshPlayer = user.getBase();
            }
        } else if (type == InventoryType.CHEST && top.getSize() == 9) {
            final User user = ess.getUser((Player) event.getWhoClicked());
            final InventoryHolder invHolder = top.getHolder();
            if (invHolder != null && invHolder instanceof HumanEntity && user.isInvSee()) {
                event.setCancelled(true);
                refreshPlayer = user.getBase();
            }
        } else if (clickedInventory != null && clickedInventory.getType() == InventoryType.PLAYER) {
            if (ess.getSettings().isDirectHatAllowed() && event.getClick() == ClickType.LEFT && event.getSlot() == 39
                && event.getCursor().getType() != Material.AIR && event.getCursor().getType().getMaxDurability() == 0
                && !MaterialUtil.isSkull(event.getCursor().getType())
                && ess.getUser(event.getWhoClicked()).isAuthorized("essentials.hat")) {
                event.setCancelled(true);
                final PlayerInventory inv = (PlayerInventory) clickedInventory;
                final ItemStack head = inv.getHelmet();
                inv.setHelmet(event.getCursor());
                event.setCursor(head);
            }
        }

        if (refreshPlayer != null) {
            final Player player = refreshPlayer;
            ess.scheduleSyncDelayedTask(new Runnable() {
                @Override
                public void run() {
                    player.updateInventory();
                }
            }, 1);
        }
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
            if (invHolder != null && invHolder instanceof HumanEntity) {
                final User user = ess.getUser((Player) event.getPlayer());
                user.setInvSee(false);
                refreshPlayer = user.getBase();
            }
        }

        if (refreshPlayer != null) {
            final Player player = refreshPlayer;
            ess.scheduleSyncDelayedTask(new Runnable() {
                @Override
                public void run() {
                    player.updateInventory();
                }
            }, 1);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerFishEvent(final PlayerFishEvent event) {
        final User user = ess.getUser(event.getPlayer());
            user.updateActivityOnInteract(true);
    }

    private static boolean isEntityPickupEvent() {
        try {
            Class.forName("org.bukkit.event.entity.EntityPickupItemEvent");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private static boolean isCommandSendEvent() {
        try {
            Class.forName("org.bukkit.event.player.PlayerCommandSendEvent");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private final class PickupListenerPre1_12 implements Listener {
        @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
        public void onPlayerPickupItem(final org.bukkit.event.player.PlayerPickupItemEvent event) {
            if (ess.getSettings().getDisableItemPickupWhileAfk()) {
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
            User user = ess.getUser(event.getPlayer());

            ArrayList<String> removedCmds = new ArrayList<>(event.getCommands());

            event.getCommands().removeIf(str -> shouldHideFromUser(str, user));

            if (ess.getSettings().isDebug()) {
                removedCmds.removeAll(event.getCommands());
                ess.getLogger().info("Removed commands: " + removedCmds.toString());
            }
        }

        /**
         * Returns true if all of the following are true:
         *   - The command is a plugin command
         *   - The plugin command is from Essentials
         *   - There is no known alternative OR the alternative is overridden by Essentials
         *   - The user is not allowed to perform the given Essentials command
         */
        private boolean shouldHideFromUser(String commandLabel, User user) {
            PluginCommand command = ess.getServer().getPluginCommand(commandLabel);

            return command != null
                && command.getPlugin().getName().equals("Essentials")
                && (ess.getSettings().isCommandOverridden(commandLabel) || (ess.getAlternativeCommandsHandler().getAlternative(commandLabel) == null))
                && !user.isAuthorized("essentials." + command.getName());
        }
    }
}
