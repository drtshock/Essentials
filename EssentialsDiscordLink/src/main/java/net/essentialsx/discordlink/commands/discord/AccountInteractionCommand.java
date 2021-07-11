package net.essentialsx.discordlink.commands.discord;

import com.google.common.collect.ImmutableList;
import net.ess3.api.IUser;
import net.essentialsx.api.v2.services.discord.InteractionCommand;
import net.essentialsx.api.v2.services.discord.InteractionCommandArgument;
import net.essentialsx.api.v2.services.discord.InteractionCommandArgumentType;
import net.essentialsx.api.v2.services.discord.InteractionEvent;
import net.essentialsx.api.v2.services.discord.InteractionMember;
import net.essentialsx.discordlink.AccountLinkManager;

import java.util.List;

import static com.earth2me.essentials.I18n.tl;

public class AccountInteractionCommand implements InteractionCommand {
    private final List<InteractionCommandArgument> arguments;
    private final AccountLinkManager accounts;

    public AccountInteractionCommand(AccountLinkManager accounts) {
        this.arguments = ImmutableList.of(new InteractionCommandArgument("user", tl("discordCommandAccountArgumentUser"), InteractionCommandArgumentType.USER, false));
        this.accounts = accounts;
    }

    @Override
    public boolean isDisabled() {
        return false;
    }

    @Override
    public boolean isEphemeral() {
        return true;
    }

    @Override
    public String getName() {
        return "account";
    }

    @Override
    public String getDescription() {
        return tl("discordCommandAccountDescription");
    }

    @Override
    public List<InteractionCommandArgument> getArguments() {
        return arguments;
    }

    @Override
    public void onCommand(InteractionEvent event) {
        final InteractionMember userArg = event.getUserArgument("user");
        final InteractionMember effectiveUser = userArg == null ? event.getMember() : userArg;
        final IUser user = accounts.getUser(effectiveUser.getId());
        if (user == null) {
            event.reply(tl(event.getMember().getId().equals(effectiveUser.getId()) ? "discordCommandAccountResponseNotLinked" : "discordCommandAccountResponseNotLinkedOther", effectiveUser.getAsMention()));
            return;
        }

        if (event.getMember().getId().equals(effectiveUser.getId())) {
            event.reply(tl("discordCommandAccountResponseLinked", user.getName()));
            return;
        }
        event.reply(tl("discordCommandAccountResponseLinkedOther", effectiveUser.getAsMention(), user.getName()));
    }
}
