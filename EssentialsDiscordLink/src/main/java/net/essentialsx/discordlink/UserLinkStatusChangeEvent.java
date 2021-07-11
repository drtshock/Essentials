package net.essentialsx.discordlink;

import net.ess3.api.IUser;
import net.essentialsx.api.v2.services.discord.InteractionMember;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class UserLinkStatusChangeEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final IUser user;
    private final InteractionMember member;
    private final boolean state;

    public UserLinkStatusChangeEvent(IUser user, InteractionMember member, boolean state) {
        super(!Bukkit.isPrimaryThread());
        this.user = user;
        this.member = member;
        this.state = state;
    }

    public IUser getUser() {
        return user;
    }

    public InteractionMember getMember() {
        return member;
    }

    public boolean isLinked() {
        return state;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
