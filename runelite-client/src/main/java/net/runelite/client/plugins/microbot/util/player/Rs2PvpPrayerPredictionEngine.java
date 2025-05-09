package net.runelite.client.plugins.microbot.util.player;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.HeadIcon;
import net.runelite.client.plugins.microbot.util.combat.armor.ArmorsGenerator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A utility class for predicting opponent prayer changes in PvP scenarios.
 * <p>
 * This engine tracks opponent prayer switching patterns and attempts to predict their
 * next prayer choice based on historical data and common switching patterns.
 * </p>
 */
@Slf4j
public class Rs2PvpPrayerPredictionEngine {

    /**
     * The maximum size of the prayer switch history to maintain.
     */
    private static final int PREDICTION_HISTORY_SIZE = 10;

    /**
     * The confidence threshold for making a prediction.
     */
    private static final double CONFIDENCE_THRESHOLD = 0.65;

    /**
     * Map storing prayer history for each opponent player.
     */
    private final Map<String, Deque<Prayer>> playerPrayerHistoryMap = new ConcurrentHashMap<>();

    /**
     * Map storing the last predicted prayer for each opponent.
     */
    private final Map<String, Prayer> lastPredictedPrayer = new ConcurrentHashMap<>();

    public void init()
    {
        ArmorsGenerator.getInstance();

    }

    /**
     * Records a prayer change for the specified player.
     *
     * @param player The player whose prayer changed
     * @param prayer The new prayer that was activated
     */
    public void recordPrayerChange(Rs2PlayerModel player, Prayer prayer) {
        if (player == null) return;

        String playerName = player.getName();
        Deque<Prayer> history = playerPrayerHistoryMap.computeIfAbsent(playerName, k -> new LinkedList<>());

        // Don't record duplicate consecutive prayers
        if (!history.isEmpty() && history.peekLast() == prayer) {
            return;
        }

        history.add(prayer);

        // Keep history size limited
        while (history.size() > PREDICTION_HISTORY_SIZE) {
            history.removeFirst();
        }

        log.debug("Recorded prayer change for {}: {}", playerName, prayer);
    }

    /**
     * Predicts the next prayer an opponent is likely to use based on their history.
     *
     * @param player The player to predict for
     * @return The predicted prayer, or null if confidence is too low
     */
    public Prayer predictNextPrayer(Rs2PlayerModel player) {
        if (player == null) return null;

        String playerName = player.getName();
        Deque<Prayer> history = playerPrayerHistoryMap.get(playerName);

        if (history == null || history.size() < 3) {
            return null; // Not enough data to make a prediction
        }

        // Convert to list for easier pattern analysis
        List<Prayer> prayerList = new ArrayList<>(history);

        // Check for immediate alternating patterns (ABABAB)
        if (prayerList.size() >= 6) {
            boolean alternatingPattern = true;
            Prayer a = prayerList.get(prayerList.size() - 2);
            Prayer b = prayerList.get(prayerList.size() - 1);

            for (int i = prayerList.size() - 3; i >= prayerList.size() - 6 && i >= 0; i -= 2) {
                if (prayerList.get(i) != a) {
                    alternatingPattern = false;
                    break;
                }
                if (i > 0 && prayerList.get(i-1) != b) {
                    alternatingPattern = false;
                    break;
                }
            }

            if (alternatingPattern) {
                // Next in alternating would be A again
                lastPredictedPrayer.put(playerName, a);
                return a;
            }
        }

        // Check for repeating sequences of 2-3 prayers
        Map<List<Prayer>, Prayer> sequenceMap = new HashMap<>();
        for (int i = 0; i < prayerList.size() - 3; i++) {
            List<Prayer> sequence = prayerList.subList(i, i + 3);
            Prayer nextPrayer = i + 3 < prayerList.size() ? prayerList.get(i + 3) : null;

            if (nextPrayer != null) {
                sequenceMap.put(sequence, nextPrayer);
            }
        }

        // Look for the current sequence in our history
        if (prayerList.size() >= 3) {
            List<Prayer> currentSequence = prayerList.subList(prayerList.size() - 3, prayerList.size());
            Prayer prediction = sequenceMap.get(currentSequence);

            if (prediction != null) {
                lastPredictedPrayer.put(playerName, prediction);
                return prediction;
            }
        }

        // Fallback to frequency analysis if no clear pattern
        Map<Prayer, Integer> frequencyMap = new HashMap<>();
        for (Prayer p : prayerList) {
            frequencyMap.put(p, frequencyMap.getOrDefault(p, 0) + 1);
        }

        // Find the most common prayer
        Prayer mostCommon = null;
        int maxCount = 0;
        for (Map.Entry<Prayer, Integer> entry : frequencyMap.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                mostCommon = entry.getKey();
            }
        }

        // Check if the most common prayer occurs with high enough frequency
        double confidence = (double) maxCount / prayerList.size();
        if (confidence >= CONFIDENCE_THRESHOLD) {
            lastPredictedPrayer.put(playerName, mostCommon);
            return mostCommon;
        }

        // Return the last prayer if we can't make a confident prediction
        return prayerList.get(prayerList.size() - 1);
    }

    /**
     * Determines the current active overhead prayer for a player.
     *
     * @param player The player to check
     * @return The detected Prayer, or null if no overhead prayer is active
     */
    public Prayer detectActivePrayer(Rs2PlayerModel player) {
        if (player == null) return null;

        HeadIcon headIcon = player.getOverheadIcon();
        if (headIcon == null) return null;

        switch (headIcon) {
            case MAGIC:
                return Prayer.PROTECT_FROM_MAGIC;
            case RANGED:
                return Prayer.PROTECT_FROM_MISSILES;
            case MELEE:
                return Prayer.PROTECT_FROM_MELEE;
            case SMITE:
                return Prayer.SMITE;
            case REDEMPTION:
                return Prayer.REDEMPTION;
            case RETRIBUTION:
                return Prayer.RETRIBUTION;
            default:
                return null;
        }
    }

    /**
     * Updates prayer history for all visible opponents and returns the predicted prayer
     * for a specific target.
     *
     * @param target The target player to predict for
     * @param allPlayers All visible players
     * @return The predicted prayer for the target, or null if no prediction can be made
     */
    public Prayer updateAndPredict(Rs2PlayerModel target, List<Rs2PlayerModel> allPlayers) {
        // Update all visible players' prayer history
        for (Rs2PlayerModel player : allPlayers) {
            if (!Rs2Pvp.isAttackable(player)) continue;

            Prayer activePrayer = detectActivePrayer(player);
            if (activePrayer != null) {
                recordPrayerChange(player, activePrayer);
            }
        }

        // Return prediction for target
        if (target != null && Rs2Pvp.isAttackable(target)) {
            return predictNextPrayer(target);
        }

        return null;
    }

    /**
     * Gets the last predicted prayer for a specific player.
     *
     * @param playerName The name of the player
     * @return The last predicted prayer, or null if no prediction was made
     */
    public Prayer getLastPrediction(String playerName) {
        return lastPredictedPrayer.get(playerName);
    }

    /**
     * Clears the prayer history for a specific player.
     *
     * @param playerName The name of the player
     */
    public void clearPlayerHistory(String playerName) {
        playerPrayerHistoryMap.remove(playerName);
        lastPredictedPrayer.remove(playerName);
    }

    /**
     * Clears all prayer history data.
     */
    public void clearAllHistory() {
        playerPrayerHistoryMap.clear();
        lastPredictedPrayer.clear();
    }
}