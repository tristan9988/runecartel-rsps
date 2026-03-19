package plugin;

import com.osroyale.Config;
import com.osroyale.content.bot.botclass.impl.*;
import com.osroyale.content.skill.impl.magic.Spellbook;
import com.osroyale.game.plugin.PluginContext;
import com.osroyale.game.world.entity.mob.UpdateFlag;
import com.osroyale.game.world.entity.mob.player.Player;
import com.osroyale.game.world.items.Item;
import com.osroyale.game.world.position.Area;

/**
 * Handles the Spawn Tab sidebar interface (interface 42500).
 * Buttons 42521-42567 map to signed short values -23015 to -22969.
 */
public class SpawnTabPlugin extends PluginContext {

    // Button IDs (as signed shorts received by server)
    // Client sends: (65536 - buttonId) as signed short for large button IDs
    // 42521 -> 65536 - 42521 = 23015, as signed = -23015
    
    // Food
    private static final int BTN_SHARK         = -23015; // 42521
    private static final int BTN_MANTA_RAY     = -23014; // 42522
    private static final int BTN_KARAMBWAN     = -23013; // 42523
    private static final int BTN_ANGLERFISH    = -23012; // 42524
    private static final int BTN_DARK_CRAB     = -23011; // 42525
    private static final int BTN_LOBSTER       = -23010; // 42526

    // Potions
    private static final int BTN_SUPER_COMBAT  = -23008; // 42528
    private static final int BTN_SUPER_RESTORE = -23007; // 42529
    private static final int BTN_SARA_BREW     = -23006; // 42530
    private static final int BTN_RANGE_POT     = -23005; // 42531
    private static final int BTN_ANTIFIRE      = -23004; // 42532
    private static final int BTN_STAMINA       = -23003; // 42533

    // Runes
    private static final int BTN_DEATH_RUNE    = -23001; // 42535
    private static final int BTN_BLOOD_RUNE    = -23000; // 42536
    private static final int BTN_SOUL_RUNE     = -22999; // 42537
    private static final int BTN_WATER_RUNE    = -22998; // 42538
    private static final int BTN_FIRE_RUNE     = -22997; // 42539
    private static final int BTN_AIR_RUNE      = -22996; // 42540
    private static final int BTN_ASTRAL_RUNE   = -22995; // 42541

    // Weapons
    private static final int BTN_D_SCIM        = -22993; // 42543
    private static final int BTN_DDS           = -22992; // 42544
    private static final int BTN_RUNE_CBOW     = -22991; // 42545
    private static final int BTN_MSB_I         = -22990; // 42546
    private static final int BTN_GMAUL         = -22989; // 42547
    private static final int BTN_WHIP          = -22988; // 42548

    // Armour
    private static final int BTN_RUNE_HELM     = -22986; // 42550
    private static final int BTN_RUNE_PLATE    = -22985; // 42551
    private static final int BTN_RUNE_LEGS     = -22984; // 42552
    private static final int BTN_RUNE_KITE     = -22983; // 42553
    private static final int BTN_DHIDE_BODY    = -22982; // 42554
    private static final int BTN_DHIDE_CHAPS   = -22981; // 42555
    private static final int BTN_GLORY         = -22980; // 42556
    private static final int BTN_RECOIL        = -22979; // 42557

    // Ammo
    private static final int BTN_RUNE_ARROW    = -22977; // 42559
    private static final int BTN_DIAMOND_BOLTS = -22976; // 42560
    private static final int BTN_RUBY_BOLTS    = -22975; // 42561

    // Gear Loadouts
    private static final int BTN_AGS_RUNE      = -22973; // 42563
    private static final int BTN_WELFARE_RUNE  = -22972; // 42564
    private static final int BTN_PURE_MELEE    = -22971; // 42565
    private static final int BTN_PURE_RANGE    = -22970; // 42566
    private static final int BTN_ZERKER_MELEE  = -22969; // 42567

