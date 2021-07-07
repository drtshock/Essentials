package net.essentialsx.api.v2.events;

import net.ess3.api.IUser;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Called before a user is teleported to spawn via the /spawn command.
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
     * @return The user who is being teleported to spawn.
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
     * The spawn location of the {@link #getUser() user's} {@link #getSpawnGroup() group}.
     * @return The spawn location of the user's group.
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
