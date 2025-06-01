package net.runelite.client.plugins.microbot.TaF.RefactoredBarrows;

public enum Houselocations {
    HOSIDIUS("Hosidius"),
    RIMMINGTON("Rimmington"),
    TAVERLEY("Taverley"),
    POLLVIVNEACH("Pollnivneach"),
    RELLEKA("Rellekka"),
    ALDARIN("Aldarin"),
    BRIMHAVEN("Brimhaven"),
    YANILLE("Yanille"),
    PRIFDDINAS("Prifddinas");

    Houselocations(String name) {
        this.name = name;
    }
    private final String name;
    public String getName() {
        return name;
    }
}
