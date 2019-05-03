package com.earth2me.essentials.commands;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.User;
import com.earth2me.essentials.textreader.*;
import com.earth2me.essentials.utils.NumberUtil;
import org.bukkit.Server;

import java.util.Collections;
import java.util.List;
import java.util.Locale;


public class Commandhelp extends EssentialsCommand {
    public Commandhelp() {
        super("help");
    }

    @Override
    protected void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        IText output;
        String pageStr = args.length > 0 ? args[0] : null;
        String chapterPageStr = args.length > 1 ? args[1] : null;
        String command = commandLabel;
        final IText input = new TextInput(user.getSource(), "help", false, ess);

        if (input.getLines().isEmpty()) {
            if (NumberUtil.isInt(pageStr) || pageStr == null) {
                output = new HelpInput(user, "", ess);
            } else {
                if (pageStr.length() > 26) {
                    pageStr = pageStr.substring(0, 25);
                }
                output = new HelpInput(user, pageStr.toLowerCase(Locale.ENGLISH), ess);
                command = command.concat(" ").concat(pageStr);
                pageStr = chapterPageStr;
            }
            chapterPageStr = null;
        } else {
            user.setDisplayNick();
            output = new KeywordReplacer(input, user.getSource(), ess);
        }
        final TextPager pager = new TextPager(output);
        pager.showPage(pageStr, chapterPageStr, command, user.getSource());
    }

    @Override
    protected void run(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        sender.sendTl("helpConsole");
    }

    @Override
    protected List<String> getTabCompleteOptions(Server server, CommandSource sender, String commandLabel, String[] args) {
        if (args.length == 1) {
            return getCommands(server);
        } else {
            return Collections.emptyList();
        }
    }
}
