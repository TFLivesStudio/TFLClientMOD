package dev.theredstonee.trsclient.core.product;

/** TFL modifications, 2026. Internal upstream packages/resource IDs remain compatible. */
public final class Product {
    public static final String ID = "tflcore";
    public static final String NAME = "TFL Client";
    public static final String SELECTION = "TFL Client";
    public static final String CONFIG = "tflcore/client.json";
    public static final String WAYPOINTS = "tflcore/waypoints.json";
    public static String displayName(String minecraftVersion) { return NAME + " " + minecraftVersion; }
    private Product() {}
    /** Technical recovery switch. Not persisted and never exposed in navigation. */
    public static boolean safeMode() { return Boolean.getBoolean("tflcore.safeMode"); }
    /** Only local, usable destinations belong in the TFL pause menu. */
    public static String[] pauseEntries() { return new String[]{"core", "clips", "serverInfo"}; }
    /** No approved TRS/TFL backend, launcher IPC, account or hosting integration is shipped. */
    public static boolean onlineServices() { return false; }
    /** Unit/integration harness only: permits literal loopback, never remote hosts or DNS aliases. */
    public static boolean testLoopback() { return Boolean.getBoolean("tflcore.test.loopback"); }
    public static void requireServiceHost(String host) throws java.io.IOException {
        if (testLoopback() && ("127.0.0.1".equals(host) || "::1".equals(host) || "[::1]".equals(host))) return;
        requireOnlineServices();
    }
    public static void requireServiceEndpoint(String url) throws java.io.IOException {
        try { requireServiceHost(java.net.URI.create(url).getHost()); }
        catch (IllegalArgumentException e) { throw new java.io.IOException("Invalid service URL", e); }
    }
    public static void requireOnlineServices() throws java.io.IOException {
        throw new java.io.IOException("Online services are unavailable in TFL Client; offline features remain available");
    }
}
