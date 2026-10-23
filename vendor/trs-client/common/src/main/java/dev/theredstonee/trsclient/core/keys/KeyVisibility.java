package dev.theredstonee.trsclient.core.keys;

import dev.theredstonee.trsclient.core.product.Product;
import java.util.ArrayList;
import java.util.List;

/** Presentation policy only: registered mappings and their saved assignments remain untouched. */
public final class KeyVisibility {
    private KeyVisibility() {}

    public static boolean visible(String nameKey) {
        if (Product.onlineServices()) return true;
        return !"key.trsclient.wardrobe".equals(nameKey)
                && !"key.trsclient.social".equals(nameKey)
                && !"key.trsclient.quickReply".equals(nameKey)
                && !"key.trsclient.saveClip".equals(nameKey)
                && !"key.trsclient.toggleRecording".equals(nameKey);
    }

    /** Apply before search/conflict detection so unavailable actions cannot create ghost search results. */
    public static List<KeySearch.Entry> visibleEntries(List<KeySearch.Entry> entries) {
        List<KeySearch.Entry> out = new ArrayList<KeySearch.Entry>();
        for (KeySearch.Entry entry : entries) if (visible(entry.nameKey)) out.add(entry);
        return out;
    }
}
