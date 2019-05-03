package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import org.bukkit.Server;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class Commanddeljail extends EssentialsCommand {
    public Commanddeljail() {
        super("deljail");
    }

    @Override
    protected void run(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }

        if (ess.getJails().getJail(args[0]) == null) {
            throw new Exception(sender.tl("jailNotExist"));
        }

        ess.getJails().removeJail(args[0]);
        sender.sendTl("deleteJail", args[0]);
    }

    @Override
    protected List<String> getTabCompleteOptions(final Server server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            try {
                return new ArrayList<>(ess.getJails().getList());
            } catch (Exception e) {
                return Collections.emptyList();
            }
        } else {
            return Collections.emptyList();
        }
    }
}
