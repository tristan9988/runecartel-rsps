package com.osroyale.engine.task;

import java.util.concurrent.locks.LockSupport;

public class TaskUtils {

    // Windows Thread.sleep() has ~15.6ms granularity. Any sleep under this threshold
    // must use busy-wait to avoid sleeping 10-15x longer than requested.
    private static final long BUSY_WAIT_THRESHOLD_MS = 16L;

    /**
     * High-precision sleep that avoids the Windows ~15.6ms timer granularity issue.
     * For sleeps under 16ms, uses busy-wait via LockSupport.parkNanos.
     * For longer sleeps, sleeps most of the duration then busy-waits the remainder.
     */
    public static void sleep(long waitMs) {
        if (waitMs <= 0L) {
            return;
        }

        long deadline = System.nanoTime() + waitMs * 1_000_000L;

        if (waitMs < BUSY_WAIT_THRESHOLD_MS) {
            // Short sleeps: busy-wait entirely (Thread.sleep is too imprecise on Windows)
            while (System.nanoTime() < deadline) {
                LockSupport.parkNanos(50_000L); // 0.05ms parks to reduce CPU spin
            }
        } else {
            // Longer sleeps: Thread.sleep for bulk, then busy-wait the tail
            long safeSleepMs = waitMs - BUSY_WAIT_THRESHOLD_MS;
            if (safeSleepMs > 0L) {
                try {
                    Thread.sleep(safeSleepMs);
                } catch (InterruptedException exception) {
                    exception.printStackTrace();
                }
            }
            // Busy-wait the remaining ~16ms for precision
            while (System.nanoTime() < deadline) {
                LockSupport.parkNanos(50_000L);
            }
        }
    }

}
