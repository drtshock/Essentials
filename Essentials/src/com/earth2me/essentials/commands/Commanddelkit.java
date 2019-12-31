package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.Kit;

import static com.earth2me.essentials.I18n.tl;


public class Commanddelkit extends EssentialsCommand {
    public Commanddelkit() {
        super("delkit");
    }

    @Override
    public void run(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1) {
            final String kitList = ess.getKits().listKits(ess, null);
            sender.sendMessage(kitList.length() > 0 ? tl("kits", kitList) : tl("noKits"));
            throw new NoChargeException();
        }
        final String kitName = args[0];
        final Kit kit = new Kit(kitName, ess);

        }

        ess.getKits().removeKit(kitName);
        sender.sendMessage(tl("deleteKit", kit));
    }

    @Override
    protected List<String> getTabCompleteOptions(final Server server, final CommandSource sender, final String commandLabel, final String[] args) {
        if (args.length == 1) {
            return Lists.newArrayList(ess.getKits().getKits().getKeys(false));
        }
        return Collections.emptyList();
    }
}
