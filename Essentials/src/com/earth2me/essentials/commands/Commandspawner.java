package com.earth2me.essentials.commands;

import com.earth2me.essentials.Mob;
import com.earth2me.essentials.Trade;
import com.earth2me.essentials.User;
import com.earth2me.essentials.utils.EnumUtil;
import com.earth2me.essentials.utils.LocationUtil;
import com.earth2me.essentials.utils.NumberUtil;
import com.earth2me.essentials.utils.StringUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.CreatureSpawner;

import java.util.Locale;


public class Commandspawner extends EssentialsCommand {
    public Commandspawner() {
        super("spawner");
    }

    @Override
    protected void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 1 || args[0].length() < 2) {
            throw new NotEnoughArgumentsException(user.tl("mobsAvailable", StringUtil.joinList(Mob.getMobList())));
        }

        final Location target = LocationUtil.getTarget(user.getBase());
        Material MOB_SPAWNER = EnumUtil.getMaterial("SPAWNER", "MOB_SPAWNER");

        if (target == null || target.getBlock().getType() != MOB_SPAWNER) {
            throw new Exception(user.tl("mobSpawnTarget"));
        }

        String name = args[0];
        int delay = 0;

        Mob mob = null;
        mob = Mob.fromName(name);
        if (mob == null) {
            throw new Exception(user.tl("invalidMob"));
        }
        if (ess.getSettings().getProtectPreventSpawn(mob.getType().toString().toLowerCase(Locale.ENGLISH))) {
            throw new Exception(user.tl("disabledToSpawnMob"));
        }
        if (!user.isAuthorized("essentials.spawner." + mob.name.toLowerCase(Locale.ENGLISH))) {
            throw new Exception(user.tl("noPermToSpawnMob"));
        }
        if (args.length > 1) {
            if (NumberUtil.isInt(args[1])) {
                delay = Integer.parseInt(args[1]);
            }
        }
        final Trade charge = new Trade("spawner-" + mob.name.toLowerCase(Locale.ENGLISH), ess);
        charge.isAffordableFor(user);
        try {
            CreatureSpawner spawner = (CreatureSpawner) target.getBlock().getState();
            spawner.setSpawnedType(mob.getType());
            spawner.setDelay(delay);
            spawner.update();
        } catch (Throwable ex) {
            throw new Exception(user.tl("mobSpawnError"), ex);
        }
        charge.charge(user);
        user.sendTl("setSpawner", mob.name);

    }
}
