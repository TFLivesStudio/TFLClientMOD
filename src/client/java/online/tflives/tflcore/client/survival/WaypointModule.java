package online.tflives.tflcore.client.survival;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import online.tflives.tflcore.client.config.SettingsStore;
import online.tflives.tflcore.client.config.Waypoint;
import online.tflives.tflcore.client.module.ModuleCategory;
import online.tflives.tflcore.client.module.TflModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Local waypoints (manual and death markers). Data never leaves the client; each waypoint is scoped
 * to the current world/server and dimension.
 */
public final class WaypointModule implements TflModule {
    private static final Logger LOGGER = LoggerFactory.getLogger("tflcore");
    private static final int MAX_DEATH_WAYPOINTS = 32;

    private final SettingsStore store;
    private boolean wasDead;

    public WaypointModule(SettingsStore store) {
        this.store = store;
    }

    @Override
    public String id() {
        return "waypoints";
    }

    @Override
    public ModuleCategory category() {
        return ModuleCategory.SURVIVAL;
    }

    @Override
    public void onTick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) {
            wasDead = false;
            return;
        }
        boolean dead = player.isDeadOrDying();
        if (dead && !wasDead) {
            wasDead = true;
            if (store.get().survival.deathWaypoint) {
                addDeathWaypoint(client);
            }
        } else if (!dead) {
            wasDead = false;
        }
    }

    public void addDeathWaypoint(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) return;
        String world = WaypointKeys.world(client);
        String dimension = WaypointKeys.dimension(client);
        Waypoint waypoint = new Waypoint(nextDeathName(client, world, dimension), world, dimension,
                player.getX(), player.getY(), player.getZ());
        waypoint.death = true;
        waypoint.color = 0xFFFF5555;
        List<Waypoint> waypoints = store.get().survival.waypoints;
        waypoints.add(waypoint);
        pruneDeathWaypoints(waypoints);
        store.get().normalize();
        store.requestSave();
        LOGGER.info("Recorded death waypoint at {}, {}, {} in {}", waypoint.x, waypoint.y, waypoint.z, dimension);
    }

    private String nextDeathName(Minecraft client, String world, String dimension) {
        int count = 0;
        for (Waypoint waypoint : store.get().survival.waypoints) {
            if (waypoint.death && waypoint.world.equals(world) && waypoint.dimension.equals(dimension)) {
                count++;
            }
        }
        return String.format(Locale.ROOT, "Death %d", count + 1);
    }

    private void pruneDeathWaypoints(List<Waypoint> waypoints) {
        int deaths = 0;
        for (Waypoint waypoint : waypoints) {
            if (waypoint.death) deaths++;
        }
        if (deaths <= MAX_DEATH_WAYPOINTS) return;
        Iterator<Waypoint> iterator = waypoints.iterator();
        while (iterator.hasNext() && deaths > MAX_DEATH_WAYPOINTS) {
            if (iterator.next().death) {
                iterator.remove();
                deaths--;
            }
        }
    }

    /** Adds a waypoint at the player's current position and selects it. */
    public boolean addAtPlayer(Minecraft client, String name) {
        LocalPlayer player = client.player;
        if (player == null) return false;
        String world = WaypointKeys.world(client);
        String dimension = WaypointKeys.dimension(client);
        String label = name == null || name.isBlank() ? "Waypoint" : name.strip();
        Waypoint waypoint = new Waypoint(label, world, dimension,
                player.getX(), player.getY(), player.getZ());
        store.get().survival.waypoints.add(waypoint);
        store.get().survival.activeWaypoint = Math.max(0, store.get().waypointsFor(world, dimension).size() - 1);
        store.get().normalize();
        store.requestSave();
        return true;
    }

    public int removeAt(String world, String dimension, int index) {
        List<Waypoint> waypoints = store.get().survival.waypoints;
        List<Waypoint> matching = store.get().waypointsFor(world, dimension);
        if (index < 0 || index >= matching.size()) return -1;
        Waypoint target = matching.get(index);
        waypoints.remove(target);
        store.get().survival.activeWaypoint = 0;
        store.requestSave();
        return matching.size() - 1;
    }
}
