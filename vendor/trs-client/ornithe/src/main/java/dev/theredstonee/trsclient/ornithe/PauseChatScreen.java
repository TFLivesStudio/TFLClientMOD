package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.ui.menus.*;
import net.minecraft.client.gui.screen.game.ChatScreen;

/** Uses the game's input, history, completion and clickable chat implementation. */
public final class PauseChatScreen extends ChatScreen implements PauseOverlay {
  public final PauseActionsUi actions;
  private boolean chatLayout;
  public boolean drawingChat;

  @Override
  public boolean pauseChatLayout() {
    return chatLayout;
  }

  @Override
  public void init() {
    chatLayout = true;
    super.init();
    String text = chatField.getText();
    chatField =
        new net.minecraft.client.gui.widget.TextFieldWidget(
            0,
            minecraft.textRenderer,
            PauseLayout.chatLeft(width) + 2,
            height - 12,
            PauseLayout.chatWidth(width) - 4,
            12);
    chatField.setMaxLength(100);
    chatField.setHasBorder(false);
    chatField.setFocused(true);
    chatField.setText(text);
    minecraft.gui.getChat().reset();
  }

  @Override
  public void removed() {
    chatLayout = false;
    minecraft.gui.getChat().reset();
    super.removed();
  }

  public PauseChatScreen(PauseActionsUi actions) {
    super("");
    this.actions = actions;
  }

  @Override
  public void render(int x, int y, float delta) {
    net.minecraft.client.gui.GuiElement.fill(0, 0, width, height, PauseLayout.SCRIM);
    net.minecraft.client.render.platform.GlStateManager.pushMatrix();
    try {
      net.minecraft.client.render.platform.GlStateManager.translatef(0, height - 48, 0);
      drawingChat = true;
      minecraft.gui.getChat().render(minecraft.gui.getTicks());
    } finally {
      drawingChat = false;
      net.minecraft.client.render.platform.GlStateManager.popMatrix();
    }
    super.render(x, y, delta);
    var c = OrnitheClient.get();
    c.canvas.begin(width, height, c.window().getScale());
    try {
      actions.render(c.canvas, width, height, x, y);
    } finally {
      c.canvas.end();
    }
  }

  @Override
  protected void mouseClicked(int x, int y, int button) {
    if (actions.inputClick(x, y, button)) return;
    super.mouseClicked(x, y, button);
  }

  @Override
  protected void keyPressed(char ch, int key) {
    if (key == 15 && (!chatField.isFocused() || isControlDown())) {
      chatField.setFocused(!chatField.isFocused());
      return;
    }
    if (!chatField.isFocused() && key != 1) {
      actions.inputKey(key, CoreScreen.logical(key), isShiftDown());
      return;
    }
    super.keyPressed(ch, key);
    if ((key == 28 || key == 156) && minecraft.screen == null) minecraft.openScreen(this);
  }

  @Override
  public boolean shouldPauseGame() {
    return false;
  }
}
