package com.osroyale.game.task.impl;

import com.osroyale.game.task.TickableTask;
import com.osroyale.game.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class SystemUpdateEvent extends TickableTask {

    private static final Logger logger = LogManager.getLogger();

    /** Total number of game ticks for the countdown (1 tick = 600ms). */
    private final int totalTicks;

    /**
     * @param totalTicks the total number of game ticks to count down
     */
    public SystemUpdateEvent(int totalTicks) {
        super(false, 1); // runs every single game tick
        this.totalTicks = totalTicks;
    }

    @Override
    protected void tick() {
        int remaining = totalTicks - tick;
        int remainingSeconds = remaining * 600 / 1000;

        // Log every 10 seconds worth of ticks (~17 ticks)
        if (remaining % 17 == 0 || remaining <= 10) {
            logger.info("Server shutdown in " + remainingSeconds + " seconds (" + remaining + " ticks)");
        }

        // Save all data 6 seconds before shutdown
        if (remaining == 10) {
            World.save();
        }

        if (remaining <= 0) {
            cancel();
        }
    }

    @Override
    protected void onCancel(boolean logout) {
        World.shutdown();
    }

}
