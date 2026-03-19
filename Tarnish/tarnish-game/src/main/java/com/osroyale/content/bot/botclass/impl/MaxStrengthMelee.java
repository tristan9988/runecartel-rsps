package com.osroyale.content.bot.botclass.impl;

import com.osroyale.content.bot.PlayerBot;
import com.osroyale.content.bot.botclass.BotClass;
import com.osroyale.content.consume.FoodData;
import com.osroyale.content.consume.PotionData;
import com.osroyale.content.skill.impl.magic.spell.impl.Vengeance;
import com.osroyale.game.event.impl.ItemClickEvent;
import com.osroyale.game.world.entity.combat.CombatType;
import com.osroyale.game.world.entity.combat.attack.FightType;
import com.osroyale.game.world.entity.combat.attack.listener.SimplifiedListener;
import com.osroyale.game.world.entity.combat.hit.Hit;
import com.osroyale.game.world.entity.mob.Mob;
import com.osroyale.game.world.entity.mob.player.Player;
import com.osroyale.game.world.entity.mob.prayer.Prayer;
import com.osroyale.game.world.entity.skill.Skill;
import com.osroyale.game.world.items.Item;
import com.osroyale.util.RandomUtils;
import plugin.click.item.EatFoodPlugin;

import java.util.concurrent.TimeUnit;

import static com.osroyale.game.world.entity.combat.attack.FormulaFactory.getModifiedMaxHit;

/**
 * Max strength PK bot class.
 * Equipment: Neitiznot faceguard, Amulet of torture, Infernal cape,
 *            Bandos chestplate, Bandos tassets, Primordial boots,
 *            Barrows gloves, Berserker ring (i), Avernic defender,
 *            Abyssal tentacle (main weapon), AGS (spec weapon).
 *
 * Inventory: Super combat potion, Super restores, Manta rays,
 *            Cooked karambwans, AGS for spec, Vengeance runes.
 */
public class MaxStrengthMelee extends SimplifiedListener<Player> implements BotClass {

    @Override
    public Item[] inventory() {
        return new Item[] {
            new Item(12695),       // Super combat potion (4)
            new Item(3024),        // Super restore (4)
            new Item(3024),        // Super restore (4)
            new Item(391),         // Manta ray
            new Item(391),         // Manta ray
            new Item(391),         // Manta ray
            new Item(391),         // Manta ray
            new Item(391),         // Manta ray
            new Item(391),         // Manta ray
            new Item(391),         // Manta ray
            new Item(391),         // Manta ray
            new Item(391),         // Manta ray
            new Item(391),         // Manta ray
            new Item(391),         // Manta ray
            new Item(391),         // Manta ray
            new Item(391),         // Manta ray
            new Item(3144),        // Cooked karambwan
            new Item(3144),        // Cooked karambwan
            new Item(3144),        // Cooked karambwan
            new Item(3144),        // Cooked karambwan
            new Item(391),         // Manta ray
            new Item(391),         // Manta ray
            new Item(391),         // Manta ray
            new Item(391),         // Manta ray
            new Item(11802),       // Armadyl godsword (spec weapon)
            new Item(557, 500),    // Earth runes (for Vengeance)
            new Item(560, 500),    // Death runes (for Vengeance)
            new Item(9075, 500)    // Astral runes (for Vengeance)
        };
    }

    @Override
    public Item[] equipment() {
        return new Item[] {
            new Item(24271),   // Neitiznot faceguard (helm)
            new Item(19553),   // Amulet of torture (amulet)
            new Item(21295),   // Infernal cape (cape)
            new Item(11832),   // Bandos chestplate (body)
            new Item(11834),   // Bandos tassets (legs)
            new Item(13239),   // Primordial boots (feet)
            new Item(7462),    // Barrows gloves (hands)
            new Item(11773),   // Berserker ring (i) (ring)
            new Item(22322),   // Avernic defender (shield)
            new Item(12006)    // Abyssal tentacle (weapon)
        };
    }

    @Override
    public int[] skills() {
        return new int[] { 99, 99, 99, 99, 99, 99, 99 };
    }

    @Override
    public void initCombat(Player target, PlayerBot bot) {
        pot(target, bot);
        bot.prayer.toggle(Prayer.PROTECT_ITEM, Prayer.PIETY);
        bot.getCombat().addListener(this);
        bot.spellCasting.cast(new Vengeance(), null);
        bot.getCombat().setFightType(FightType.WHIP_LASH);
    }

    @Override
    public void handleCombat(Player target, PlayerBot bot) {
        // Mirror smite prayer
        if (!bot.prayer.isActive(Prayer.SMITE) && target.prayer.isActive(Prayer.SMITE)) {
            bot.prayer.toggle(Prayer.SMITE);
            bot.speak("Let's smite then...");
        } else if (bot.prayer.isActive(Prayer.SMITE) && !target.prayer.isActive(Prayer.SMITE)) {
            bot.prayer.toggle(Prayer.SMITE);
        }

        // If we have spec queued and opponent is protecting melee, cancel
        if (bot.isSpecialActivated() && target.prayer.isActive(Prayer.PROTECT_FROM_MELEE)) {
            bot.speak("Nice prayer...");
            bot.getCombatSpecial().disable(bot, false);
            bot.endFight();
        }

        // Mirror protect from melee
        if (!bot.prayer.isActive(Prayer.PROTECT_FROM_MELEE) && target.prayer.isActive(Prayer.PROTECT_FROM_MELEE)) {
            bot.prayer.toggle(Prayer.PROTECT_FROM_MELEE);
        } else if (bot.prayer.isActive(Prayer.PROTECT_FROM_MELEE) && !target.prayer.isActive(Prayer.PROTECT_FROM_MELEE)) {
            bot.prayer.toggle(Prayer.PROTECT_FROM_MELEE);
        }

        // Re-cast vengeance when off cooldown
        if (bot.spellCasting.vengeanceDelay.elapsedTime(TimeUnit.SECONDS) >= 30) {
            bot.spellCasting.cast(new Vengeance(), null);
        }
    }

