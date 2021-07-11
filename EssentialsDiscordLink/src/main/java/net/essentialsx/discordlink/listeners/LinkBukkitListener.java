package net.essentialsx.discordlink.listeners;

import net.essentialsx.api.v2.events.AsyncUserDataLoadEvent;
import net.essentialsx.api.v2.events.discord.DiscordMessageEvent;
import net.essentialsx.api.v2.services.discord.MessageType;
import net.essentialsx.discordlink.DiscordLinkSettings;
import net.essentialsx.discordlink.EssentialsDiscordLink;
import net.essentialsx.discordlink.UserLinkStatusChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import static com.earth2me.essentials.I18n.tl;

public class LinkBukkitListener implements Listener {
    private final EssentialsDiscordLink ess;

    public LinkBukkitListener(EssentialsDiscordLink ess) {
        this.ess = ess;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onConnect(final AsyncPlayerPreLoginEvent event) {
        if (ess.getSettings().getLinkPolicy() != DiscordLinkSettings.LinkPolicy.KICK) {
            return;
        }

        if (!ess.getLinkManager().isLinked(event.getUniqueId())) {
            String code;
            try {
                code = ess.getLinkManager().createCode(event.getUniqueId());
            } catch (IllegalArgumentException e) {
                code = e.getMessage();
            }
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, tl("discordLinkLoginKick", "/link " + code, ess.getSettings().getInviteUrl()));
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(final PlayerInteractEvent event) {
        if (ess.getSettings().getLinkPolicy() != DiscordLinkSettings.LinkPolicy.FREEZE) {
            return;
        }

        if (!ess.getLinkManager().isLinked(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onCommand(final PlayerCommandPreprocessEvent event) {
        if (ess.getSettings().getLinkPolicy() != DiscordLinkSettings.LinkPolicy.FREEZE) {
            return;
        }

        //todo maybe allowed commands
        if (!ess.getLinkManager().isLinked(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            String code;
            try {
                code = ess.getLinkManager().createCode(event.getPlayer().getUniqueId());
            } catch (IllegalArgumentException e) {
                code = e.getMessage();
            }
            event.getPlayer().sendMessage(tl("discordLinkLoginPrompt", "/link " + code, ess.getSettings().getInviteUrl()));
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onChat(final AsyncPlayerChatEvent event) {
        if (ess.getSettings().getLinkPolicy() != DiscordLinkSettings.LinkPolicy.FREEZE) {
            return;
        }

        if (!ess.getLinkManager().isLinked(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            String code;
            try {
                code = ess.getLinkManager().createCode(event.getPlayer().getUniqueId());
            } catch (IllegalArgumentException e) {
                code = e.getMessage();
            }
            event.getPlayer().sendMessage(tl("discordLinkLoginPrompt", "/link " + code, ess.getSettings().getInviteUrl()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUserDataLoad(final AsyncUserDataLoadEvent event) {
        if (ess.getSettings().getLinkPolicy() != DiscordLinkSettings.LinkPolicy.FREEZE) {
            return;
        }

        if (!ess.getLinkManager().isLinked(event.getUser().getBase().getUniqueId())) {
            event.getUser().setFreeze(true);
            String code;
            try {
                code = ess.getLinkManager().createCode(event.getUser().getBase().getUniqueId());
            } catch (IllegalArgumentException e) {
                code = e.getMessage();
            }
            event.getUser().sendMessage(tl("discordLinkLoginPrompt", "/link " + code, ess.getSettings().getInviteUrl()));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDiscordMessage(final DiscordMessageEvent event) {
        if (ess.getSettings().isBlockUnlinkedChat() && event.getType() == MessageType.DefaultTypes.CHAT && !ess.getLinkManager().isLinked(event.getUUID())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onUserLinkStatusChange(final UserLinkStatusChangeEvent event) {
        if (event.isLinked()) {
            event.getUser().setFreeze(false);
            //todo link logic for roles or whatever
            return;
        }

        //todo unlink logic for roles or whatever
        switch (ess.getSettings().getLinkPolicy()) {
            case KICK: {
                String code;
                try {
                    code = ess.getLinkManager().createCode(event.getUser().getBase().getUniqueId());
                } catch (IllegalArgumentException e) {
                    code = e.getMessage();
                }
                final String finalCode = code;
                final Runnable kickTask = () -> event.getUser().getBase().kickPlayer(tl("discordLinkLoginKick", "/link " + finalCode, ess.getSettings().getInviteUrl()));
                if (Bukkit.isPrimaryThread()) {
                    kickTask.run();
                } else {
                    ess.getEss().scheduleSyncDelayedTask(kickTask);
                }
                break;
            }
            case FREEZE: {
                String code;
                try {
                    code = ess.getLinkManager().createCode(event.getUser().getBase().getUniqueId());
                } catch (IllegalArgumentException e) {
                    code = e.getMessage();
                }
                event.getUser().sendMessage(tl("discordLinkLoginPrompt", "/link " + code, ess.getSettings().getInviteUrl()));
                event.getUser().setFreeze(true);
                break;
            }
            default: {
                break;
            }
        }
    }
}
