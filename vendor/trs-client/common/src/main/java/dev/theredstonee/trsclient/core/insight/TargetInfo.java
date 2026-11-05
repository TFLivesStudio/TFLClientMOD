package dev.theredstonee.trsclient.core.insight;

import java.util.*;

/** Only data already available to the client. Never requests block-entity inventories or server data. */
public final class TargetInfo {
    public final String name, id, origin;
    public final List<String> properties;
    public final Object icon;
    public final String tool;
    public final Float health, maxHealth;
    public TargetInfo(String name, String id, String origin, List<String> properties, Object icon, Float health, Float maxHealth) {
        this(name, id, origin, properties, icon, health, maxHealth, null);
    }
    public TargetInfo(String name, String id, String origin, List<String> properties, Object icon, Float health, Float maxHealth, String tool) {
        this.tool = tool == null ? "" : tool;
        this.name = name == null ? "" : name;
        this.id = id == null ? "" : id;
        this.origin = origin == null ? "" : origin;
        this.properties = Collections.unmodifiableList(new ArrayList<String>(properties));
        this.icon = icon;
        this.health = finite(health); this.maxHealth = finite(maxHealth);
    }
    private static Float finite(Float value) { return value == null || Float.isNaN(value) || Float.isInfinite(value) || value < 0 ? null : value; }
    public List<String> lines(WorldInsightModule settings) {
        List<String> result = new ArrayList<String>();
        result.add(name);
        if (settings.identifiers.get() && !id.isEmpty()) result.add(id);
        if (settings.origin.get() && !origin.isEmpty()) result.add(origin);
        if (settings.properties.get()) result.addAll(properties);
        if (settings.tool.get() && !tool.isEmpty()) result.add(dev.theredstonee.trsclient.core.i18n.I18n.tr("insight.tool", tool));
        if (settings.health.get() && health != null) result.add(String.format(Locale.ROOT, "%.1f%s ♥", health, maxHealth == null ? "" : String.format(Locale.ROOT, " / %.1f", maxHealth)));
        return result;
    }
}
