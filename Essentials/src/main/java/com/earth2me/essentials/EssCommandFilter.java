package com.earth2me.essentials;

import java.math.BigDecimal;
import java.util.regex.Pattern;

public class EssCommandFilter extends CommandFilter {

    private final String command;

    public EssCommandFilter(String name, String command, Pattern pattern, Integer cooldown, boolean persistentCooldown, BigDecimal cost) {
        super(name, pattern, cooldown, persistentCooldown, cost);
        this.command = command;
    }

    public String getCommand() {
        return command;
    }
}
