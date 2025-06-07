package net.runelite.client.plugins.microbot.TaF.ShopNBankstand.Model;

import lombok.Getter;

import java.util.List;

public class SequenceHandler {
    @Getter
    private static SequenceHandler instance;
    public SequenceHandler() {
        instance = this;
    }
    /**
     * This is the list of processing sequences that will be executed.
     * Each sequence will be processed in order, and once one sequence is completed,
     * the next one will be started.
     */
    private List<ProcessingSequence> processingSequence;
}
