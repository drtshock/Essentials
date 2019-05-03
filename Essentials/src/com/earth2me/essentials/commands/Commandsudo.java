package com.earth2me.essentials.commands;

import com.earth2me.essentials.ChargeException;
import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.User;
import net.ess3.api.MaxMoneyException;
import org.bukkit.Server;

import java.util.Locale;


public class Commandsudo extends EssentialsLoopCommand {
    public Commandsudo() {
        super("sudo");
    }

    @Override
    public void run(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 2) {
            throw new NotEnoughArgumentsException();
        }

        final String[] arguments = new String[args.length - 1];
        if (arguments.length > 0) {
            System.arraycopy(args, 1, arguments, 0, args.length - 1);
        }

        final String command = getFinalArg(arguments, 0);
        boolean multiple = !sender.isPlayer() ? true : ess.getUser(sender.getPlayer()).isAuthorized("essentials.sudo.multiple");

        sender.sendTl("sudoRun", args[0], command, "");
        loopOnlinePlayers(server, sender, multiple, multiple, args[0], new String[]{command});
    }

    @Override
    protected void updatePlayer(final Server server, final CommandSource sender, final User user, String[] args) throws NotEnoughArgumentsException, PlayerExemptException, ChargeException, MaxMoneyException {
        if (user.getName().equals(sender.getSender().getName())) {
            return; // Silently don't do anything.
        }

        if (user.isAuthorized("essentials.sudo.exempt") && sender.isPlayer()) {
            sender.sendTl("sudoExempt", user.getName());
            return;
        }

        if (args[0].toLowerCase(Locale.ENGLISH).startsWith("c:")) {
            user.getBase().chat(getFinalArg(args, 0).substring(2));
            return;
        }

        final String command = getFinalArg(args, 0);
        if (command != null && command.length() > 0) {
            class SudoCommandTask implements Runnable {
                @Override
                public void run() {
                    try {
                        user.getBase().chat("/" + command);
                    } catch (Exception e) {
                        sender.sendTl("errorCallingCommand", command);
                    }
                }
            }
            ess.scheduleSyncDelayedTask(new SudoCommandTask());
        }
    }
}
