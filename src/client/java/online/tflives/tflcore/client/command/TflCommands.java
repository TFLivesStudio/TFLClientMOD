package online.tflives.tflcore.client.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import online.tflives.tflcore.client.TflCoreClient;
import online.tflives.tflcore.client.config.SettingsStore;
import online.tflives.tflcore.client.config.Waypoint;
import online.tflives.tflcore.client.hud.Benchmark;
import online.tflives.tflcore.client.profile.ClientProfile;
import online.tflives.tflcore.client.survival.WaypointKeys;
import online.tflives.tflcore.client.ui.TflDiagnosticsScreen;
import online.tflives.tflcore.client.ui.TflHudEditorScreen;

import java.nio.file.Path;
import java.util.List;

/**
 * Local client commands. They only read local settings and client state; no command contacts a
 * server or transmits data.
 */
public final class TflCommands {
    private TflCommands() {
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommands.literal("tflcore")
                .then(ClientCommands.literal("diag").executes(context -> {
                    Minecraft client = context.getSource().getClient();
                    client.gui.setScreen(new TflDiagnosticsScreen(client.gui.screen()));
                    return 1;
                }))
                .then(ClientCommands.literal("hud").executes(context -> {
                    Minecraft client = context.getSource().getClient();
                    if (client.player == null) {
                        context.getSource().sendFeedback(Component.translatable("tflcore.notice.world_required"));
                        return 0;
                    }
                    client.gui.setScreen(new TflHudEditorScreen(client.gui.screen()));
                    return 1;
                }))
                .then(ClientCommands.literal("profile")
                        .then(ClientCommands.argument("name", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    for (ClientProfile profile : ClientProfile.values()) {
                                        builder.suggest(profile.name().toLowerCase(java.util.Locale.ROOT));
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    String name = StringArgumentType.getString(context, "name");
                                    ClientProfile profile = ClientProfile.fromName(name);
                                    if (profile == ClientProfile.CUSTOM) {
                                        context.getSource().sendError(
                                                Component.translatable("tflcore.command.unknown_profile", name));
                                        return 0;
                                    }
                                    TflCoreClient.applyProfile(profile);
                                    context.getSource().sendFeedback(
                                            Component.translatable("tflcore.command.profile_applied", profile.name()));
                                    return 1;
                                })))
                .then(ClientCommands.literal("waypoint")
                        .then(ClientCommands.literal("add")
                                .executes(context -> add(context.getSource(), null))
                                .then(ClientCommands.argument("name", StringArgumentType.greedyString())
                                        .executes(context -> add(context.getSource(),
                                                StringArgumentType.getString(context, "name")))))
                        .then(ClientCommands.literal("list").executes(context -> {
                            Minecraft client = context.getSource().getClient();
                            List<Waypoint> list = TflCoreClient.settings().get().waypointsFor(
                                    WaypointKeys.world(client), WaypointKeys.dimension(client));
                            if (list.isEmpty()) {
                                context.getSource().sendFeedback(
                                        Component.translatable("tflcore.command.no_waypoints"));
                                return 0;
                            }
                            for (int i = 0; i < list.size(); i++) {
                                Waypoint waypoint = list.get(i);
                                context.getSource().sendFeedback(Component.literal(
                                        (i + 1) + ". " + waypoint.name + "  "
                                                + Math.round(waypoint.x) + " / "
                                                + Math.round(waypoint.y) + " / "
                                                + Math.round(waypoint.z)));
                            }
                            return 1;
                        }))
                        .then(ClientCommands.literal("remove")
                                .then(ClientCommands.argument("index", IntegerArgumentType.integer(1))
                                        .executes(context -> {
                                            Minecraft client = context.getSource().getClient();
                                            int index = IntegerArgumentType.getInteger(context, "index") - 1;
                                            int remaining = TflCoreClient.waypoints().removeAt(
                                                    WaypointKeys.world(client), WaypointKeys.dimension(client), index);
                                            if (remaining < 0) {
                                                context.getSource().sendError(
                                                        Component.translatable("tflcore.command.bad_index"));
                                                return 0;
                                            }
                                            context.getSource().sendFeedback(
                                                    Component.translatable("tflcore.command.waypoint_removed"));
                                            return 1;
                                        }))))
                .then(ClientCommands.literal("bench")
                        .executes(context -> bench(context.getSource(),
                                TflCoreClient.settings().get().perf.benchmarkSeconds))
                        .then(ClientCommands.argument("seconds", IntegerArgumentType.integer(3, 120))
                                .executes(context -> bench(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "seconds")))))
                .then(ClientCommands.literal("export").executes(context -> {
                    Path target = TflCoreClient.settings().configRoot().resolve("tflcore-export.json");
                    SettingsStore.ImportResult result = TflCoreClient.settings().exportTo(target);
                    context.getSource().sendFeedback(Component.translatable(
                            "tflcore.command.export", result.name(), target.toString()));
                    return result == SettingsStore.ImportResult.OK ? 1 : 0;
                })));
    }

    private static int bench(FabricClientCommandSource source, int seconds) {
        if (source.getClient().player == null) {
            source.sendError(Component.translatable("tflcore.notice.world_required"));
            return 0;
        }
        Benchmark.start(source.getClient(), seconds);
        return 1;
    }

    private static int add(FabricClientCommandSource source, String name) {
        Minecraft client = source.getClient();
        if (client.player == null) {
            source.sendError(Component.translatable("tflcore.notice.world_required"));
            return 0;
        }
        String label = name == null || name.isBlank()
                ? Component.translatable("tflcore.waypoint.default_name").getString()
                : name;
        if (!TflCoreClient.waypoints().addAtPlayer(client, label)) {
            source.sendError(Component.translatable("tflcore.notice.world_required"));
            return 0;
        }
        source.sendFeedback(Component.translatable("tflcore.command.waypoint_added", label));
        return 1;
    }
}
