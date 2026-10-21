package online.tflives.tflcore.client.hud;

import net.minecraft.client.Minecraft;

import java.util.Arrays;

/**
 * Bounded frame-time diagnostics.
 *
 * <p>Percentiles are computed from an actual per-frame buffer, not derived from instantaneous FPS.
 * The buffer is small (512 samples) and sorting only happens when the diagnostics UI asks for it.</p>
 */
public final class Diagnostics {
    private static final int CAPACITY = 512;
    private static final long[] FRAMES_NS = new long[CAPACITY];
    private static final long[] SORT_SCRATCH = new long[CAPACITY];
    private static int cursor;
    private static int count;
    private static long lastSampleNanos;

    private Diagnostics() {
    }

    /** Samples {@link Minecraft#getFrameTimeNs()} into the ring buffer. */
    public static void sampleFrame(Minecraft client) {
        if (client == null) return;
        long frameNs = client.getFrameTimeNs();
        if (frameNs <= 0 || frameNs > 5_000_000_000L) return;
        FRAMES_NS[cursor] = frameNs;
        cursor = (cursor + 1) % CAPACITY;
        if (count < CAPACITY) count++;
        lastSampleNanos = System.nanoTime();
    }

    private static long[] sortedSnapshot() {
        System.arraycopy(FRAMES_NS, 0, SORT_SCRATCH, 0, count);
        Arrays.sort(SORT_SCRATCH, 0, count);
        return SORT_SCRATCH;
    }

    public static int samples() {
        return count;
    }

    public static double averageMillis() {
        if (count == 0) return 0;
        long sum = 0;
        for (int i = 0; i < count; i++) {
            sum += FRAMES_NS[i];
        }
        return sum / (double) count / 1_000_000.0;
    }

    public static double percentileMillis(double percentile) {
        if (count == 0) return 0;
        long[] sorted = sortedSnapshot();
        int index = (int) Math.round((percentile / 100.0) * (count - 1));
        index = Math.max(0, Math.min(index, count - 1));
        return sorted[index] / 1_000_000.0;
    }

    public static double minMillis() {
        if (count == 0) return 0;
        long min = Long.MAX_VALUE;
        for (int i = 0; i < count; i++) min = Math.min(min, FRAMES_NS[i]);
        return min / 1_000_000.0;
    }

    public static double maxMillis() {
        if (count == 0) return 0;
        long max = 0;
        for (int i = 0; i < count; i++) max = Math.max(max, FRAMES_NS[i]);
        return max / 1_000_000.0;
    }

    public static long usedMemoryMb() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L);
    }

    public static long maxMemoryMb() {
        return Runtime.getRuntime().maxMemory() / (1024L * 1024L);
    }

    public static long lastSampleAgeMillis() {
        return lastSampleNanos == 0 ? -1 : (System.nanoTime() - lastSampleNanos) / 1_000_000L;
    }

    public static void reset() {
        cursor = 0;
        count = 0;
        lastSampleNanos = 0;
    }
}
