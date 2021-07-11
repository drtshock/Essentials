package net.essentialsx.discordlink;

import com.earth2me.essentials.EssentialsConf;
import com.earth2me.essentials.IConf;

import java.io.File;

public class DiscordLinkSettings implements IConf {
    private final EssentialsDiscordLink plugin;
    private final EssentialsConf config;

    private LinkPolicy linkPolicy;

    public DiscordLinkSettings(EssentialsDiscordLink plugin) {
        this.plugin = plugin;
        this.config = new EssentialsConf(new File(plugin.getDataFolder(), "config.yml"));
        config.setTemplateName("/config.yml", EssentialsDiscordLink.class);
        reloadConfig();
    }

    public LinkPolicy getLinkPolicy() {
        return linkPolicy;
    }

    public String getInviteUrl() {
        return config.getString("discord-url", "https://discord.gg/invite-code");
    }

    public boolean isBlockUnlinkedChat() {
        return config.getBoolean("block-unlinked-chat", false);
    }

    public enum LinkPolicy {
        KICK,
        FREEZE,
        NONE;

        static LinkPolicy fromName(final String name, final LinkPolicy def) {
            for (LinkPolicy policy : values()) {
                if (policy.name().equalsIgnoreCase(name)) {
                    return policy;
                }
            }
            return def;
        }
    }

    @Override
    public void reloadConfig() {
        config.load();

        linkPolicy = LinkPolicy.fromName(config.getString("link-policy"), LinkPolicy.NONE);
    }
}
