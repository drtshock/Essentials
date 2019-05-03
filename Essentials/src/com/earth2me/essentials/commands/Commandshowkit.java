package com.earth2me.essentials.commands;

import com.earth2me.essentials.Kit;
import com.earth2me.essentials.User;
import org.bukkit.Server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class Commandshowkit extends EssentialsCommand {

    public Commandshowkit() {
        super("showkit");
    }

    @Override
    public void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length != 1) {
            throw new NotEnoughArgumentsException();
        }

        final String[] kits = args[0].toLowerCase(Locale.ENGLISH).split(",");
        for (final String kitName : kits) {
            Kit kit = new Kit(kitName, ess);
            user.sendTl("kitContains", kitName);
            for (String s : kit.getItems()) {
                user.sendTl("kitItem", s);
            }
        }
    }

    @Override
    protected List<String> getTabCompleteOptions(final Server server, final User user, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return new ArrayList<>(ess.getKits().getKits().getKeys(false)); // TODO: Move this to its own method
        } else {
            return Collections.emptyList();
        }
    }
}
