package net.runelite.client.plugins.microbot.example;

import net.runelite.api.HeadIcon;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.reflection.Rs2Reflection;

import java.util.concurrent.TimeUnit;


public class ExampleScript extends Script {

    public static boolean test = false;
    public boolean run(ExampleConfig config) {
        Microbot.enableAutoRunOn = false;
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;
                long startTime = System.currentTimeMillis();
                var objects = Rs2GameObject.getObjectIdsByName("Node");
                for (var o : objects) {
                    System.out.println(o);
                }

            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
        return true;
    }

    private Rs2NpcModel findNewTarget() {
        return Rs2Npc.getAttackableNpcs("Tormented Demon")
                .filter(npc -> npc.getInteracting() == null || npc.getInteracting() == Microbot.getClient().getLocalPlayer())
                .filter(npc -> {
                    HeadIcon demonHeadIcon = Rs2Reflection.getHeadIcon(npc);
                    if (demonHeadIcon != null) {
                        //switchGear(config, demonHeadIcon);
                        return true;
                    }
                    //logOnceToChat("Null HeadIcon for NPC " + npc.getName());
                    return false;
                })
                .findFirst()
                .orElse(null);
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }
    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        Microbot.log("EventId " + event.getId());
    }


}
