package com.earth2me.essentials.signs;

import com.earth2me.essentials.ChargeException;
import com.earth2me.essentials.Trade;
import com.earth2me.essentials.User;
import net.ess3.api.IEssentials;
import org.bukkit.ChatColor;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;


public class SignWarp extends EssentialsSign {
    public SignWarp() {
        super("Warp");
    }

    @Override
    protected boolean onSignCreate(final ISign sign, final User player, final String username, final IEssentials ess) throws SignException {
        validateTrade(sign, 3, player, ess);
        final String warpName = sign.getLine(1);

        if (warpName.isEmpty()) {
            sign.setLine(1, ChatColor.RED + "<Warp name>");
            throw new SignException(player.tl("invalidSignLine", 1));
        } else {
            try {
                ess.getWarps().getWarp(warpName);
            } catch (Exception ex) {
                throw new SignException(ex.getMessage(), ex);
            }
            final String group = sign.getLine(2);
            if ("Everyone".equalsIgnoreCase(group) || "Everybody".equalsIgnoreCase(group)) {
                sign.setLine(2, ChatColor.GREEN + "Everyone");
            }
            return true;
        }
    }

    @Override
    protected boolean onSignInteract(final ISign sign, final User player, final String username, final IEssentials ess) throws SignException, ChargeException {
        final String warpName = sign.getLine(1);
        final String group = sign.getLine(2);

        if (!group.isEmpty()) {
            if (!"§2Everyone".equals(group) && !player.inGroup(group)) {
                throw new SignException(player.tl("warpUsePermission"));
            }
        } else {
            if (ess.getSettings().getPerWarpPermission() && !player.isAuthorized("essentials.warps." + warpName)) {
                throw new SignException(player.tl("warpUsePermission"));
            }
        }

        final Trade charge = getTrade(player, sign, 3, ess);
        try {
            player.getTeleport().warp(player, warpName, charge, TeleportCause.PLUGIN);
            Trade.log("Sign", "Warp", "Interact", username, null, username, charge, sign.getBlock().getLocation(), ess);
        } catch (Exception ex) {
            throw new SignException(ex.getMessage(), ex);
        }
        return true;
    }
}
