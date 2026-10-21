package online.tflives.tflcore.client.ui;

/**
 * Shared motion timing for interactive widgets.
 *
 * <p>The settings screen exposes a transition duration; widgets convert it to a per-frame step
 * using the render partial tick. Reduced motion forces an immediate state change so the UI never
 * animates for users who asked for less motion.</p>
 */
public final class TflMotion {
    private static float transitionSeconds = 0.16F;
    private static boolean reduced;

    private TflMotion() {
    }

    public static void configure(int transitionMillis, boolean reducedMotion) {
        reduced = reducedMotion;
        transitionSeconds = Math.max(0.001F, Math.min(transitionMillis, 2000) / 1000F);
    }

    /** Returns the fraction of a transition to apply for this frame (0..1). */
    public static float step(float partialTick) {
        if (reduced) {
            return 1F;
        }
        float tickSeconds = Math.max(0F, partialTick) * 0.05F;
        return Math.min(1F, tickSeconds / transitionSeconds);
    }
}
