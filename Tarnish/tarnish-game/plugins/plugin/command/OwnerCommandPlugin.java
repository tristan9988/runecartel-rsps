package plugin.command;

import com.osroyale.Config;
import com.osroyale.content.activity.randomevent.impl.MimeEvent;
import com.osroyale.content.bloodmoney.BloodMoneyChest;
import com.osroyale.content.bot.PlayerBot;
import com.osroyale.content.bot.botclass.impl.MaxStrengthMelee;
import com.osroyale.content.bot.objective.BotObjective;
import com.osroyale.content.consume.FoodData;
import com.osroyale.content.consume.PotionData;
import com.osroyale.content.dialogue.DialogueFactory;
import com.osroyale.content.tittle.PlayerTitle;
import com.osroyale.game.Graphic;
import com.osroyale.game.plugin.extension.CommandExtension;
import com.osroyale.game.task.Task;
import com.osroyale.game.world.Interactable;
import com.osroyale.game.world.World;
import com.osroyale.game.world.entity.combat.hit.Hit;
import com.osroyale.game.world.entity.combat.strategy.player.special.CombatSpecial;
import com.osroyale.game.world.entity.mob.UpdateFlag;
import com.osroyale.game.world.entity.mob.npc.Npc;
import com.osroyale.game.world.entity.mob.npc.definition.NpcDefinition;
import com.osroyale.game.world.entity.mob.player.*;
import com.osroyale.game.world.entity.mob.player.command.Command;
import com.osroyale.game.world.entity.mob.player.command.CommandParser;
import com.osroyale.game.world.entity.mob.player.profile.ProfileRepository;
import com.osroyale.game.world.entity.skill.Skill;
import com.osroyale.game.world.items.Item;
import com.osroyale.game.world.position.Area;
import com.osroyale.game.world.position.Position;
import com.osroyale.net.packet.out.SendInputAmount;
import com.osroyale.net.packet.out.SendInputMessage;
import com.osroyale.net.packet.out.SendMessage;
import com.osroyale.net.packet.out.SendString;
import com.osroyale.util.RandomUtils;
import com.osroyale.util.StringUtils;
import com.osroyale.util.Utility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class OwnerCommandPlugin extends CommandExtension {
    @Override
    protected void register() {
        commands.add(new Command("unipmute") {
            @Override
            public void execute(Player player, CommandParser parser) {
                if (parser.hasNext()) {
                    final String name = parser.nextLine();
                    if (World.search(name).isPresent()) {
                        final Player target = World.search(name).get();
                        if (player == target) {
                            return;
                        }

                        if (IPMutedPlayers.unIpMute(target.lastHost)) {
                            player.send(new SendMessage("<col=ff0000>" + target.getUsername() + " has been un-ipmuted."));
                        }
                    } else {
                        player.send(new SendMessage("The player '" + name + "' either doesn't exist, or is offline."));
                    }
                }
            }
        });

        commands.add(new Command("ipmute") {
            @Override
            public void execute(Player player, CommandParser parser) {
                if (parser.hasNext()) {
                    final String name = parser.nextLine();
                    if (World.search(name).isPresent()) {
                        final Player target = World.search(name).get();
                        if (player == target) {
                            player.send(new SendMessage("You cannot ip-mute yourself."));
                            return;
                        }

                        for (Player p : World.getPlayers()) {
                            if (p == null) {
                                continue;
                            }

                            if (p.lastHost.equalsIgnoreCase(target.lastHost)) {
                                player.send(new SendMessage("<col=ff0000>" + p.getUsername() + " has been ip-muted."));
                            }
                        }

                        IPMutedPlayers.ipMute(target.lastHost);
                    } else {
                        player.send(new SendMessage("The player '" + name + "' either doesn't exist, or is offline."));
                    }
                }
            }
        });

        commands.add(new Command("ipban") {
            @Override
            public void execute(Player player, CommandParser parser) {
                if (parser.hasNext()) {
                    final String name = parser.nextLine();
                    if (World.search(name).isPresent()) {
                        final Player target = World.search(name).get();
                        if (player == target) {
                            player.send(new SendMessage("You cannot ip-ban yourself."));
                            return;
                        }

                        for (Player p : World.getPlayers()) {
                            if (p == null) {
                                continue;
                            }

                            if (p.lastHost.equalsIgnoreCase(target.lastHost)) {
                                player.send(new SendMessage("<col=ff0000>" + p.getUsername() + " has been ip-banned."));
                                p.logout(true);
                            }
                        }

                        IPBannedPlayers.ipBan(target.lastHost);
                    } else {
                        player.send(new SendMessage("The player '" + name + "' either doesn't exist, or is offline."));
                    }
                }
            }
        });

        commands.add(new Command("ban") {
            @Override
            public void execute(Player player, CommandParser parser) {
                if (parser.hasNext()) {
                    final String name = parser.nextLine();
                    if (World.search(name).isPresent()) {
                        final Player target = World.search(name).get();
                        if (player == target) {
                            player.send(new SendMessage("You cannot ban yourself."));
                            return;
                        }

                        final String username = StringUtils.capitalize(target.getUsername());
                        player.send(new SendMessage("<col=ff0000>" + username + " has been banned."));
                        target.logout(true);
                        BannedPlayers.ban(username);
                    } else {
                        player.send(new SendMessage("The player '" + name + "' either doesn't exist, or is offline."));
                    }
                }
            }
        });

        commands.add(new Command("unban") {
            @Override
            public void execute(Player player, CommandParser parser) {
                if (parser.hasNext()) {
                    final String username = parser.nextLine();

                    if (BannedPlayers.unban(username)) {
                        player.send(new SendMessage("<col=ff0000>" + username + " has been un-banned."));
                    }
                }
            }
        });

        commands.add(new Command("checkaccs") {
            @Override
            public void execute(Player player, CommandParser parser) {
                    Player other = player.managing.get();
                    List<String> list = ProfileRepository.getRegistry(other.lastHost);

                    if (!list.isEmpty()) {
                        for (int index = 0; index < 50; index++) {
                            String name = index >= list.size() ? "" : list.get(index);
                            player.send(new SendString(name, 37111 + index));
                        }

                        player.message("<col=FF0D5D>There are " + list.size() + " accounts linked to " + Utility.formatName(other.getName()) + ".");
                        player.send(new SendString("Profiles:\\n" + list.size(), 37107));
                        player.send(new SendString(other.getName(), 37103));
                        player.interfaceManager.open(37100);
                    }
            }
        });

        commands.add(new Command("bombs") {
            @Override
            public void execute(Player player, CommandParser parser) {
                Position center = new Position(player.getX() - 2, player.getY() - 2, player.getHeight());
                Interactable target = Interactable.create(center, 5, 5);
                Position[] boundaries = Utility.getInnerBoundaries(target);
                List<Position> list = new ArrayList<>();
                Collections.addAll(list, boundaries);

                for (int index = 0; index < list.size(); index++) {
                    int finalIndex = index;
                    World.schedule(index, () -> World.sendGraphic(new Graphic(659), list.get(finalIndex), player.instance));
                }
            }
        });
        commands.add(new Command("bloodmoneychest") {
            @Override
            public void execute(Player player, CommandParser parser) {
                if (!BloodMoneyChest.active) {
                    BloodMoneyChest.spawn();
                    BloodMoneyChest.stopwatch.reset();
                }
            }
        });

        commands.add(new Command("resetplayer") {
            @Override
            public void execute(Player player, CommandParser parser) {
                if (parser.hasNext()) {
                    StringBuilder name = new StringBuilder(parser.nextString());

                    while (parser.hasNext()) {
                        name.append(" ").append(parser.nextString());
                    }

                    World.search(name.toString()).ifPresent(other -> {
                        DialogueFactory factory = player.dialogueFactory;
                        factory.sendOption("Skills", () -> {
                            factory.onAction(() -> player.send(new SendInputAmount("Enter the skill id", 1, input -> {
                                int skill = Integer.parseInt(input);
                                if (skill > -1 && skill < Skill.SKILL_COUNT) {
                                    other.skills.setMaxLevel(skill, skill == 3 ? 10 : 1);
                                    other.skills.setCombatLevel();
                                    other.updateFlags.add(UpdateFlag.APPEARANCE);
                                    player.message(other.getName() + "'s " + Skill.getName(skill) + " was reset");
                                }
                            })));
                        }, "Inventory", () -> {
                            other.inventory.clear();
                            player.message(other.getName() + "'s inventory was cleared");
                            factory.clear();
                        }, "Equipment", () -> {
                            other.equipment.clear();
                            player.message(other.getName() + "'s equipment was cleared");
                            factory.clear();
                        }, "Bank", () -> {
                            other.bank.clear();
                            player.message(other.getName() + "'s bank was cleared");
                            factory.clear();
                        });
                        player.dialogueFactory.execute();
                    });
                } else {
                    player.message("Invalid command use; ::resetplayer settings");
                }
            }
        });

        commands.add(new Command("doubleexp") {
            @Override
            public void execute(Player player, CommandParser parser) {
                Config.DOUBLE_EXPERIENCE = !Config.DOUBLE_EXPERIENCE;
                World.sendMessage("<col=CF2192>Tarnish: </col>Double experience is now " + (Config.DOUBLE_EXPERIENCE ? "activated" : "de-activated") + ".");
            }
        });

        commands.add(new Command("wildplayers") {
            @Override
            public void execute(Player player, CommandParser parser) {
                for (Player other : World.getPlayers()) {
                    if (other != null && Area.inWilderness(other)) {
                        int level = other.wilderness;
                        player.message("<col=255>" + other.getName() + " (level " + other.skills.getCombatLevel() + ") is in wilderness level " + level + ".");
                    }
                }
            }
        });

        commands.add(new Command("settitle") {
            @Override
            public void execute(Player player, CommandParser parser) {
                if (parser.hasNext()) {
                    StringBuilder name = new StringBuilder(parser.nextString());

                    while (parser.hasNext()) {
                        name.append(" ").append(parser.nextString());
                    }

                    World.search(name.toString()).ifPresent(other -> {
                        player.send(new SendInputMessage("Enter the title", 15, input -> {
                            other.playerTitle = PlayerTitle.create(input, other.playerTitle.getColor());
                        }));
                    });

                } else {
                    player.message("Invalid command use; ::title settings");
                }
            }
        });

        commands.add(new Command("setpt", "setplaytime") {
            @Override
            public void execute(Player player, CommandParser parser) {
                if (parser.hasNext()) {
                    StringBuilder name = new StringBuilder(parser.nextString());

                    while (parser.hasNext()) {
                        name.append(" ").append(parser.nextString());
                    }

                    World.search(name.toString()).ifPresent(other -> {
                        player.send(new SendInputAmount("Enter the play time", 8, input -> {
                            other.playTime = Integer.parseInt(input);
                        }));
                    });

                } else {
                    player.message("Invalid command use; ::setpt settings");
                }
            }
        });

        commands.add(new Command("giveitem", "gi") {
            @Override
            public void execute(Player player, CommandParser parser) {
                if (parser.hasNext()) {
                    StringBuilder name = new StringBuilder(parser.nextString());

                    while (parser.hasNext()) {
                        name.append(" ").append(parser.nextString());
                    }

                    World.search(name.toString()).ifPresent(other -> {
                        player.send(new SendInputAmount("Enter the itemId", 5, input -> {
                            other.inventory.add(new Item(Integer.parseInt(input), 1));
                        }));
                    });

                } else {
                    player.message("Invalid command use; ::giveitem settings");
                }
            }
        });

        commands.add(new Command("giveexp", "giveexperience") {
            @Override
            public void execute(Player player, CommandParser parser) {
                if (parser.hasNext()) {
                    StringBuilder name = new StringBuilder(parser.nextString());

                    while (parser.hasNext()) {
                        name.append(" ").append(parser.nextString());
                    }
                    World.search(name.toString()).ifPresent(other -> {
                        player.send(new SendInputAmount("Enter the skillid", 5, input -> {
                            other.skills.addExperience(Integer.parseInt(input), 1_500_000);
                        }));
                    });

                } else {
                    player.message("Invalid command use; ::kill settings");
                }
            }
        });

        commands.add(new Command("kill") {
            @Override
            public void execute(Player player, CommandParser parser) {
                if (parser.hasNext()) {
                    StringBuilder name = new StringBuilder(parser.nextString());

                    while (parser.hasNext()) {
                        name.append(" ").append(parser.nextString());
                    }
                    World.search(name.toString()).ifPresent(other -> other.damage(new Hit(other.getCurrentHealth())));

                } else {
                    player.message("Invalid command use; ::kill settings");
                }
            }
        });


        commands.add(new Command("randomevent") {
            @Override
            public void execute(Player player, CommandParser parser) {
                if (parser.hasNext()) {
                    StringBuilder name = new StringBuilder(parser.nextString());

                    while (parser.hasNext()) {
                        name.append(" ").append(parser.nextString());
                    }
                    World.search(name.toString()).ifPresent(MimeEvent::create);

                } else {
                    player.message("Invalid command use; ::randomevent settings");
                }
            }
        });

        commands.add(new Command("alltome") {
            @Override
            public void execute(Player player, CommandParser parser) {
                Position position = player.getPosition().copy();
                World.getPlayers().forEach(players -> {
                    if (!players.isBot && !players.equals(player)) {
                        players.move(position);
                        players.send(new SendMessage("You have been mass teleported."));
                    }
                });
            }
        });

        commands.add(new Command("setrank", "giverank", "rank") {
            @Override
            public void execute(Player player, CommandParser parser) {
                if (parser.hasNext()) {
                    StringBuilder name = new StringBuilder(parser.nextString());

                    while (parser.hasNext()) {
                        name.append(" ").append(parser.nextString());
                    }

                    World.search(name.toString()).ifPresent(other -> {
                        DialogueFactory factory = player.dialogueFactory;
                        factory.sendOption("Owner", () -> {
                            other.right = PlayerRight.OWNER;
                            other.updateFlags.add(UpdateFlag.APPEARANCE);
                            player.message("You have promoted " + other.getName() + ": " + other.right.getName());
                            other.message("You have been promoted: " + other.right.getName());
                        }, "Developer", () -> {
                            other.right = PlayerRight.DEVELOPER;
                            other.updateFlags.add(UpdateFlag.APPEARANCE);
                            player.message("You have promoted " + other.getName() + ": " + other.right.getName());
                            other.message("You have been promoted: " + other.right.getName());
                        }, "Administrator", () -> {
                            other.right = PlayerRight.ADMINISTRATOR;
                            other.updateFlags.add(UpdateFlag.APPEARANCE);
                            player.message("You have promoted " + other.getName() + ": " + other.right.getName());
                            other.message("You have been promoted: " + other.right.getName());
                        }, "Moderator", () -> {
                            other.right = PlayerRight.MODERATOR;
                            other.updateFlags.add(UpdateFlag.APPEARANCE);
                            player.message("You have promoted " + other.getName() + ": " + other.right.getName());
                            other.message("You have been promoted: " + other.right.getName());
                        }, "More...", () -> {
                            factory.sendOption("Manager", () -> {
                                other.right = PlayerRight.MANAGER;
                                other.updateFlags.add(UpdateFlag.APPEARANCE);
                                player.message("You have promoted " + other.getName() + ": " + other.right.getName());
                                other.message("You have been promoted: " + other.right.getName());
                            }, "Helper", () -> {
                                other.right = PlayerRight.HELPER;
                                other.updateFlags.add(UpdateFlag.APPEARANCE);
                                player.message("You have promoted " + other.getName() + ": " + other.right.getName());
                                other.message("You have been promoted: " + other.right.getName());
                            }, "Ironman", () -> {
                                other.right = PlayerRight.IRONMAN;
                                other.updateFlags.add(UpdateFlag.APPEARANCE);
                                player.message("You have promoted " + other.getName() + ": " + other.right.getName());
                                other.message("You have been promoted: " + other.right.getName());
                            }, "Player (Reset)", () -> {
                                other.right = PlayerRight.PLAYER;
                                other.updateFlags.add(UpdateFlag.APPEARANCE);
                                player.message("You have reset " + other.getName() + " to Player rank.");
                                other.message("Your rank has been reset to Player.");
                            }, "Nevermind", () -> {
                                factory.clear();
                            }).execute();
                        }).execute();
                    });
                } else {
                    player.message("Invalid command use; ::setrank playername");
                }
            }
        });

        commands.add(new Command("fight") {
            @Override
            public void execute(Player player, CommandParser parser) {
                if (parser.hasNext(2)) {
                    int one = parser.nextInt();
                    int two = parser.nextInt();
                    Position start = player.getPosition().copy();
                    if (NpcDefinition.get(one) == null || NpcDefinition.get(two) == null) {
                        player.send(new SendMessage("Definition for one or more of the monsters were null."));
                        return;
                    }
                    Npc boss1 = new Npc(one, new Position(start.getX() - 3, start.getY() + 3));
                    Npc boss2 = new Npc(two, new Position(start.getX() + 3, start.getY() + 3));
                    boss1.register();
                    boss2.register();
                    boss1.walk = false;
                    boss2.walk = false;
                    boss1.definition.setAggressive(false);
                    boss2.definition.setAggressive(false);
                    boss1.definition.setRespawnTime(-1);
                    boss2.definition.setRespawnTime(-1);
                    World.schedule(new Task(1) {
                        int count = 0;

                        @Override
                        protected void execute() {
                            if (count == 0) {
                                boss1.interact(boss2);
                                boss1.speak("I will fight for you, " + player.getName() + "!");
                            } else if (count == 1) {
                                boss2.interact(boss1);
                                boss2.speak("But I will win for you, " + player.getName() + "!");
                            } else if (count == 3) {
                                boss1.speak("3");
                                boss2.speak("3");
                            } else if (count == 4) {
                                boss1.speak("2");
                                boss2.speak("2");
                            } else if (count == 5) {
                                boss1.speak("1");
                                boss2.speak("1");
                            } else if (count == 6) {
                                boss1.speak("Good luck " + boss2.getName() + "!");
                                boss2.speak("Good luck " + boss1.getName() + "!");
                            } else if (count > 7) {
                                cancel();
                            }
                            count++;
                        }

                        @Override
                        protected void onCancel(boolean logout) {
                            boss1.getCombat().attack(boss2);
                            boss2.getCombat().attack(boss1);
                        }
                    });
                } else {
                    player.send(new SendMessage("Invalid command - ::fight 3080 3080"));
                }
            }
        });

        commands.add(new Command("spawnbots") {
            /** Wilderness positions for levels 1-4 (spread out across the wilderness). */
            private final Position[] WILDY_POSITIONS = {
                new Position(3090, 3525),  // ~Level 1 wilderness
                new Position(3083, 3530),  // ~Level 2 wilderness
                new Position(3098, 3537),  // ~Level 3 wilderness
                new Position(3075, 3543),  // ~Level 4 wilderness
                new Position(3105, 3534),  // ~Level 2-3 wilderness
                new Position(3092, 3528),  // ~Level 1-2 wilderness
                new Position(3080, 3535),  // ~Level 2 wilderness
                new Position(3100, 3540),  // ~Level 3 wilderness
                new Position(3070, 3530),  // ~Level 2 wilderness
                new Position(3110, 3538),  // ~Level 3 wilderness
            };

            private final String[] BOT_PREFIXES = {
                "PKer", "Wildy", "Edge", "Str", "God", "Nh", "Pure", "Zerk",
                "Tank", "Hybrid", "Tribrid", "Ags", "Gmaul", "Dds", "Veng",
                "Risk", "Dark", "Swift", "Iron", "Void", "Dharok", "Barrage",
                "Zer0", "Ice", "Fire", "Shadow", "Blood", "Skull", "Rush",
                "Smite", "Rag", "Deep", "Ancients", "Melee", "Range", "Mage"
            };

            private final String[] BOT_SUFFIXES = {
                "Max", "Slayer", "Pker", "Mauler", "Sword", "Killer", "Bridder",
                "Rusher", "Tanker", "Beast", "Legend", "Chief", "King", "Lord",
                "Savage", "Demon", "Reaper", "Hunter", "Sniper", "Wizard",
                "Ninja", "Fury", "Wrath", "Storm", "Blade", "Fang", "Viper",
                "Hawk", "Wolf", "Titan", "Ace", "Ghost", "Phantom", "Ranger",
                "Cleric", "Brute", "Brawler", "Menace", "Venom", "Toxin"
            };

            @Override
            public void execute(Player player, CommandParser parser) {
                int amount = 5; // default amount
                if (parser.hasNext()) {
                    try {
                        amount = parser.nextInt();
                        if (amount < 1) amount = 1;
                        if (amount > 50) amount = 50;
                    } catch (NumberFormatException e) {
                        player.send(new SendMessage("Invalid amount. Usage: ::spawnbots [amount]"));
                        return;
                    }
                }

                int spawned = 0;
                for (int i = 0; i < amount; i++) {
                    // Generate a unique bot name from prefix + suffix + random number
                    String prefix = BOT_PREFIXES[RandomUtils.inclusive(0, BOT_PREFIXES.length - 1)];
                    String suffix = BOT_SUFFIXES[RandomUtils.inclusive(0, BOT_SUFFIXES.length - 1)];
                    String botName = prefix + " " + suffix + RandomUtils.inclusive(1, 999);

                    PlayerBot bot = new PlayerBot(botName);
                    // Pick a random wilderness position
                    Position basePos = WILDY_POSITIONS[RandomUtils.inclusive(0, WILDY_POSITIONS.length - 1)];
                    // Add small random offset so bots don't stack on the same tile
                    int offsetX = RandomUtils.inclusive(-3, 3);
                    int offsetY = RandomUtils.inclusive(-3, 3);
                    bot.setPosition(new Position(basePos.getX() + offsetX, basePos.getY() + offsetY));
                    bot.register();

                    // Assign max strength bot class and gear them up
                    MaxStrengthMelee botClass = new MaxStrengthMelee();
                    bot.botClass = botClass;

                    // Set inventory and equipment
                    Item[] inventory = botClass.inventory();
                    bot.inventory.set(inventory);
                    bot.equipment.manualWearAll(botClass.equipment());

                    // Count food for the bot's eating logic
                    bot.foodRemaining = 0;
                    bot.statBoostersRemaining = 0;
                    for (Item item : inventory) {
                        if (item == null) continue;
                        if (FoodData.forId(item.getId()).isPresent()) {
                            bot.foodRemaining++;
                        }
                        Optional<PotionData> potion = PotionData.forId(item.getId());
                        if (!potion.isPresent() || potion.get() == PotionData.SUPER_RESTORE_POTIONS || potion.get() == PotionData.SARADOMIN_BREW) {
                            continue;
                        }
                        bot.statBoostersRemaining++;
                    }

                    // Set all combat stats to 99
                    int[] skills = botClass.skills();
                    for (int skill = 0; skill < skills.length; skill++) {
                        bot.skills.setMaxLevel(skill, skills[skill]);
                    }
                    bot.skills.setCombatLevel();
                    CombatSpecial.restore(bot, 100);

                    // Have the bot walk around in wilderness
                    BotObjective.WALK_IN_WILDERNESS.init(bot);
                    spawned++;
                }
                player.send(new SendMessage("<col=ff0000>Spawned " + spawned + " max strength PK bots in level 1-4 wilderness!"));
            }
        });

        commands.add(new Command("copybotgear") {
            @Override
            public void execute(Player player, CommandParser parser) {
                MaxStrengthMelee botClass = new MaxStrengthMelee();
                player.inventory.clear(false);
                player.equipment.clear(false);
                player.equipment.manualWearAll(botClass.equipment());
                player.inventory.addAll(botClass.inventory());
                player.inventory.refresh();
                player.equipment.login();
                player.skills.restoreAll();
                CombatSpecial.restore(player, 100);
                player.send(new SendMessage("<col=ff0000>You have been given max strength PK bot gear!"));
            }
        });

        commands.add(new Command("listbots") {
            @Override
            public void execute(Player player, CommandParser parser) {
                int count = 0;
                for (Player p : World.getPlayers()) {
                    if (p != null && p.isBot) {
                        player.send(new SendMessage("<col=255>[Bot] " + p.getName() + " at (" + p.getX() + ", " + p.getY() + ")"));
                        count++;
                    }
                }
                player.send(new SendMessage("<col=ff0000>Total bots online: " + count));
            }
        });

        commands.add(new Command("clearpkbots") {
            @Override
            public void execute(Player player, CommandParser parser) {
                int removed = 0;
                for (Player p : World.getPlayers()) {
                    if (p != null && p.isBot && p.getName().matches("(PKer Max|Wildy Slayer|Edge Pker|Str Mauler|GodSword).*")) {
                        p.unregister();
                        removed++;
                    }
                }
                player.send(new SendMessage("<col=ff0000>Removed " + removed + " PK bots from the wilderness."));
            }
        });
    }

    @Override
    public boolean canAccess(Player player) {
        return player.right == PlayerRight.OWNER;
    }

}
