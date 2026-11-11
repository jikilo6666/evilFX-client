package dev.evilfx.feature;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.List;

/**
 * Passive clicks-per-second counter. Polls the raw GLFW mouse state once per rendered frame and
 * records press edges in a one-second sliding window. It never sends or alters any input.
 * (Polling per frame, not per tick, so fast clicking is not undercounted.)
 */
public final class ClickTracker {
    private static final List<ArrayDeque<Long>> CLICKS = List.of(new ArrayDeque<>(), new ArrayDeque<>());
    private static final boolean[] DOWN = new boolean[2];

    private ClickTracker() {}

    public static void poll(MinecraftClient mc) {
        long window = mc.getWindow().getHandle();
        boolean inGame = mc.currentScreen == null;
        long now = Util.getMeasuringTimeMs();
        for (int b = 0; b < 2; b++) {
            boolean pressed = inGame && GLFW.glfwGetMouseButton(window, b) == GLFW.GLFW_PRESS;
            ArrayDeque<Long> q = CLICKS.get(b);
            if (pressed && !DOWN[b]) q.addLast(now);
            DOWN[b] = pressed;
            while (!q.isEmpty() && now - q.peekFirst() > 1000) q.pollFirst();
        }
    }

    public static int cps(int button) {
        return CLICKS.get(button).size();
    }

    public static boolean isDown(int button) {
        return DOWN[button];
    }
}
