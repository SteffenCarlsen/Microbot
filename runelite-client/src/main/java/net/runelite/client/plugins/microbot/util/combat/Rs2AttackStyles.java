package net.runelite.client.plugins.microbot.util.combat;

import net.runelite.api.VarPlayer;
import net.runelite.api.Varbits;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.combat.AttackTypeHelpers.AttackType;
import net.runelite.client.plugins.microbot.util.combat.AttackTypeHelpers.WeaponAttackType;

import java.util.Arrays;
import java.util.Objects;

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
        return getAttackStyle(currentEquippedWeaponTypeVarbit, currentAttackStyleVarbit, currentCastingModeVarbit);
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

    public static boolean setAttackStyle(AttackType attackType) {
        final int currentEquippedWeaponTypeVarbit = Microbot.getVarbitValue(Varbits.EQUIPPED_WEAPON_TYPE);
        final AttackType[] attackTypes = getAttackTypesForCurrentWeapon(currentEquippedWeaponTypeVarbit);
        if (Arrays.stream(attackTypes).noneMatch(x -> Objects.equals(x.getName(), attackType.getName()))) {
            Microbot.log("Attack style " + attackType.getName() + " not found in current weapon's attack types: " + Arrays.toString(attackTypes));
            return false;
        }
        for (int i = 0; i < attackTypes.length; i++) {
            if (Objects.equals(attackTypes[i].getName(), attackType.getName())) {
                switch (i) {
                    case 0:
                        return Rs2Combat.setAttackStyle(WidgetInfo.COMBAT_STYLE_ONE);
                    case 1:
                        return Rs2Combat.setAttackStyle(WidgetInfo.COMBAT_STYLE_TWO);
                    case 2:
                        return Rs2Combat.setAttackStyle(WidgetInfo.COMBAT_STYLE_THREE);
                    case 3:
                        return Rs2Combat.setAttackStyle(WidgetInfo.COMBAT_STYLE_FOUR);
                    default:
                        return false;

                }
            }
        }
        Microbot.log("Attack style " + attackType.getName() + " not found in current weapon's attack types.");
        return false;
    }

    private static AttackType getAttackStyle(int equippedWeaponType, int attackStyleIndex, int castingMode)
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
