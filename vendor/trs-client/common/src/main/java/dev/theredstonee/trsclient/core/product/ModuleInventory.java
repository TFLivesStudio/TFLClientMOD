package dev.theredstonee.trsclient.core.product;

import com.google.gson.*;
import dev.theredstonee.trsclient.core.module.TrsModules;
import dev.theredstonee.trsclient.core.module.Module;
import dev.theredstonee.trsclient.core.module.Setting;

/** Build-time inventory of the real registry, not a list inferred from source filenames. */
public final class ModuleInventory {
    private ModuleInventory() {}
    public static void main(String[] args) throws java.io.IOException {
        if(args.length!=1)throw new IllegalArgumentException("Output JSON path required");
        TrsModules modules=new TrsModules();JsonArray rows=new JsonArray();
        for(Module m:modules.registry.all()) {
            JsonObject row=new JsonObject();row.addProperty("id",m.id());row.addProperty("name",m.name());
            row.addProperty("category",m.category().name());row.addProperty("hud",m.isHud());row.addProperty("defaultEnabled",m.isEnabled());
            String state="registered; offline logic covered where tests exist; in-game verification pending";
            if(m.id().equals("trsOnline")||m.id().equals("social")||m.id().equals("emotes"))state="TRS backend dependent; service integration disabled";
            if(m.id().equals("clips"))state="TRS launcher IPC dependent; recording integration disabled; local screenshots retained";
            row.addProperty("classification",state);JsonArray settings=new JsonArray();for(Setting setting:m.settings())settings.add(new JsonPrimitive(setting.key()));row.add("settings",settings);rows.add(row);
        }
        JsonObject root=new JsonObject();root.addProperty("schemaVersion",1);root.addProperty("implementation","TRS common registry with TFL modifications");root.add("modules",rows);
        java.nio.file.Files.write(java.nio.file.Paths.get(args[0]),new GsonBuilder().setPrettyPrinting().create().toJson(root).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
