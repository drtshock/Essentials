package net.essentialsx.api.v2.events;

import net.ess3.api.IUser;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Called when a user is teleported home via the /spawn command.
 */
public class UserTeleportSpawnEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private final IUser user;
    private final String spawnGroup;
    private final Location target;
    private boolean cancelled = false;

    public UserTeleportSpawnEvent(final IUser user, final String spawnGroup, final Location target) {
        this.user = user;
        this.spawnGroup = spawnGroup;
        this.target = target;
    }

    /**
     * Returns the user who is being teleported
     *
     * @return The teleportee.
     */
    public IUser getUser() {
        return user;
    }

    /**
     * Return the group of spawn
     *
     * @return Name of group
     */
    public String getSpawnGroup() {
        return spawnGroup;
    }

    /**
     * Returns the location the user is teleporting to.
     *
     * @return Teleportation destination location.
     */
    public Location getSpawnLocation() {
        return target;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(final boolean cancelled) {
        this.cancelled = cancelled;
    }


    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
