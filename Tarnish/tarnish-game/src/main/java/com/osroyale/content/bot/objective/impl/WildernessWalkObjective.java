package com.osroyale.content.bot.objective.impl;

import com.osroyale.content.bot.BotUtility;
import com.osroyale.content.bot.PlayerBot;
import com.osroyale.content.bot.objective.BotObjectiveListener;
import com.osroyale.game.world.World;
import com.osroyale.game.world.entity.mob.player.Player;
import com.osroyale.game.world.position.Area;
import com.osroyale.game.world.position.Position;
import com.osroyale.util.RandomUtils;
import com.osroyale.util.Utility;

public class WildernessWalkObjective implements BotObjectiveListener {

    @Override
    public void init(PlayerBot bot) {
        bot.loop(1, () -> {
            if (bot.movement.needsPlacement()) {
                return;
            }

            // ~5% chance per tick to look for another bot to fight
            if (bot.opponent == null && !bot.isDead() && !bot.getCombat().inCombat()
                    && bot.botClass != null && bot.foodRemaining > 0
                    && RandomUtils.success(0.05)) {
                PlayerBot target = findNearbyBot(bot);
                if (target != null) {
                    startBotFight(bot, target);
                    return;
                }
            }

            int x = bot.getX() + RandomUtils.inclusive(-5, 5);
            int y = bot.getY() + RandomUtils.inclusive(-5, 5);
            if (x < 3061) x = 3061;
            if (y < 3525) y = 3525;
            if (x > 3101) x = 3101;
            if (y > 3547) y = 3547;

            bot.walkTo(Position.create(x, y));
            bot.pause(RandomUtils.inclusive(4, 15));
        });
    }

    @Override
    public void finish(PlayerBot bot) {
    }

    /**
     * Scans nearby players for another idle bot within 10 tiles.
     */
    private PlayerBot findNearbyBot(PlayerBot bot) {
        for (Player player : World.getPlayers()) {
            if (player == null || player == bot || !player.isBot) {
                continue;
            }
            if (!(player instanceof PlayerBot other)) {
                continue;
            }
            // Must be nearby, alive, idle, in wilderness, and have gear/food
            if (other.isDead() || other.getCurrentHealth() <= 0) continue;
            if (other.opponent != null) continue;
            if (other.getCombat().inCombat()) continue;
            if (other.botClass == null || other.foodRemaining <= 0) continue;
            if (!Area.inWilderness(other)) continue;
            if (!bot.getPosition().isWithinDistance(other.getPosition(), 10)) continue;

            return other;
        }
        return null;
    }

    /**
     * Initiates a fight between two bots.
     */
    private void startBotFight(PlayerBot attacker, PlayerBot defender) {
        // Set up attacker
        attacker.botClass.initCombat(defender, attacker);
        attacker.getCombat().attack(defender);
        attacker.speak(Utility.randomElement(BotUtility.FIGHT_START_MESSAGES));
        attacker.opponent = defender;
        attacker.stopLoop();

        // Defender retaliates after a short delay
        defender.schedule(2, () -> {
            if (defender.opponent != null || defender.isDead()) return;
            defender.botClass.initCombat(attacker, defender);
            defender.getCombat().attack(attacker);
            defender.speak(Utility.randomElement(BotUtility.FIGHT_START_MESSAGES));
            defender.opponent = attacker;
            defender.stopLoop();
        });
    }
}
