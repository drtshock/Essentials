package net.ess3.api.events;

import com.earth2me.essentials.signs.EssentialsSign;
import net.ess3.api.IUser;

/**
 * Fired when an Essentials sign is broken.
 *
 * This is primarily intended for use with EssentialsX's sign abstraction - external plugins should not listen on this event.
 */
public class SignBreakEvent extends SignEvent {
    public SignBreakEvent(final EssentialsSign.ISign sign, final EssentialsSign essSign, final IUser user) {
        super(sign, essSign, user);
    }
}
