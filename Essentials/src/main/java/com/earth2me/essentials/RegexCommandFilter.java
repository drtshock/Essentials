package com.earth2me.essentials;

import java.math.BigDecimal;
import java.util.regex.Pattern;

public class RegexCommandFilter extends CommandFilter {

    public RegexCommandFilter(String name, Pattern pattern, Integer cooldown, boolean persistentCooldown, BigDecimal cost) {
        super(name, pattern, cooldown, persistentCooldown, cost);
    }
}
