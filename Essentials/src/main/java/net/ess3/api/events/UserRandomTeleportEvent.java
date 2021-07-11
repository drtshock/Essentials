package net.ess3.api.events;

import net.ess3.api.IUser;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Called when the player uses the command /tpr
 */
public class UserRandomTeleportEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private final IUser user;
    private String name;
    private Location center;
    private double minRange;
    private double maxRange;
    private boolean cancelled = false;
    private boolean modified = false;

    public UserRandomTeleportEvent(final IUser user, final String name, final Location center, final double minRange, final double maxRange) {
        this(user, center, minRange, maxRange);
        this.name = name;
    }

    @Deprecated
    public UserRandomTeleportEvent(final IUser user, final Location center, final double minRange, final double maxRange) {
        super(!Bukkit.isPrimaryThread());
        this.user = user;
        this.center = center;
        this.minRange = minRange;
        this.maxRange = maxRange;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public IUser getUser() {
        return user;
    }

    public String getName() {
        return name;
    }

    public Location getCenter() {
        return center;
    }

    /**
     * Sets the center location to teleport from.
     *
     * @param center Center location.
     */
    public void setCenter(final Location center) {
        if (!this.center.equals(center)) {
            modified = true;
        }
        this.center = center;
    }

    public double getMinRange() {
        return minRange;
    }

    /**
     * Sets the minimum range for the teleport.
     *
     * @param minRange Minimum range.
     */
    public void setMinRange(final double minRange) {
        if (this.minRange != minRange) {
            modified = true;
        }
        this.minRange = minRange;
    }

    public double getMaxRange() {
        return maxRange;
    }

    /**
     * Sets the maximum range for the teleport.
     *
     * @param maxRange Maximum range.
     */
    public void setMaxRange(final double maxRange) {
        if (this.maxRange != maxRange) {
            modified = true;
        }
        this.maxRange = maxRange;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(final boolean b) {
        cancelled = b;
    }

    public boolean isModified() {
        return modified;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
