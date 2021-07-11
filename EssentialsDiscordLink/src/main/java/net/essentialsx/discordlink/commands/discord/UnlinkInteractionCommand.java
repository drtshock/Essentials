package net.essentialsx.discordlink.commands.discord;

import net.essentialsx.api.v2.services.discord.InteractionCommand;
import net.essentialsx.api.v2.services.discord.InteractionCommandArgument;
import net.essentialsx.api.v2.services.discord.InteractionEvent;
import net.essentialsx.discordlink.AccountLinkManager;

import java.util.List;

import static com.earth2me.essentials.I18n.tl;

public class UnlinkInteractionCommand implements InteractionCommand {
    private final AccountLinkManager accounts;

    public UnlinkInteractionCommand(final AccountLinkManager accounts) {
        this.accounts = accounts;
    }

    @Override
    public void onCommand(InteractionEvent event) {
        if (!accounts.removeAccount(event.getMember())) {
            event.reply(tl("discordCommandUnlinkInvalidCode"));
            return;
        }
        event.reply(tl("discordCommandUnlinkUnlinked"));
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
        return "unlink";
    }

    @Override
    public String getDescription() {
        return tl("discordCommandUnlinkDescription");
    }

    @Override
    public List<InteractionCommandArgument> getArguments() {
        return null;
    }
}
