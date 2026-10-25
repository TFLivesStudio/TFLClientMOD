package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.module.HudModule;
import dev.theredstonee.trsclient.core.ui.Canvas;
import dev.theredstonee.trsclient.core.ui.menu.*;
import dev.theredstonee.trsclient.ornithe.capability.CapabilityRegistry.State;
import dev.theredstonee.trsclient.ornithe.mixin.GameGuiAccess;
import java.util.*;
import net.minecraft.client.gui.BossBar;
import net.minecraft.scoreboard.*;
import net.minecraft.scoreboard.team.Team;

/**
 * Existing sidebar/boss/title data presented through shared HUD layout/editor; no packet changes.
 */
public final class OrnitheVanillaHuds {
  private final OrnitheClient c;
  private final List<String> scores = new ArrayList<>(16);
  private int scoreWidth;

  public OrnitheVanillaHuds(OrnitheClient c) {
    this.c = c;
    c.capabilities.connect(
        c.modules.qol.tabPing,
        State.EXPERIMENTAL,
        "Native tab-list numeric latency and optional threshold colors; no active ping packet in"
            + " 1.8.9",
        java.util.List.of("colors"),
        "TabPingMixin.renderPing",
        "PlayerInfo.getPing");
    for (var m : List.of(c.modules.qol.scoreboard, c.modules.qol.bossBar, c.modules.qol.titles))
      c.capabilities.connect(
          m,
          State.LIMITED,
          "Native 1.8.9 scoreboard/boss/title data, shared HUD placement and editor. Vanilla"
              + " presentation resumes when disabled.",
          m.settings().stream().map(s -> s.key()).toList(),
          "OrnitheVanillaHuds.tick",
          "VanillaHudMixin",
          "GameGuiAccess",
          "HudLayout",
          "HudEditor");
    c.hudItems.add(
        new HudItem() {
          public HudModule module() {
            return c.modules.qol.scoreboard;
          }

          public int width() {
            return preview() && scores.isEmpty() ? 120 : scoreWidth;
          }

          public int height() {
            return (preview() && scores.isEmpty() ? 3 : scores.size()) * 11 + 8;
          }

          public void draw(Canvas canvas) {
            var lines =
                preview() && scores.isEmpty()
                    ? List.of("TFL Client", "Player  10", "Objective  25")
                    : scores;
            if (lines.isEmpty()) return;
            canvas.fill(0, 0, width(), height(), module().backgroundArgb());
            for (int i = 0; i < lines.size(); i++)
              canvas.text(
                  lines.get(i), 5, 4 + i * 11, module().textColor.argb(), module().shadow());
          }
        });
    c.hudItems.add(
        new HudItem() {
          public HudModule module() {
            return c.modules.qol.bossBar;
          }

          public int width() {
            return 192;
          }

          public int height() {
            return 28;
          }

          public void draw(Canvas canvas) {
            if (!preview() && (BossBar.name == null || BossBar.timer <= 0)) return;
            String title = preview() ? "Boss Bar" : BossBar.name;
            float health = preview() ? .75f : Math.max(0, Math.min(1, BossBar.health));
            canvas.fill(0, 0, width(), height(), module().backgroundArgb());
            canvas.text(
                canvas.clip(title, 182), 5, 4, module().textColor.argb(), module().shadow());
            canvas.fill(5, 17, 187, 23, 0xA020122A);
            canvas.fill(5, 17, 5 + Math.round(182 * health), 23, 0xFFC64CE0);
          }
        });
    c.hudItems.add(
        new HudItem() {
          public HudModule module() {
            return c.modules.qol.titles;
          }

          private GameGuiAccess data() {
            return (GameGuiAccess) c.mc.gui;
          }

          private String title() {
            return preview() ? "TFL Client" : data().tfl$title();
          }

          private String sub() {
            return preview() ? "Title preview" : data().tfl$subtitle();
          }

          public int width() {
            return Math.max(c.canvas.textWidth(title()) * 2, c.canvas.textWidth(sub())) + 10;
          }

          public int height() {
            return 42;
          }

          public void draw(Canvas canvas) {
            var d = data();
            if (!preview() && d.tfl$titleTime() <= 0) return;
            float remaining = d.tfl$titleTime() - c.hudPartial;
            float alpha = 1;
            int total = d.tfl$fadeIn() + d.tfl$duration() + d.tfl$fadeOut();
            if (remaining > d.tfl$duration() + d.tfl$fadeOut())
              alpha = d.tfl$fadeIn() <= 0 ? 1 : (total - remaining) / d.tfl$fadeIn();
            else if (remaining < d.tfl$fadeOut())
              alpha = d.tfl$fadeOut() <= 0 ? 1 : remaining / d.tfl$fadeOut();
            int a = preview() ? 255 : Math.max(0, Math.min(255, (int) (alpha * 255)));
            if (a <= 8) return;
            canvas.fill(
                0,
                0,
                width(),
                height(),
                (module().backgroundArgb() & 0xFFFFFF)
                    | ((module().backgroundArgb() >>> 24) * a / 255 << 24));
            int color = (module().textColor.argb() & 0xFFFFFF) | (a << 24);
            canvas.push();
            canvas.translate((width() - canvas.textWidth(title()) * 2) / 2f, 3);
            canvas.scale(2);
            canvas.text(title(), 0, 0, color, module().shadow());
            canvas.pop();
            canvas.text(
                sub(), (width() - canvas.textWidth(sub())) / 2, 28, color, module().shadow());
          }
        });
  }

  private boolean preview() {
    return c.mc.world == null || c.mc.screen instanceof CoreScreen cs && cs.ui instanceof HudEditor;
  }

  public void tick() {
    scores.clear();
    scoreWidth = 1;
    if (!c.capabilities.active(c.modules.qol.scoreboard)
        || c.mc.world == null
        || c.mc.player == null) return;
    var board = c.mc.world.getScoreboard();
    ScoreboardObjective objective = null;
    var team = board.getTeamOfMember(c.mc.player.getName());
    if (team != null && team.getColor().getId() >= 0)
      objective = board.getDisplayObjective(3 + team.getColor().getId());
    if (objective == null) objective = board.getDisplayObjective(1);
    if (objective == null) return;
    if (c.modules.qol.scoreboardTitle.get()) scores.add(objective.getDisplayName());
    var entries = new ArrayList<>(board.getScores(objective));
    entries.removeIf(e -> e.getOwner() == null || e.getOwner().startsWith("#"));
    entries.sort(ScoreboardScore.COMPARATOR.reversed());
    for (int i = 0; i < Math.min(15, entries.size()); i++) {
      var entry = entries.get(i);
      scores.add(
          Team.getMemberDisplayName(board.getTeamOfMember(entry.getOwner()), entry.getOwner())
              + (c.modules.qol.scoreboardHideNumbers.get() ? "" : "  §c" + entry.get()));
    }
    for (String line : scores) scoreWidth = Math.max(scoreWidth, c.canvas.textWidth(line) + 10);
  }
}
