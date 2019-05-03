package com.earth2me.essentials.commands;

import com.google.common.collect.Lists;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.Mob;
import com.earth2me.essentials.User;
import org.bukkit.Chunk;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

// This could be rewritten in a simpler form if we made a mapping of all Entity names to their types (which would also provide possible mod support)

public class Commandremove extends EssentialsCommand {
    public Commandremove() {
        super("remove");
    }

    @Override
    protected void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        World world = user.getWorld();
        int radius = 0;
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }
        if (args.length >= 2) {
            try {
                radius = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                world = ess.getWorld(args[1]);
            }
        }
        if (args.length >= 3) {
            // This is to prevent breaking the old syntax
            radius = 0;
            world = ess.getWorld(args[2]);
        }
        parseCommand(server, user.getSource(), args, world, radius);

    }

    @Override
    protected void run(final Server server, final CommandSource sender, final String commandLabel, final String[] args) throws Exception {
        if (args.length < 2) {
            throw new NotEnoughArgumentsException();
        }
        World world = ess.getWorld(args[1]);
        parseCommand(server, sender, args, world, 0);
    }

    private void parseCommand(Server server, CommandSource sender, String[] args, World world, int radius) throws Exception {
        List<String> types = new ArrayList<>();
        List<String> customTypes = new ArrayList<>();

        if (world == null) {
            throw new Exception(sender.tl("invalidWorld"));
        }

        if (args[0].contentEquals("*") || args[0].contentEquals("all")) {
            types.add(0, "ALL");
        } else {
            for (String entityType : args[0].split(",")) {
                ToRemove toRemove;
                try {
                    toRemove = ToRemove.valueOf(entityType.toUpperCase(Locale.ENGLISH));
                } catch (Exception e) {
                    try {
                        toRemove = ToRemove.valueOf(entityType.concat("S").toUpperCase(Locale.ENGLISH));
                    } catch (Exception ee) {
                        toRemove = ToRemove.CUSTOM;
                        customTypes.add(entityType);
                    }
                }
                types.add(toRemove.toString());
            }
        }
        removeHandler(sender, types, customTypes, world, radius);
    }

    private void removeHandler(CommandSource sender, List<String> types, List<String> customTypes, World world, int radius) {
        int removed = 0;
        if (radius > 0) {
            radius *= radius;
        }

        ArrayList<ToRemove> removeTypes = new ArrayList<>();
        ArrayList<Mob> customRemoveTypes = new ArrayList<>();

        for (String s : types) {
            removeTypes.add(ToRemove.valueOf(s));
        }

        boolean warnUser = false;

        for (String s : customTypes) {
            Mob mobType = Mob.fromName(s);
            if (mobType == null) {
                warnUser = true;
            } else {
                customRemoveTypes.add(mobType);
            }
        }

        if (warnUser) {
            sender.sendTl("invalidMob");
        }

        for (Chunk chunk : world.getLoadedChunks()) {
            for (Entity e : chunk.getEntities()) {
                if (radius > 0) {
                    if (sender.getPlayer().getLocation().distanceSquared(e.getLocation()) > radius) {
                        continue;
                    }
                }
                if (e instanceof HumanEntity) {
                    continue;
                }

                for (ToRemove toRemove : removeTypes) {

                    // We should skip any TAMED animals unless we are specifially targetting them.
                    if (e instanceof Tameable && ((Tameable) e).isTamed() && !removeTypes.contains(ToRemove.TAMED)) {
                        continue;
                    }

                    // We should skip any NAMED animals unless we are specifially targetting them.
                    if (e instanceof LivingEntity && e.getCustomName() != null && !removeTypes.contains(ToRemove.NAMED)) {
                        continue;
                    }

                    switch (toRemove) {
                        case TAMED:
                            if (e instanceof Tameable && ((Tameable) e).isTamed()) {
                                e.remove();
                                removed++;
                            }
                            break;
                        case NAMED:
                            if (e instanceof LivingEntity && e.getCustomName() != null) {
                                e.remove();
                                removed++;
                            }
                            break;
                        case DROPS:
                            if (e instanceof Item) {
                                e.remove();
                                removed++;
                            }
                            break;
                        case ARROWS:
                            if (e instanceof Projectile) {
                                e.remove();
                                removed++;
                            }
                            break;
                        case BOATS:
                            if (e instanceof Boat) {
                                e.remove();
                                removed++;
                            }
                            break;
                        case MINECARTS:
                            if (e instanceof Minecart) {
                                e.remove();
                                removed++;
                            }
                            break;
                        case XP:
                            if (e instanceof ExperienceOrb) {
                                e.remove();
                                removed++;
                            }
                            break;
                        case PAINTINGS:
                            if (e instanceof Painting) {
                                e.remove();
                                removed++;
                            }
                            break;
                        case ITEMFRAMES:
                            if (e instanceof ItemFrame) {
                                e.remove();
                                removed++;
                            }
                            break;
                        case ENDERCRYSTALS:
                            if (e instanceof EnderCrystal) {
                                e.remove();
                                removed++;
                            }
                            break;
                        case AMBIENT:
                            if (e instanceof Flying) {
                                e.remove();
                                removed++;
                            }
                            break;
                        case HOSTILE:
                        case MONSTERS:
                            if (e instanceof Monster || e instanceof ComplexLivingEntity || e instanceof Flying || e instanceof Slime) {
                                e.remove();
                                removed++;
                            }
                            break;
                        case PASSIVE:
                        case ANIMALS:
                            if (e instanceof Animals || e instanceof NPC || e instanceof Snowman || e instanceof WaterMob || e instanceof Ambient) {
                                e.remove();
                                removed++;
                            }
                            break;
                        case MOBS:
                            if (e instanceof Animals || e instanceof NPC || e instanceof Snowman || e instanceof WaterMob || e instanceof Monster || e instanceof ComplexLivingEntity || e instanceof Flying || e instanceof Slime || e instanceof Ambient) {
                                e.remove();
                                removed++;
                            }
                            break;
                        case ENTITIES:
                        case ALL:
                            if (e instanceof Entity) {
                                e.remove();
                                removed++;
                            }
                            break;
                        case CUSTOM:
                            for (Mob type : customRemoveTypes) {
                                if (e.getType() == type.getType()) {
                                    e.remove();
                                    removed++;
                                }
                            }
                            break;
                    }
                }
            }
        }
        sender.sendTl("removed", removed);
    }

    @Override
    protected List<String> getTabCompleteOptions(Server server, CommandSource sender, String commandLabel, String[] args) {
        if (args.length == 1) {
            List<String> options = Lists.newArrayList();
            for (ToRemove toRemove : ToRemove.values()) {
                options.add(toRemove.name().toLowerCase(Locale.ENGLISH));
            }
            return options;
        } else if (args.length == 2) {
            List<String> worlds = Lists.newArrayList();
            for (World world : server.getWorlds()) {
                worlds.add(world.getName());
            }
            return worlds;
        } else {
            return Collections.emptyList();
        }
    }

    private enum ToRemove {
        DROPS,
        ARROWS,
        BOATS,
        MINECARTS,
        XP,
        PAINTINGS,
        ITEMFRAMES,
        ENDERCRYSTALS,
        HOSTILE,
        MONSTERS,
        PASSIVE,
        ANIMALS,
        AMBIENT,
        MOBS,
        ENTITIES,
        ALL,
        CUSTOM,
        TAMED,
        NAMED
    }
}