package net.essentialsx.discord.interactions;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.essentialsx.api.v2.services.discord.InteractionMember;
import net.essentialsx.discord.util.DiscordUtil;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class InteractionMemberImpl implements InteractionMember {
    private final Member member;

    public InteractionMemberImpl(Member member) {
        this.member = member;
    }

    @Override
    public String getName() {
        return member.getUser().getName();
    }

    @Override
    public String getDiscriminator() {
        return member.getUser().getDiscriminator();
    }

    @Override
    public String getAsMention() {
        return member.getAsMention();
    }

    @Override
    public String getEffectiveName() {
        return member.getEffectiveName();
    }

    @Override
    public String getNickname() {
        return member.getNickname();
    }

    @Override
    public String getId() {
        return member.getId();
    }

    @Override
    public boolean isAdmin() {
        return member.hasPermission(Permission.ADMINISTRATOR);
    }

    @Override
    public boolean hasRoles(List<String> roleDefinitions) {
        return DiscordUtil.hasRoles(member, roleDefinitions);
    }

    public Member getJdaObject() {
        return member;
    }

    @Override
    public CompletableFuture<Boolean> sendPrivateMessage(String content) {
        final CompletableFuture<Boolean> future = new CompletableFuture<>();
        final CompletableFuture<PrivateChannel> privateFuture = member.getUser().openPrivateChannel().submit();
        privateFuture.thenCompose(privateChannel -> privateChannel.sendMessage(content).submit())
                .whenComplete((m, error) -> {
                    if (error != null) {
                        future.complete(false);
                        return;
                    }
                    future.complete(true);
                });
        return future;
    }
}
