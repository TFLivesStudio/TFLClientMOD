package dev.theredstonee.trsclient.core.product;

/** Optional development harness boundary. No direct linkage from an installed user JAR. */
public final class DevelopmentHooks {
    private DevelopmentHooks() {}
    public static void install() {
        if (!Boolean.getBoolean("trsclient.autotest")) return;
        try {
            Class.forName("dev.theredstonee.trsclient.dev.AutoTest").getMethod("installIfRequested").invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Development harness unavailable or failed; use the development classpath", e);
        }
    }
}
