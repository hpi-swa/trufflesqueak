package de.hpi.swa.trufflesqueak.util;

public class InterruptHandler {
    private int nextWakeupTick;

    public void nextWakeupTick(int msTime) {
        nextWakeupTick = msTime;
    }
}
