package dev.theredstonee.trsclient.core.ui;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class TextInputSelectionTest {
  @Test void selectAllReplacesAndDeletesText() {
    TextInput input = new TextInput(8);
    input.setText("antes");
    assertTrue(input.key(UiKey.SELECT_ALL));
    assertTrue(input.type('ñ'));
    assertEquals("ñ", input.text());
    input.key(UiKey.SELECT_ALL);
    input.key(UiKey.BACKSPACE);
    assertEquals("", input.text());
  }
}
