package com.earth2me.essentials;

import net.ess3.api.IEssentials;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static com.earth2me.essentials.I18n.tl;


public class EssentialsEntityListener implements Listener {
    private static final Logger LOGGER = Logger.getLogger("Essentials");
    private static final transient Pattern powertoolPlayer = Pattern.compile("\\{player\\}");
    private final IEssentials ess;

    public EssentialsEntityListener(IEssentials ess) {
        this.ess = ess;
    }

    // This method does something undocumented reguarding certain bucket types #EasterEgg
    @EventHandler(priority = EventPriority.LOW)
    public void onEntityDamage(final EntityDamageByEntityEvent event) {
        final Entity eAttack = event.getDamager();
        final Entity eDefend = event.getEntity();
        if (eAttack instanceof Player) {
            final User attacker = ess.getUser((Player) eAttack);
            if (eDefend instanceof Player) {
                onPlayerVsPlayerDamage(event, (Player) eDefend, attacker);
            } else if (eDefend instanceof Ageable) {
                final ItemStack hand = attacker.getBase().getItemInHand();
                if (ess.getSettings().isMilkBucketEasterEggEnabled()
                        && hand != null && hand.getType() == Material.MILK_BUCKET) {
                    ((Ageable) eDefend).setBaby();
                    hand.setType(Material.BUCKET);
                    attacker.getBase().setItemInHand(hand);
                    attacker.getBase().updateInventory();
                    event.setCancelled(true);
                }
            }
            attacker.updateActivityOnInteract(true);
        } else if (eAttack instanceof Projectile && eDefend instanceof Player) {
            final Projectile projectile = (Projectile) event.getDamager();
            //This should return a ProjectileSource on 1.7.3 beta +
            final Object shooter = projectile.getShooter();
            if (shooter instanceof Player) {
                final User attacker = ess.getUser((Player) shooter);
                onPlayerVsPlayerDamage(event, (Player) eDefend, attacker);
                attacker.updateActivityOnInteract(true);
            }
        }
    }

    private void onPlayerVsPlayerDamage(final EntityDamageByEntityEvent event, final Player defender, final User attacker) {
        if (ess.getSettings().getLoginAttackDelay() > 0 && (System.currentTimeMillis() < (attacker.getLastLogin() + ess.getSettings().getLoginAttackDelay())) && !attacker.isAuthorized("essentials.pvpdelay.exempt")) {
            event.setCancelled(true);
        }

        if (!defender.equals(attacker.getBase()) && (attacker.hasInvulnerabilityAfterTeleport() || ess.getUser(defender).hasInvulnerabilityAfterTeleport())) {
            event.setCancelled(true);
        }

        if (attacker.isGodModeEnabled() && !attacker.isAuthorized("essentials.god.pvp")) {
            event.setCancelled(true);
        }

        if (attacker.isHidden() && !attacker.isAuthorized("essentials.vanish.pvp")) {
            event.setCancelled(true);
        }

        if (attacker.arePowerToolsEnabled()) {
            onPlayerVsPlayerPowertool(event, defender, attacker);
        }
    }

    private void onPlayerVsPlayerPowertool(final EntityDamageByEntityEvent event, final Player defender, final User attacker) {
        final List<String> commandList = attacker.getPowertool(attacker.getBase().getItemInHand());
        if (commandList != null && !commandList.isEmpty()) {
            for (final String tempCommand : commandList) {
                final String command = powertoolPlayer.matcher(tempCommand).replaceAll(defender.getName());
                if (command != null && !command.isEmpty() && !command.equals(tempCommand)) {

                    class PowerToolInteractTask implements Runnable {
                        @Override
                        public void run() {
                            attacker.getBase().chat("/" + command);
                            LOGGER.log(Level.INFO, String.format("[PT] %s issued server command: /%s", attacker.getName(), command));
                        }
                    }
                    ess.scheduleSyncDelayedTask(new PowerToolInteractTask());

                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamage(final EntityDamageEvent event) {
        if (event.getEntity() instanceof Player && ess.getUser((Player) event.getEntity()).isGodModeEnabled()) {
            final Player player = (Player) event.getEntity();
            player.setFireTicks(0);
            player.setRemainingAir(player.getMaximumAir());
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityCombust(final EntityCombustEvent event) {
        if (event.getEntity() instanceof Player && ess.getUser((Player) event.getEntity()).isGodModeEnabled()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityCombustByEntity(final EntityCombustByEntityEvent event) {
        if (event.getCombuster() instanceof Arrow && event.getEntity() instanceof Player) {
            Arrow combuster = (Arrow) event.getCombuster();
            if (combuster.getShooter() instanceof Player) {
                final User srcCombuster = ess.getUser(((Player) combuster.getShooter()).getUniqueId());
                if (srcCombuster.isGodModeEnabled() && !srcCombuster.isAuthorized("essentials.god.pvp")) {
                    event.setCancelled(true);
                }
                if (srcCombuster.isHidden() && !srcCombuster.isAuthorized("essentials.vanish.pvp")) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeathEvent(final PlayerDeathEvent event) {
        final User user = ess.getUser(event.getEntity());
        if (user.isAuthorized("essentials.back.ondeath") && !ess.getSettings().isCommandDisabled("back")) {
            user.setLastLocation();
            user.sendMessage(user.tl( "backAfterDeath"));
        }
        if (!ess.getSettings().areDeathMessagesEnabled()) {
            event.setDeathMessage("");
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerDeathExpEvent(final PlayerDeathEvent event) {
        final User user = ess.getUser(event.getEntity());
        if (user.isAuthorized("essentials.keepxp")) {
            event.setKeepLevel(true);
            event.setDroppedExp(0);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onFoodLevelChange(final FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            final User user = ess.getUser((Player) event.getEntity());
            if (user.isGodModeEnabled()) {
                if (user.isGodModeEnabledRaw()) {
                    user.getBase().setFoodLevel(20);
                    user.getBase().setSaturation(10);
                }
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityRegainHealth(final EntityRegainHealthEvent event) {
        if (event.getRegainReason() == RegainReason.SATIATED && event.getEntity() instanceof Player && ess.getUser((Player) event.getEntity()).isAfk() && ess.getSettings().getFreezeAfkPlayers()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPotionSplashEvent(final PotionSplashEvent event) {
        for (LivingEntity entity : event.getAffectedEntities()) {
            if (entity instanceof Player && ess.getUser((Player) entity).isGodModeEnabled()) {
                event.setIntensity(entity, 0d);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player) {
            final User user = ess.getUser((Player) event.getEntity());
            if (user.isAfk()) {
                user.updateActivityOnInteract(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getTarget() instanceof Player) {
            final User user = ess.getUser((Player) event.getTarget());
            if (user.isVanished()) {
                event.setCancelled(true);
            }
        }
    }
}
