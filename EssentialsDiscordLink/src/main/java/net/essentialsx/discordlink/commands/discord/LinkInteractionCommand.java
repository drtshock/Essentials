package net.essentialsx.discordlink.commands.discord;

import com.google.common.collect.ImmutableList;
import net.essentialsx.api.v2.services.discord.InteractionCommand;
import net.essentialsx.api.v2.services.discord.InteractionCommandArgument;
import net.essentialsx.api.v2.services.discord.InteractionCommandArgumentType;
import net.essentialsx.api.v2.services.discord.InteractionEvent;
import net.essentialsx.discordlink.AccountLinkManager;

import java.util.List;
import java.util.UUID;

import static com.earth2me.essentials.I18n.tl;

public class LinkInteractionCommand implements InteractionCommand {
    private final List<InteractionCommandArgument> arguments;
    private final AccountLinkManager accounts;

    public LinkInteractionCommand(final AccountLinkManager accounts) {
        this.arguments = ImmutableList.of(new InteractionCommandArgument("code", tl("discordCommandLinkArgumentCode"), InteractionCommandArgumentType.STRING, true));
        this.accounts = accounts;
    }

    @Override
    public void onCommand(InteractionEvent event) {
        if (accounts.isLinked(event.getMember().getId())) {
            event.reply(tl("discordCommandLinkHasAccount"));
            return;
        }

        final UUID uuid = accounts.getPendingUUID(event.getStringArgument("code"));
        if (uuid == null) {
            event.reply(tl("discordCommandLinkInvalidCode"));
            return;
        }

        accounts.registerAccount(uuid, event.getMember());
        event.reply(tl("discordCommandLinkLinked"));
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
        return "link";
    }

    @Override
    public String getDescription() {
        return tl("discordCommandLinkDescription");
    }

    @Override
    public List<InteractionCommandArgument> getArguments() {
        return arguments;
    }
}

