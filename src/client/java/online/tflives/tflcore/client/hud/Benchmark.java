package online.tflives.tflcore.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import online.tflives.tflcore.client.TflCoreClient;

import java.util.Locale;

/**
 * Reproducible manual benchmark.
 *
 * <p>The user starts a window, plays normally, and receives average / p95 / p99 frame times plus the
 * sampled FPS. Results are written to the log. TFL Core never claims performance gains without
 * comparable runs on the same scene and settings.</p>
 */
public final class Benchmark {
    private static boolean running;
    private static long endNanos;
    private static int seconds;
    private static String lastResult = "";

    private Benchmark() {
    }

    public static void start(Minecraft client, int durationSeconds) {
        seconds = Math.max(3, Math.min(durationSeconds, 120));
        Diagnostics.reset();
        InputTracker.reset();
        running = true;
        endNanos = System.nanoTime() + seconds * 1_000_000_000L;
        lastResult = "";
        TflCoreClient.notifyPlayer(client, Component.translatable("tflcore.bench.started", seconds));
    }

    public static void tick(Minecraft client) {
        if (!running) return;
        if (System.nanoTime() < endNanos) return;
        running = false;
        int samples = Diagnostics.samples();
        if (samples < 8) {
            lastResult = Component.translatable("tflcore.bench.no_samples").getString();
        } else {
            lastResult = String.format(Locale.ROOT,
                    "avg %.2f ms | p50 %.2f | p95 %.2f | p99 %.2f | %.1f fps | %d samples",
                    Diagnostics.averageMillis(),
                    Diagnostics.percentileMillis(50),
                    Diagnostics.percentileMillis(95),
                    Diagnostics.percentileMillis(99),
                    1000.0 / Math.max(0.001, Diagnostics.averageMillis()),
                    samples);
        }
        org.slf4j.LoggerFactory.getLogger("tflcore").info("Benchmark ({}s): {}", seconds, lastResult);
        TflCoreClient.notifyPlayer(client, Component.literal(lastResult));
    }

    public static boolean running() {
        return running;
    }

    public static String lastResult() {
        return lastResult;
    }

    public static long remainingMillis() {
        return running ? Math.max(0, (endNanos - System.nanoTime()) / 1_000_000L) : 0;
    }
}
