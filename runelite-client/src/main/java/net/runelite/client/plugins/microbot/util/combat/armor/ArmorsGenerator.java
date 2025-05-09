package net.runelite.client.plugins.microbot.util.combat.armor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ArmorsGenerator {
    private static volatile ArmorsGenerator instance;
    private final Map<Integer, Armor> armorCache;
    private final Map<ArmorType, List<Armor>> armorByType;
    private boolean initialized = false;

    private ArmorsGenerator() {
        this.armorCache = new ConcurrentHashMap<>();
        this.armorByType = new ConcurrentHashMap<>();
        for (ArmorType type : ArmorType.values()) {
            armorByType.put(type, new ArrayList<>());
        }
    }

    public static ArmorsGenerator getInstance() {
        if (instance == null) {
            synchronized (ArmorsGenerator.class) {
                if (instance == null) {
                    instance = new ArmorsGenerator();
                }
            }
        }
        return instance;
    }

    private synchronized void initialize() {
        if (initialized) {
            return;
        }

        // Initialize shields
        for (var shieldData : ArmorIds.Shields) {
            loadArmorFromSet(shieldData);
        }
        // Initialize body armor
        for (var bodyData : BodyIds.Bodies) {
            loadArmorFromSet(bodyData);
        }
        // Initialize legs armor
        for (var legsData : ArmorIds.Legs) {
            loadArmorFromSet(legsData);
        }
        // Initialize gloves
        for (var glovesData : ArmorIds.Gloves) {
            loadArmorFromSet(glovesData);
        }
        // Initialize boots
        for (var bootsData : ArmorIds.Boots) {
            loadArmorFromSet(bootsData);
        }
        // Initialize capes
        for (var capeData : ArmorIds.Capes) {
            loadArmorFromSet(capeData);
        }
        // Initialize amulets
        for (var amuletData : ArmorIds.Necks) {
            loadArmorFromSet(amuletData);
        }
        for (var weaponData : ArmorIds.OneHandedWeapons) {
            loadArmorFromSet(weaponData);
        }
        for (var twoHandedData : ArmorIds.TwoHandedWeapons) {
            loadArmorFromSet(twoHandedData);
        }

        initialized = true;
    }

    private void loadArmorFromSet(List<String> armorData) {
        List<String> values = new ArrayList<>(armorData);
        if (values.size() >= 18) {
            Armor armor = new Armor(0);
            armor.loadValues(values);
            if (armor.id > 0) { // Skip null IDs
                cacheArmor(armor);
            }
        }
    }

    private void cacheArmor(Armor armor) {
        armorCache.put(armor.id, armor);
        armorByType.get(armor.type).add(armor);
    }

    public Armor getArmorById(int id) {
        if (!initialized) {
            initialize();
        }
        return armorCache.get(id);
    }

    public List<Armor> getArmorByType(ArmorType type) {
        if (!initialized) {
            initialize();
        }
        return Collections.unmodifiableList(armorByType.get(type));
    }

    public List<Armor> getAllArmor() {
        if (!initialized) {
            initialize();
        }
        return Collections.unmodifiableList(new ArrayList<>(armorCache.values()));
    }

    // Specialized query methods
    public List<Armor> getArmorByName(String name) {
        if (!initialized) {
            initialize();
        }
        return armorCache.values().stream()
                .filter(a -> a.Name.toLowerCase().contains(name.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<Armor> filterByMinDefense(int minDefense) {
        if (!initialized) {
            initialize();
        }
        return armorCache.values().stream()
                .filter(a -> Math.max(Math.max(a.StabDefence, a.SlashDefence),
                        Math.max(Math.max(a.CrushDefence, a.MagicDefence), a.RangedDefence)) >= minDefense)
                .collect(Collectors.toList());
    }
}