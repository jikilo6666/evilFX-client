package dev.evilfx.feature;

/** Client tick counter that only advances while a world is loaded. Shared by the trackers. */
public final class Clock {
    public static int ticks;

    private Clock() {}

    public static void reset() {
        ticks = 0;
    }
}
