package net.runelite.client.plugins.microbot.TaF.AmmoniteCrabs;

import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.TaF.AmmoniteCrabs.enums.AmmoniteCrabState;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.player.Rs2PlayerModel;
import net.runelite.client.plugins.microbot.util.security.Login;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import java.awt.event.KeyEvent;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AmmoniteCrabScript extends Script {

    public static String version = "1.0";

    public int afkTimer = 0;
    public int hijackTimer = 0;
    public AmmoniteCrabState ammoniteCrabState = AmmoniteCrabState.SCANNING_WORLD;
    public int timesHopped = 0;

    public boolean run(AmmoniteCrabConfig config) {
        initialPlayerLocation = null;
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;
                long startTime = System.currentTimeMillis();
                Rs2Combat.enableAutoRetialiate();

                if (otherPlayerDetected() && !Rs2Combat.inCombat()) {
                    hijackTimer++;
                } else {
                    hijackTimer = 0;
                }

                if (hijackTimer > 10) {
                    ammoniteCrabState = AmmoniteCrabState.HOP_WORLD;
                }

                if (Microbot.getClient().getLocalPlayer().getWorldLocation().distanceTo(config.crabLocation().getWorldhopLocation()) > 10 && (ammoniteCrabState != AmmoniteCrabState.RESET_AGGRO && ammoniteCrabState != AmmoniteCrabState.WALK_BACK && ammoniteCrabState != AmmoniteCrabState.BANK)) {
                    ammoniteCrabState = AmmoniteCrabState.WALK_BACK;
                    resetAggro(config);
                    resetAfkTimer();
                }

                if (Rs2Combat.inCombat() && ammoniteCrabState != AmmoniteCrabState.AFK && ammoniteCrabState != AmmoniteCrabState.FIGHT) {
                    // If already in combat, ensure we're in FIGHT state
                    ammoniteCrabState = AmmoniteCrabState.FIGHT;
                }
                // Only scan if we're not already doing something else important and not in combat
                else if (ammoniteCrabState != AmmoniteCrabState.RESET_AGGRO &&
                        ammoniteCrabState != AmmoniteCrabState.WALK_BACK &&
                        ammoniteCrabState != AmmoniteCrabState.HOP_WORLD &&
                        ammoniteCrabState != AmmoniteCrabState.BANK &&
                        ammoniteCrabState != AmmoniteCrabState.FIGHT &&
                        ammoniteCrabState != AmmoniteCrabState.AFK) {
                    ammoniteCrabState = AmmoniteCrabState.SCANNING_WORLD;
                }

                if (config.useFood() || config.usePotions()) {
                    Rs2Player.eatAt(50);
                    usePotions(config);
                    if (Rs2Inventory.getInventoryFood().isEmpty()) {
                        ammoniteCrabState = AmmoniteCrabState.BANK;
                        Rs2Bank.walkToBank(BankLocation.FOSSIL_ISLAND);
                        if (Rs2Bank.useBank()) {
                            if (config.usePotions()) {
                                Rs2Bank.withdrawX(config.potions().getPotionName() + "(4)", config.withdrawNumber());
                            }
                            Rs2Bank.withdrawAll(config.food().getName(), true);
                            ammoniteCrabState = AmmoniteCrabState.WALK_BACK;
                        }
                        return;
                    }
                }

                switch (ammoniteCrabState) {
                    case FIGHT:
                        if (!Microbot.getClient().getLocalPlayer().isInteracting() && !Rs2Combat.inCombat()) {
                            Rs2Tab.switchToCombatOptionsTab();
                            Rs2Combat.enableAutoRetialiate();
                        }

                        if (Rs2Player.getWorldLocation().distanceTo(config.crabLocation().getFightLocation()) > 0) {
                            Rs2Walker.walkFastCanvas(config.crabLocation().getFightLocation());
                        }

                        if (!isNpcAggressive() || afkTimer >= 10) {
                            ammoniteCrabState = AmmoniteCrabState.AFK;
                        }
                        break;
                    case AFK:
                        if (afkTimer < 10) {
                            afkTimer++;
                        }

                        if (Rs2Combat.inCombat()) {
                            resetAfkTimer();
                        }
                        if (afkTimer >= 10) {
                            if (otherPlayerDetected(config.crabLocation().getFightLocation())) {
                                Rs2Walker.walkTo(config.crabLocation().getWorldhopLocation());
                                ammoniteCrabState = AmmoniteCrabState.HOP_WORLD;
                            } else {
                                // If no other players detected, reset aggro
                                ammoniteCrabState = AmmoniteCrabState.RESET_AGGRO;
                            }
                        }
                        break;
                    case RESET_AGGRO:
                        resetAggro(config);
                        break;
                    case WALK_BACK:
                        walkBack(config);
                        break;
                    case HOP_WORLD:
                        int world = Login.getRandomWorld(true, null);
                        boolean isHopped = Microbot.hopToWorld(world);
                        if (isHopped) {
                            boolean result = sleepUntil(() -> Rs2Widget.findWidget("Switch World") != null);
                            if (result) {
                                Rs2Keyboard.keyPress(KeyEvent.VK_SPACE);
                                sleepUntil(() -> Microbot.getClient().getGameState() == GameState.HOPPING);
                                sleepUntil(() -> Microbot.getClient().getGameState() == GameState.LOGGED_IN);
                                sleep(1200,2000);
                                // After successful hop, reset counters and scan the area
                                timesHopped = 0;
                                hijackTimer = 0;
                                ammoniteCrabState = AmmoniteCrabState.SCANNING_WORLD;
                            }
                        } else {
                            // If failed to hop, increment counter and try again if under limit
                            timesHopped++;
                            if (timesHopped > 10) {
                                // If too many failed hops, reset and try regular play
                                timesHopped = 0;
                                ammoniteCrabState = AmmoniteCrabState.SCANNING_WORLD;
                            }
                        }
                        break;
                    case SCANNING_WORLD:
                        scanAmmoniteCrabLocation(config);
                        break;
                }
            } catch (Exception ex) {
                Microbot.logStackTrace(this.getClass().getSimpleName(), ex);
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
        return true;
    }

    private void scanAmmoniteCrabLocation(AmmoniteCrabConfig config) {
        // We most likely already round a world, try to fight
        if (Rs2Player.isInCombat()) {
            ammoniteCrabState = AmmoniteCrabState.FIGHT;
            return;
        }
        // First check if we need to walk to the scanning location
        if (config.crabLocation().getWorldhopLocation().distanceTo(Microbot.getClient().getLocalPlayer().getWorldLocation()) > 2) {
            Rs2Walker.walkTo(config.crabLocation().getWorldhopLocation());
            return; // Exit and continue scanning on next tick when closer
        }

        // Now check if our fighting spot is occupied
        if (otherPlayerDetected(config.crabLocation().getFightLocation())) {
            ammoniteCrabState = AmmoniteCrabState.HOP_WORLD;
            return;
        } else {
            // Location is clear, walk to fighting spot
            Rs2Walker.walkTo(config.crabLocation().getFightLocation(), 0);
            if (Rs2Player.getWorldLocation().distanceTo(config.crabLocation().getFightLocation()) <= 3) {
                // We're close enough to fight spot, transition to fighting
                resetAfkTimer();
                ammoniteCrabState = AmmoniteCrabState.FIGHT;
            } else {
                // Keep trying to get to the fight location
                Rs2Walker.walkFastCanvas(config.crabLocation().getFightLocation());
            }
        }
    }

    private void usePotions(AmmoniteCrabConfig config) {
        if (config.usePotions()) {
            var potions = Rs2Inventory.getFilteredPotionItemsInInventory(config.potions().getPotionName());
            if (Rs2Player.getBoostedSkillLevel(config.potions().getBoostedSkill()) - Rs2Player.getRealSkillLevel(config.potions().getBoostedSkill()) < 3 && !potions.isEmpty()) {
                Rs2Inventory.interact(potions.get(0), "Drink");
            }
        }
    }

    private void walkBack(AmmoniteCrabConfig config) {
        Rs2Walker.walkTo(config.crabLocation().getFightLocation());
        if (Microbot.getClient().getLocalPlayer().getWorldLocation().distanceTo(config.crabLocation().getFightLocation()) <= 3) {
            attackScatteredCrabs(config);
            resetAfkTimer();
            ammoniteCrabState = AmmoniteCrabState.FIGHT;
        }
    }

    // Attack all scattered crabs in the area
    private void attackScatteredCrabs(AmmoniteCrabConfig config) {
        var ammoniteCrabs = Rs2Npc.getNpcs("Ammonite Crab", true).filter(x -> x != null && !x.isDead() && x.getWorldLocation().distanceTo(config.crabLocation().getFightLocation()) > 1 && x.getWorldLocation().distanceTo(config.crabLocation().getFightLocation()) < 15).collect(Collectors.toList());
        for (Rs2NpcModel ammoniteCrab : ammoniteCrabs) {
            if (ammoniteCrab != null && !ammoniteCrab.isDead()) {
                Rs2Npc.attack(ammoniteCrab);
                Rs2Player.waitForAnimation(1600);
                sleep(1600,2400);
            }
        }
    }

    /**
     * Checks if there are fossil rocks spawned next to the player
     * This is used to know if the aggro timer has ran out
     *
     * @return true if npc is aggressive
     */
    private boolean isNpcAggressive() {
        List<Rs2NpcModel> npcs = Rs2Npc.getNpcs("Fossil Rock", true).collect(Collectors.toList());
        if (npcs.isEmpty()) {
            return true;
        }
        for (NPC ammoniteRock : npcs) {
            //ignore ammonitecrabs far away from the player
            if (!ammoniteRock.getWorldArea().isInMeleeDistance(Microbot.getClient().getLocalPlayer().getWorldArea()))
                continue;

            return false; //found a fossil rock crab near the player
        }
        return true; //did not find any fossil rocks near the player
    }

    private void resetAggro(AmmoniteCrabConfig config) {
        Rs2Walker.walkTo(config.crabLocation().getResetLocation());
        if (Microbot.getClient().getLocalPlayer().getWorldLocation().distanceTo(config.crabLocation().getResetLocation()) <= 3) {
            ammoniteCrabState = AmmoniteCrabState.WALK_BACK;
        }
    }

    /**
     * Reset afk timer and sets state back to fight
     */
    private void resetAfkTimer() {
        afkTimer = 0;
        // Only change state if we're currently in AFK state
        if (ammoniteCrabState == AmmoniteCrabState.AFK) {
            ammoniteCrabState = AmmoniteCrabState.FIGHT;
        }
    }

    private boolean otherPlayerDetected() {
        return otherPlayerDetected(Microbot.getClient().getLocalPlayer().getWorldLocation());
    }

    private boolean otherPlayerDetected(WorldPoint worldPoint) {
        for (Rs2PlayerModel player : Rs2Player.getPlayers(player -> true).collect(Collectors.toList())) {
            if (player.getWorldLocation().distanceTo(worldPoint) > 3)
                continue;
            return true;
        }
        return false;
    }
}