package dev.shymike.upgradedchat.client.features;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class TickScheduler {
    private static boolean shouldRun = false;
    private static Runnable task;

    public static void scheduleForNextTick(Runnable runnable) {
        task = runnable;
        shouldRun = true;
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (shouldRun) {
                shouldRun = false;
                Runnable toRun = task;
                task = null;
                if (toRun != null) toRun.run();
            }
        });
    }
}
