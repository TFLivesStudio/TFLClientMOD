package online.tflives.tflcore.client.hud;

import java.util.ArrayDeque;

/**
 * Client input telemetry for the CPS widget.
 *
 * <p>Only the local player's own mouse state is observed, at frame rate (HUD render) with a tick
 * fallback. Nothing is recorded to disk and nothing leaves the client.</p>
 */
public final class InputTracker {
    private static final long WINDOW_NANOS = 1_000_000_000L;
    private static final ArrayDeque<Long> LEFT = new ArrayDeque<>();
    private static final ArrayDeque<Long> RIGHT = new ArrayDeque<>();
    private static boolean lastLeft;
    private static boolean lastRight;
    private static long lastAttackNanos;

    private InputTracker() {
    }

    public static void update(boolean leftDown, boolean rightDown) {
        long now = System.nanoTime();
        if (leftDown && !lastLeft) {
            LEFT.addLast(now);
            lastAttackNanos = now;
        }
        if (rightDown && !lastRight) {
            RIGHT.addLast(now);
        }
        lastLeft = leftDown;
        lastRight = rightDown;
        trim(LEFT, now);
        trim(RIGHT, now);
    }

    private static void trim(ArrayDeque<Long> deque, long now) {
        while (!deque.isEmpty() && now - deque.peekFirst() > WINDOW_NANOS) {
            deque.removeFirst();
        }
        while (deque.size() > 64) {
            deque.removeFirst();
        }
    }

    public static int leftCps() {
        trim(LEFT, System.nanoTime());
        return LEFT.size();
    }

    public static int rightCps() {
        trim(RIGHT, System.nanoTime());
        return RIGHT.size();
    }

    /** Nanoseconds since the last left click, or -1 when nothing was clicked this session. */
    public static long nanosSinceAttack() {
        return lastAttackNanos == 0 ? -1 : System.nanoTime() - lastAttackNanos;
    }

    public static long lastAttackNanos() {
        return lastAttackNanos;
    }

    public static void reset() {
        LEFT.clear();
        RIGHT.clear();
        lastLeft = false;
        lastRight = false;
        lastAttackNanos = 0;
    }
}