    @Override
    public void endFight(PlayerBot bot) {
        bot.prayer.deactivate(Prayer.PROTECT_ITEM, Prayer.SMITE, Prayer.PIETY);
    }

    @Override
    public void hit(Player attacker, Mob defender, Hit hit) {
        int max = getModifiedMaxHit(attacker, defender, CombatType.MELEE);

        boolean hasRoom = attacker.inventory.getFreeSlots() > 0;
        boolean hasSpec = attacker.getSpecialPercentage().intValue() >= 50;
        boolean lowHp = defender.getCurrentHealth() <= defender.getMaximumHealth() * RandomUtils.inclusive(0.45, 0.60);
        boolean combo = defender.getCurrentHealth() <= defender.getMaximumHealth() * RandomUtils.inclusive(0.45, 0.65)
                && hit.getDamage() >= max * RandomUtils.inclusive(0.60, 0.75);

        if (!hasRoom || (!combo && !lowHp))
            return;

        PlayerBot bot = ((PlayerBot) attacker);
        bot.schedule(4, () -> {
            // Switch to AGS for spec
            if (bot.equipment.getWeapon().matchesId(12006)) {
                int index = bot.inventory.computeIndexForId(11802);
                bot.equipment.equip(index);
                bot.getCombat().setFightType(FightType.GODSWORD_SLASH);
            }

            if (hasSpec && (lowHp || RandomUtils.success(0.80)))
                bot.getCombatSpecial().enable(bot);

            bot.schedule(4, () -> {
                // Switch back to tentacle whip after spec
                if (!bot.isSpecialActivated() && bot.equipment.getWeapon().matchesId(11802)) {
                    int idx = bot.inventory.computeIndexForId(12006);
                    bot.equipment.equip(idx);
                    idx = bot.inventory.computeIndexForId(22322);
                    if (idx >= 0) bot.equipment.equip(idx);
                    bot.getCombat().setFightType(FightType.WHIP_LASH);
                }
            });
        });
    }

    @Override
    public void pot(Player target, PlayerBot bot) {
        if (target.getCurrentHealth() <= Math.floor(target.getMaximumHealth() * 0.35))
            return;

        if (!bot.potionDelay.elapsed(1250)) {
            return;
        }

        PotionData potion;
        ItemClickEvent event;

        if (checkSkill(bot, Skill.PRAYER, 40)) {
            int index = bot.inventory.computeIndexForId(3024);
            if (index >= 0) {
                event = new ItemClickEvent(0, bot.inventory.get(index), index);
                potion = PotionData.SUPER_RESTORE_POTIONS;
                bot.pot(target, event, potion);
            }
        } else if (checkSkill(bot, Skill.ATTACK, 115)
                || checkSkill(bot, Skill.STRENGTH, 115)
                || checkSkill(bot, Skill.DEFENCE, 115)) {
            int index = bot.inventory.computeIndexForId(12695);
            if (index >= 0) {
                event = new ItemClickEvent(0, bot.inventory.get(index), index);
                potion = PotionData.SUPER_COMBAT_POTION;
                bot.pot(target, event, potion);
            }
        }
    }

    @Override
    public void eat(Player target, PlayerBot bot) {
        int max = target.playerAssistant.getMaxHit(bot, target.getStrategy().getCombatType());
        if (bot.getCurrentHealth() > bot.getMaximumHealth() * 0.45 && max < bot.getCurrentHealth())
            return;

        if (target.getCurrentHealth() <= Math.floor(target.getMaximumHealth() * 0.35) && max < bot.getCurrentHealth())
            return;

        // Eat manta ray
        int index = bot.inventory.computeIndexForId(391);
        if (index >= 0) {
            EatFoodPlugin.eat(bot, bot.inventory.get(index), index, FoodData.MANTA);
            bot.foodRemaining--;
        }

        if (bot.getCurrentHealth() >= bot.getMaximumHealth() * 0.35)
            return;

        // Combo eat with karambwan
        index = bot.inventory.computeIndexForId(3144);
        if (index >= 0) {
            EatFoodPlugin.eat(bot, bot.inventory.get(index), index, FoodData.COOKED_KARAMBWAN);
            bot.foodRemaining--;
        }
    }

    @Override
    public boolean canOtherAttack(Mob attacker, Player defender) {
        if (defender.getCombat().isAttacking() && !defender.getCombat().isAttacking(attacker)) {
            attacker.getPlayer().message("You cannot attack a bot while they are attacking another player.");
            return false;
        }
        return true;
    }

    @Override
    public void block(Mob attacker, Player defender, Hit hit, CombatType combatType) {
        ((PlayerBot) defender).consumableDelay = RandomUtils.inclusive(1, 3);
    }

    private boolean checkSkill(PlayerBot bot, int id, int minimum) {
        return bot.skills.getLevel(id) < minimum;
    }
}

