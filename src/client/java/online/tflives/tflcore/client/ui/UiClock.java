package online.tflives.tflcore.client.ui;

/** Monotonic UI time driven by client ticks, never by wall-clock time. */
public final class UiClock {
    private static float seconds;
    private static float lastDelta;

    private UiClock() {
    }

    public static void tick() {
        seconds += 0.05F;
        if (seconds > 1_000_000F) {
            seconds = 0F;
        }
    }

    public static float seconds() {
        return seconds;
    }

    /** Smooth value for render callbacks; {@code partialTick} is the screen delta (0..1). */
    public static float seconds(float partialTick) {
        lastDelta = partialTick;
        return seconds + partialTick * 0.05F;
    }

    public static float lastDelta() {
        return lastDelta;
    }

    public static void reset() {
        seconds = 0F;
    }
}
