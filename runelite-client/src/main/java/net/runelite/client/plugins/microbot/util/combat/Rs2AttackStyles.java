package net.runelite.client.plugins.microbot.util.combat;

import net.runelite.api.VarPlayer;
import net.runelite.api.Varbits;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.combat.AttackTypeHelpers.AttackType;
import net.runelite.client.plugins.microbot.util.combat.AttackTypeHelpers.WeaponAttackType;

public class Rs2AttackStyles {
    /**
     * Retrieves the current attack type based on the player's equipped weapon and attack style.
     * It considers the equipped weapon type, the current attack style index, and the casting mode.
     *
     * @return The current AttackType based on the player's settings.
     */
    public static AttackType getAttackTypeForCurrentWeapon() {
        final int currentAttackStyleVarbit = Microbot.getVarbitPlayerValue(VarPlayer.ATTACK_STYLE);
        final int currentEquippedWeaponTypeVarbit = Microbot.getVarbitValue(Varbits.EQUIPPED_WEAPON_TYPE);
        final int currentCastingModeVarbit = Microbot.getVarbitValue(Varbits.DEFENSIVE_CASTING_MODE);
        return updateAttackStyle(currentEquippedWeaponTypeVarbit, currentAttackStyleVarbit, currentCastingModeVarbit);
    }

    /**
     * Retrieves the available attack types for the currently equipped weapon.
     * This method uses the equipped weapon type to determine the possible attack styles.
     *
     * @return An array of AttackType representing the available attack styles for the current weapon.
     */
    public static AttackType[] getAttackTypesForCurrentWeapon() {
        final int currentEquippedWeaponTypeVarbit = Microbot.getVarbitValue(Varbits.EQUIPPED_WEAPON_TYPE);
        return getAttackTypesForCurrentWeapon(currentEquippedWeaponTypeVarbit);
    }

    private static AttackType updateAttackStyle(int equippedWeaponType, int attackStyleIndex, int castingMode)
    {
        boolean isDefensiveCasting = castingMode == 1 && attackStyleIndex == 4;
        AttackType attackType = null;
        AttackType[] attackTypes = WeaponAttackType.getWeaponAttackType(equippedWeaponType).getAttackTypes();
        if (attackStyleIndex < attackTypes.length)
        {
            attackType = attackTypes[attackStyleIndex];
        }
        else if (attackStyleIndex == 4 || isDefensiveCasting)
        {
            attackType = AttackType.MAGIC;
        }
        if (attackType == null)
        {
            attackType = AttackType.NONE;
        }

        return attackType;
    }
    private static AttackType[] getAttackTypesForCurrentWeapon(int equippedWeaponType)
    {
        return WeaponAttackType.getWeaponAttackType(equippedWeaponType).getAttackTypes();
    }
}
