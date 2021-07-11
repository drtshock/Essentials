package net.essentialsx.api.v2.events;

import com.earth2me.essentials.IUser;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Called when a player accepts or denies a teleport.
 */
public class TeleportRequestResponseEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private final net.ess3.api.IUser requestee;
    private final net.ess3.api.IUser requester;
    private final IUser.TpaRequest tpaRequest;
    private final boolean accept;
    private boolean canceled = false;

    public TeleportRequestResponseEvent(net.ess3.api.IUser requestee, net.ess3.api.IUser requester, IUser.TpaRequest tpaRequest, boolean accept) {
        this.requestee = requestee;
        this.requester = requester;
        this.tpaRequest = tpaRequest;
        this.accept = accept;
    }

    /**
     * Gets the user who is accepting/denying this teleport request.
     * @return the user accepting/denying the request.
     */
    public net.ess3.api.IUser getRequestee() {
        return requestee;
    }

    /**
     * Gets the user who submitted this teleport request.
     * @return the user who sent the request.
     */
    public net.ess3.api.IUser getRequester() {
        return requester;
    }

    /**
     * Gets information about this teleport request.
     * @return the {@link com.earth2me.essentials.IUser.TpaRequest} object of this event.
     */
    public IUser.TpaRequest getTpaRequest() {
        return tpaRequest;
    }

    /**
     * Whether or not the request has been accepted.
     * @return true if accepted, false if denied.
     */
    public boolean isAccept() {
        return accept;
    }

    /**
     * Whether or not the request has been denied.
     * @return true if denied, false if accepted.
     */
    public boolean isDeny() {
        return !isAccept();
    }

    @Override
    public boolean isCancelled() {
        return canceled;
    }

    /**
     * Sets whether or not to cancel this teleport request.
     * Note that cancelling this event will not show a message to users about the cancellation.
     * @param cancel whether or not to cancel this teleport request.
     */
    @Override
    public void setCancelled(boolean cancel) {
        this.canceled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
