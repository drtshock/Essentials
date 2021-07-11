package com.earth2me.essentials.spawn;

import com.earth2me.essentials.Kit;
import com.earth2me.essentials.OfflinePlayer;
import com.earth2me.essentials.RandomTeleport;
import com.earth2me.essentials.User;
import com.earth2me.essentials.textreader.IText;
import com.earth2me.essentials.textreader.KeywordReplacer;
import com.earth2me.essentials.textreader.SimpleTextPager;
import com.earth2me.essentials.utils.VersionUtil;
import net.ess3.api.IEssentials;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.earth2me.essentials.I18n.tl;

class EssentialsSpawnPlayerListener implements Listener {
    private static final Logger logger = Logger.getLogger("EssentialsSpawn");
    private final transient IEssentials ess;
    private final transient SpawnStorage spawns;

    EssentialsSpawnPlayerListener(final IEssentials ess, final SpawnStorage spawns) {
        super();
        this.ess = ess;
        this.spawns = spawns;
    }

    void onPlayerRespawn(final PlayerRespawnEvent event) {
        final User user = ess.getUser(event.getPlayer());

        if (user.isJailed() && user.getJail() != null && !user.getJail().isEmpty()) {
            return;
        }

        if (VersionUtil.getServerBukkitVersion().isHigherThanOrEqualTo(VersionUtil.v1_16_1_R01) && event.isAnchorSpawn() && ess.getSettings().isRespawnAtAnchor()) {
            return;
        }

        if (ess.getSettings().getRespawnAtHome()) {
            final Location home;

            Location bed = null;
            if (ess.getSettings().isRespawnAtBed()) {
                // cannot nuke this sync load due to the event being sync so it would hand either way
                bed = user.getBase().getBedSpawnLocation();
            }

            if (bed != null) {
                home = bed;
            } else {
                home = user.getHome(user.getLocation());
            }

            if (home != null) {
                event.setRespawnLocation(home);
                return;
            }
        } else if (ess.getSettings().isRandomRespawn()) {
            final String name = ess.getSettings().getRandomRespawnLocation();
            final RandomTeleport randomTeleport = ess.getRandomTeleport();
            randomTeleport.getRandomLocation(name).thenAccept(location -> {
                final CompletableFuture<Boolean> future = new CompletableFuture<>();
                user.getAsyncTeleport().now(location, false, PlayerTeleportEvent.TeleportCause.COMMAND, future);
            });
        }
        final Location spawn = spawns.getSpawn(user.getGroup());
        if (spawn != null) {
            event.setRespawnLocation(spawn);
        }
    }

    void onPlayerJoin(final PlayerJoinEvent event) {
        ess.runTaskAsynchronously(() -> delayedJoin(event.getPlayer()));
    }

    private void delayedJoin(final Player player) {
        if (player.hasPlayedBefore()) {
            logger.log(Level.FINE, "Old player join");
            final List<String> spawnOnJoinGroups = ess.getSettings().getSpawnOnJoinGroups();
            if (!spawnOnJoinGroups.isEmpty()) {
                final User user = ess.getUser(player);

                if (ess.getSettings().isUserInSpawnOnJoinGroup(user) && !user.isAuthorized("essentials.spawn-on-join.exempt")) {
                    ess.scheduleSyncDelayedTask(() -> {
                        final Location spawn = spawns.getSpawn(user.getGroup());
                        if (spawn == null) {
                            return;
                        }
                        final CompletableFuture<Boolean> future = new CompletableFuture<>();
                        future.exceptionally(e -> {
                            ess.showError(user.getSource(), e, "spawn-on-join");
                            return false;
                        });
                        user.getAsyncTeleport().nowUnsafe(spawn, TeleportCause.PLUGIN, future);
                    });
                }
            }

            return;
        }

        final User user = ess.getUser(player);

        if (!"none".equalsIgnoreCase(ess.getSettings().getNewbieSpawn())) {
            ess.scheduleSyncDelayedTask(new NewPlayerTeleport(user), 1L);
        }

        ess.scheduleSyncDelayedTask(() -> {
            if (!user.getBase().isOnline()) {
                return;
            }

            //This method allows for multiple line player announce messages using multiline yaml syntax #EasterEgg
            if (ess.getSettings().getAnnounceNewPlayers()) {
                final IText output = new KeywordReplacer(ess.getSettings().getAnnounceNewPlayerFormat(), user.getSource(), ess);
                final SimpleTextPager pager = new SimpleTextPager(output);

                for (final String line : pager.getLines()) {
                    ess.broadcastMessage(user, line);
                }
            }

            final String kitName = ess.getSettings().getNewPlayerKit();
            if (!kitName.isEmpty()) {
                try {
                    final Kit kit = new Kit(kitName.toLowerCase(Locale.ENGLISH), ess);
                    kit.expandItems(user);
                } catch (final Exception ex) {
                    logger.log(Level.WARNING, ex.getMessage());
                }
            }

            logger.log(Level.FINE, "New player join");
        }, 2L);
    }

    private class NewPlayerTeleport implements Runnable {
        private final transient User user;

        NewPlayerTeleport(final User user) {
            this.user = user;
        }

        @Override
        public void run() {
            if (user.getBase() instanceof OfflinePlayer || !user.getBase().isOnline()) {
                return;
            }

            final Location spawn = spawns.getSpawn(ess.getSettings().getNewbieSpawn());
            if (spawn != null) {
                final CompletableFuture<Boolean> future = new CompletableFuture<>();
                future.exceptionally(e -> {
                    logger.log(Level.WARNING, tl("teleportNewPlayerError"), e);
                    return false;
                });
                user.getAsyncTeleport().now(spawn, false, TeleportCause.PLUGIN, future);
            }
        }
    }
}
