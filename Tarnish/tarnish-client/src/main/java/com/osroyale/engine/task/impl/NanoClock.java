package com.osroyale.engine.task.impl;

import com.osroyale.Client;
import com.osroyale.engine.task.Clock;
import com.osroyale.engine.task.TaskUtils;
import net.runelite.rs.api.RSNanoClock;

public class NanoClock extends Clock implements RSNanoClock {

    long lastTimeNano;
    private long tmpNanoTime;

    public NanoClock() {
        this.lastTimeNano = System.nanoTime();
    }

    public void mark() {
        this.lastTimeNano = System.nanoTime();
    }

    public int wait(int var1, int var2) {
        if (Client.instance.isUnlockedFps() && Client.instance.getRSGameState() >= 25) {

            long nanoTime = System.nanoTime();

            if (nanoTime >= getLastTimeNano() && nanoTime >= tmpNanoTime) {
                long cycleDuration;
                long diff;

                if (Client.instance.getUnlockedFpsTarget() > 0L) {
                    cycleDuration = nanoTime - tmpNanoTime;
                    diff = Client.instance.getUnlockedFpsTarget() - cycleDuration;
                    diff /= 1000000L;
                    if (diff > 0L) {
                        try {
                            if (diff % 10L == 0L) {
                                Thread.sleep(diff - 1L);
                                Thread.sleep(1L);
                            } else {
                                Thread.sleep(diff);
                            }
                        } catch (InterruptedException exception) {
                            exception.printStackTrace();
                        }

                        nanoTime = System.nanoTime();
                    }
                }

                tmpNanoTime = nanoTime;

                cycleDuration = (long) var1 * 1000000L;
                diff = nanoTime - getLastTimeNano();

                int cycles = (int) (diff / cycleDuration);

                setLastTimeNano(getLastTimeNano() + (long) cycles * cycleDuration);

                if (cycles > 10) {
                    cycles = 10;
                }

                return cycles;
            } else {
                setLastTimeNano(tmpNanoTime = nanoTime);

                return 1;
            }
        } else {
            long remainingNanos = this.lastTimeNano - System.nanoTime();

            if (remainingNanos > 1000000L) {
                // We have real time to spare before the next frame — sleep precisely
                TaskUtils.sleep(remainingNanos / 1000000L);
            } else if (remainingNanos > 0) {
                // Less than 1ms remaining — yield instead of sleeping (avoids 15ms Windows penalty)
                Thread.yield();
            }
            // If remainingNanos <= 0, we're behind schedule — don't sleep at all

            long var7 = System.nanoTime();

            int var9;
            for (var9 = 0; var9 < 10 && (var9 < 1 || this.lastTimeNano < var7); this.lastTimeNano += (long) var1 * 1000000L) {
                ++var9;
            }

            if (this.lastTimeNano < var7) {
                this.lastTimeNano = var7;
            }

            return var9;
        }
    }

    @Override
    public long getLastTimeNano() {
        return lastTimeNano;
    }

    @Override
    public void setLastTimeNano(long lastNanoTime) {
        this.lastTimeNano = lastNanoTime;
    }
}
