package net.runelite.client.plugins.microbot.util.combat.armor;

import net.runelite.api.gameval.ItemID;

import java.util.List;

public class Armor {
    int id;
    ArmorType type;
    public String Name;
    public boolean IsMembers;
    public int StabAttack;
    public int SlashAttack;
    public int CrushAttack;
    public int MagicAttack;
    public int RangedAttack;
    public int StabDefence;
    public int SlashDefence;
    public int CrushDefence;
    public int MagicDefence;
    public int RangedDefence;
    public int Strength;
    public int RangedStrength;
    public int MagicDamage;
    public int Prayer;
    public double Weight;
    public Armor(int id) {
        this.id = id;
    }

    public void loadValues(List<String> values) {
        this.id = values.get(0).equals("null") ? 0 : Integer.parseInt(values.get(0));
        this.Name = values.get(1);
        this.IsMembers = Boolean.parseBoolean(values.get(2));
        this.StabAttack = Integer.parseInt(values.get(3));
        this.SlashAttack = Integer.parseInt(values.get(4));
        this.CrushAttack = Integer.parseInt(values.get(5));
        this.MagicAttack = Integer.parseInt(values.get(6));
        this.RangedAttack = Integer.parseInt(values.get(7));
        this.StabDefence = Integer.parseInt(values.get(8));
        this.SlashDefence = Integer.parseInt(values.get(9));
        this.CrushDefence = Integer.parseInt(values.get(10));
        this.MagicDefence = Integer.parseInt(values.get(11));
        this.RangedDefence = Integer.parseInt(values.get(12));
        this.Strength = Integer.parseInt(values.get(13));
        this.RangedStrength = Integer.parseInt(values.get(14));
        this.MagicDamage = Integer.parseInt(values.get(15));
        this.Prayer = Integer.parseInt(values.get(16));
        this.Weight = Double.parseDouble(values.get(17));
        this.type = calculateArmorType();
    }

    private ArmorType calculateArmorType() {
        // Track which style has the highest bonuses
        int meleePoints = 0;
        int rangedPoints = 0;
        int magicPoints = 0;

        // Calculate points based on defensive stats
        meleePoints += StabDefence + SlashDefence + CrushDefence;
        rangedPoints += RangedDefence;
        magicPoints += MagicDefence;

        // Calculate points based on offensive stats
        meleePoints += StabAttack + SlashAttack + CrushAttack;
        rangedPoints += RangedAttack;
        magicPoints += MagicAttack;

        // Calculate points based on strength bonuses
        meleePoints += Strength * 2;
        rangedPoints += RangedStrength * 2;
        magicPoints += MagicDamage * 2;

        // Determine if item is hybrid (supports multiple styles)
        int countSignificantStyles = 0;

        if (meleePoints > 10) countSignificantStyles++;
        if (rangedPoints > 10) countSignificantStyles++;
        if (magicPoints > 10) countSignificantStyles++;

        if (countSignificantStyles > 1) {
            return ArmorType.HYBRID;
        }

        // Return armor type based on highest point value
        if (magicPoints >= meleePoints && magicPoints >= rangedPoints) {
            return ArmorType.MAGIC;
        } else if (rangedPoints >= meleePoints && rangedPoints >= magicPoints) {
            return ArmorType.RANGED;
        } else {
            return ArmorType.MELEE;
        }
    }
}