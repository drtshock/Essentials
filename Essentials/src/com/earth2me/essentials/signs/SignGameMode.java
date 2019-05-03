package com.earth2me.essentials.signs;

import com.earth2me.essentials.ChargeException;
import com.earth2me.essentials.Trade;
import com.earth2me.essentials.User;
import net.ess3.api.IEssentials;
import org.bukkit.GameMode;

import java.util.Locale;


public class SignGameMode extends EssentialsSign {
    public SignGameMode() {
        super("GameMode");
    }

    @Override
    protected boolean onSignCreate(final ISign sign, final User player, final String username, final IEssentials ess) throws SignException {
        final String gamemode = sign.getLine(1);
        if (gamemode.isEmpty()) {
            sign.setLine(1, "Survival");
        }

        validateTrade(sign, 2, player, ess);

        return true;
    }

    @Override
    protected boolean onSignInteract(final ISign sign, final User player, final String username, final IEssentials ess) throws SignException, ChargeException {
        final Trade charge = getTrade(player, sign, 2, ess);
        final String mode = sign.getLine(1).trim();

        if (mode.isEmpty()) {
            throw new SignException(player.tl("invalidSignLine", 2));
        }

        charge.isAffordableFor(player);

        performSetMode(mode.toLowerCase(Locale.ENGLISH), player);
        player.sendTl("gameMode", player.tl(player.getBase().getGameMode().toString().toLowerCase(Locale.ENGLISH)), player.getDisplayName());
        Trade.log("Sign", "gameMode", "Interact", username, null, username, charge, sign.getBlock().getLocation(), ess);
        charge.charge(player);
        return true;
    }

    private void performSetMode(String mode, User player) throws SignException {
        if (mode.contains("survi") || mode.equalsIgnoreCase("0")) {
            player.getBase().setGameMode(GameMode.SURVIVAL);
        } else if (mode.contains("creat") || mode.equalsIgnoreCase("1")) {
            player.getBase().setGameMode(GameMode.CREATIVE);
        } else if (mode.contains("advent") || mode.equalsIgnoreCase("2")) {
            player.getBase().setGameMode(GameMode.ADVENTURE);
        } else if (mode.contains("spec") || mode.equalsIgnoreCase("3")) {
            player.getBase().setGameMode(GameMode.SPECTATOR);
        } else {
            throw new SignException(player.tl("invalidSignLine", 2));
        }
    }
}
