package com.earth2me.essentials;

import com.google.common.base.Preconditions;
import net.ess3.api.IUser;

import java.math.BigDecimal;
import java.util.Date;
import java.util.regex.Pattern;

public abstract class CommandFilter {

    public enum Type {
        REGEX,
        ESS
    }

    private final String name;
    private final Pattern pattern;
    private final Integer cooldown;
    private final boolean persistentCooldown;
    private final BigDecimal cost;

    public CommandFilter(String name, Pattern pattern, Integer cooldown, boolean persistentCooldown, BigDecimal cost) {
        Preconditions.checkNotNull(pattern);
        this.name = name;
        this.pattern = pattern;
        this.cooldown = cooldown;
        this.persistentCooldown = persistentCooldown;
        this.cost = cost;
    }

    public String getName() {
        return name;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public boolean hasCooldown() {
        return cooldown != null;
    }

    public Integer getCooldown() {
        return cooldown;
    }

    public boolean applyCooldownTo(IUser user) {
        if (!hasCooldown()) return false;
        final Date expiry = new Date(System.currentTimeMillis() + cooldown);
        user.addCommandCooldown(pattern, expiry, persistentCooldown);
        return true;
    }

    public boolean isPersistentCooldown() {
        return persistentCooldown;
    }

    public boolean hasCost() {
        return cost != null;
    }

    public BigDecimal getCost() {
        return cost;
    }
}