    @Override
    protected boolean onClick(Player player, int button) {
        switch (button) {
            // === FOOD (item IDs) ===
            case BTN_SHARK:        return spawnItem(player, 385, 10);   // Shark
            case BTN_MANTA_RAY:    return spawnItem(player, 391, 10);   // Manta ray
            case BTN_KARAMBWAN:    return spawnItem(player, 3144, 10);  // Cooked karambwan
            case BTN_ANGLERFISH:   return spawnItem(player, 13441, 10); // Anglerfish
            case BTN_DARK_CRAB:    return spawnItem(player, 11936, 10); // Dark crab
            case BTN_LOBSTER:      return spawnItem(player, 379, 10);   // Lobster

            // === POTIONS ===
            case BTN_SUPER_COMBAT: return spawnItem(player, 12695, 1);  // Super combat (4)
            case BTN_SUPER_RESTORE:return spawnItem(player, 3024, 1);   // Super restore (4)
            case BTN_SARA_BREW:    return spawnItem(player, 6685, 1);   // Saradomin brew (4)
            case BTN_RANGE_POT:    return spawnItem(player, 2444, 1);   // Ranging potion (4)
            case BTN_ANTIFIRE:     return spawnItem(player, 2452, 1);   // Antifire (4)
            case BTN_STAMINA:      return spawnItem(player, 12625, 1);  // Stamina potion (4)

            // === RUNES ===
            case BTN_DEATH_RUNE:   return spawnItem(player, 560, 500);  // Death rune
            case BTN_BLOOD_RUNE:   return spawnItem(player, 565, 500);  // Blood rune
            case BTN_SOUL_RUNE:    return spawnItem(player, 566, 500);  // Soul rune
            case BTN_WATER_RUNE:   return spawnItem(player, 555, 500);  // Water rune
            case BTN_FIRE_RUNE:    return spawnItem(player, 554, 500);  // Fire rune
            case BTN_AIR_RUNE:     return spawnItem(player, 556, 500);  // Air rune
            case BTN_ASTRAL_RUNE:  return spawnItem(player, 9075, 500); // Astral rune

            // === WEAPONS ===
            case BTN_D_SCIM:       return spawnItem(player, 4587, 1);   // Dragon scimitar
            case BTN_DDS:          return spawnItem(player, 5698, 1);   // Dragon dagger (p++)
            case BTN_RUNE_CBOW:    return spawnItem(player, 9185, 1);   // Rune crossbow
            case BTN_MSB_I:        return spawnItem(player, 12788, 1);  // Magic shortbow (i)
            case BTN_GMAUL:        return spawnItem(player, 4153, 1);   // Granite maul
            case BTN_WHIP:         return spawnItem(player, 4151, 1);   // Abyssal whip

            // === ARMOUR ===
            case BTN_RUNE_HELM:    return spawnItem(player, 1163, 1);   // Rune full helm
            case BTN_RUNE_PLATE:   return spawnItem(player, 1127, 1);   // Rune platebody
            case BTN_RUNE_LEGS:    return spawnItem(player, 1079, 1);   // Rune platelegs
            case BTN_RUNE_KITE:    return spawnItem(player, 1201, 1);   // Rune kiteshield
            case BTN_DHIDE_BODY:   return spawnItem(player, 1135, 1);   // Green d'hide body
            case BTN_DHIDE_CHAPS:  return spawnItem(player, 1099, 1);   // Green d'hide chaps
            case BTN_GLORY:        return spawnItem(player, 1712, 1);   // Amulet of glory (4)
            case BTN_RECOIL:       return spawnItem(player, 2550, 1);   // Ring of recoil

            // === AMMO ===
            case BTN_RUNE_ARROW:   return spawnItem(player, 892, 200);  // Rune arrow
            case BTN_DIAMOND_BOLTS:return spawnItem(player, 9243, 200); // Diamond bolts (e)
            case BTN_RUBY_BOLTS:   return spawnItem(player, 9242, 200); // Ruby bolts (e)

            // === GEAR LOADOUTS ===
            case BTN_AGS_RUNE:     return applyLoadout(player, new AGSRuneMelee());
            case BTN_WELFARE_RUNE: return applyLoadout(player, new WelfareRuneMelee());
            case BTN_PURE_MELEE:   return applyLoadout(player, new PureMelee());
            case BTN_PURE_RANGE:   return applyLoadout(player, new PureRangeMelee());
            case BTN_ZERKER_MELEE: return applyLoadout(player, new ZerkerMelee());
        }
        return false;
    }

    private boolean spawnItem(Player player, int itemId, int amount) {
        if (player.inventory.getFreeSlots() == 0) {
            player.message("Your inventory is full!");
            return true;
        }
        player.inventory.add(new Item(itemId, amount));
        return true;
    }

    private boolean applyLoadout(Player player, com.osroyale.content.bot.botclass.BotClass botClass) {
        if (player.getCombat().inCombat()) {
            player.message("You can not do this while in combat!");
            return true;
        }

        if (Area.inWilderness(player)) {
            player.message("You can not do this while in the wilderness!");
            return true;
        }

        if (player.pvpInstance && Area.inPvP(player)) {
            player.message("You must be in a safe zone to do this!");
            return true;
        }

        // Bank current gear & inventory
        player.prayer.reset();
        player.bank.depositeEquipment(false);
        player.bank.depositeInventory(false);

        if (!player.inventory.isEmpty() || !player.equipment.isEmpty()) {
            player.message("You have no space in your bank!");
            return true;
        }

        // Set skills
        int[] skills = botClass.skills();
        if (skills != null) {
            for (int index = 0; index < skills.length; index++) {
                player.skills.setMaxLevel(index, skills[index]);
                player.skills.setLevel(index, skills[index]);
            }
            player.skills.setCombatLevel();
        }

        player.updateFlags.add(UpdateFlag.APPEARANCE);

        // Equip gear
        Item[] equipment = botClass.equipment();
        if (equipment != null) {
            for (Item item : equipment) {
                if (item != null) {
                    player.equipment.manualWear(item.copy());
                }
            }
        }

        // Set spellbook to modern (default for these loadouts)
        player.spellbook = Spellbook.MODERN;
        player.interfaceManager.setSidebar(Config.MAGIC_TAB, player.spellbook.getInterfaceId());

        // Add inventory items
        Item[] inventory = botClass.inventory();
        if (inventory != null) {
            for (Item item : inventory) {
                if (item != null) {
                    player.inventory.add(item.copy());
                }
            }
        }

        player.equipment.refresh();
        player.message("Gear loadout applied!");

        if (player.pvpInstance) {
            player.playerAssistant.setValueIcon();
        }

        return true;
    }
}
