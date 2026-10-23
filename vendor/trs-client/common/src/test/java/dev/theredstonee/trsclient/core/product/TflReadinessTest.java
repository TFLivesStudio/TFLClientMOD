package dev.theredstonee.trsclient.core.product;

import com.google.gson.*;
import dev.theredstonee.trsclient.core.config.*;
import dev.theredstonee.trsclient.core.module.TrsModules;
import dev.theredstonee.trsclient.core.online.*;
import dev.theredstonee.trsclient.core.ui.*;
import dev.theredstonee.trsclient.core.ui.background.MenuBackground;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

class TflReadinessTest {
    @TempDir Path dir;
    Path write(String name,String json) throws IOException {
        Path p=dir.resolve(name);Files.createDirectories(p.getParent());Files.write(p,json.getBytes(StandardCharsets.UTF_8));return p;
    }
    @Test void productionTransportsFailBeforeDnsOrCredentialsAreUsed() {
        String previous=System.getProperty("tflcore.test.loopback");System.clearProperty("tflcore.test.loopback");
        try {
            assertFalse(Product.onlineServices());
            for(String url:new String[]{"https://trs-launcher.theredstonee.de/v1/me","http://127.0.0.1:1/","https://api.minecraftservices.com/"}) {
                assertThrows(IOException.class,()->new Http.UrlConnection("test").send(new Http.Request("GET",url)));
                assertThrows(IOException.class,()->new Http.UrlConnection("test").send(new Http.Request("PATCH",url)));
            }
        } finally {if(previous!=null)System.setProperty("tflcore.test.loopback",previous);}
    }
    @Test void testTransportExceptionCannotReachRemoteOrDnsAlias() {
        assertThrows(IOException.class,()->Product.requireServiceEndpoint("https://example.com"));
        assertThrows(IOException.class,()->Product.requireServiceEndpoint("http://127.0.0.1.example.com"));
        assertThrows(IOException.class,()->Product.requireServiceEndpoint("http://localhost"));
    }
    @Test void onlineIsOffAndHiddenOnFreshInstall() {
        TrsModules m=new TrsModules();assertFalse(m.trsOnline.isEnabled());assertFalse(m.trsOnline.available());
        assertTrue(m.waypoints.isEnabled());assertNotNull(m.minimap);assertNotNull(m.capePhysics);
    }
    @Test void futureConfigurationIsReadOnlyAndUnchanged() throws Exception {
        Path f=write("client-test.json","{\"configVersion\":99,\"future\":123}");byte[] bytes=Files.readAllBytes(f);
        ConfigStore s=new ConfigStore(f);TrsModules m=new TrsModules();
        assertEquals(ConfigStore.Status.READ_ONLY,s.load(m.registry));assertTrue(s.isReadOnly());
        assertThrows(IOException.class,()->s.save(m.registry));assertArrayEquals(bytes,Files.readAllBytes(f));
    }
    @Test void importPreservesOriginalBackupAndUnknownFieldsAcrossSave() throws Exception {
        Path source=write("tflcore.json","{\"schemaVersion\":2,\"unknown\":42,\"hud\":{\"widgets\":{\"fps\":{\"enabled\":false,\"anchor\":\"TOP_RIGHT\",\"scale\":1.5}}}}");
        byte[] before=Files.readAllBytes(source);Path target=dir.resolve(Product.CONFIG);
        ConfigStore store=new ConfigStore(target);TrsModules m=new TrsModules();
        assertEquals(ConfigStore.Status.LOADED,store.load(m.registry));assertFalse(m.fps.isEnabled());assertEquals(1.5,m.fps.scale.get());
        store.save(m.registry);assertArrayEquals(before,Files.readAllBytes(source));
        JsonObject saved=TflMigration.read(target);assertEquals(42,saved.getAsJsonObject("tflLegacyV2").get("unknown").getAsInt());
        try(java.util.stream.Stream<Path> paths=Files.list(dir)){assertEquals(1,paths.filter(p->p.toString().endsWith(".bak")).count());}
        byte[] after=Files.readAllBytes(target);TflMigration.prepare(target);assertArrayEquals(after,Files.readAllBytes(target));
    }
    @Test void serverWaypointsConvertWithoutAttachingToAnotherWorld() throws Exception {
        write("tflcore.json","{\"schemaVersion\":2,\"survival\":{\"waypoints\":[{\"world\":\"server:Example.COM\",\"dimension\":\"minecraft:overworld\",\"x\":-1.2,\"y\":70,\"z\":8,\"enabled\":true}]}}");
        TflMigration.prepare(dir.resolve(Product.CONFIG));JsonObject w=TflMigration.read(dir.resolve(Product.WAYPOINTS));
        assertEquals(-2,w.getAsJsonArray("mp:example.com").get(0).getAsJsonObject().get("x").getAsInt());
    }
    @Test void migrationRefusesFutureSourceAndDoesNotCreateDestination() throws Exception {
        Path source=write("tflcore.json","{\"schemaVersion\":3}");byte[] before=Files.readAllBytes(source);
        ConfigStore s=new ConfigStore(dir.resolve(Product.CONFIG));assertEquals(ConfigStore.Status.READ_ONLY,s.load(new TrsModules().registry));
        assertFalse(Files.exists(dir.resolve(Product.CONFIG)));assertArrayEquals(before,Files.readAllBytes(source));
    }
    @Test void corruptMigrationCannotOverwriteExistingWaypointDestination() throws Exception {
        write("tflcore.json","{\"schemaVersion\":2,\"survival\":{\"waypoints\":[{\"world\":\"sp:world\",\"x\":1,\"y\":2,\"z\":3}]}}");
        Path marker=write(Product.WAYPOINTS,"{\"sp:keep\":[]}");byte[] before=Files.readAllBytes(marker);
        assertThrows(IOException.class,()->TflMigration.prepare(dir.resolve(Product.CONFIG)));
        assertArrayEquals(before,Files.readAllBytes(marker));assertFalse(Files.exists(dir.resolve(Product.CONFIG)));
    }
    @Test void importExportRoundtripAndTraversalAreChecked() throws Exception {
        ConfigStore s=new ConfigStore(dir.resolve("client.json"));TrsModules m=new TrsModules();s.load(m.registry);
        m.fps.setEnabled(false);s.exportTo("snapshot.json",m.registry);m.fps.setEnabled(true);s.importFrom("snapshot.json",m.registry);assertFalse(m.fps.isEnabled());
        assertThrows(IOException.class,()->s.exportTo("../escape.json",m.registry));
        assertThrows(IOException.class,()->s.importFrom("client.json",m.registry));
        write("future.json","{\"configVersion\":999}");assertThrows(IOException.class,()->s.importFrom("future.json",m.registry));assertFalse(m.fps.isEnabled());
        write("invalid.json","{\"modules\":[]}");assertThrows(IOException.class,()->s.importFrom("invalid.json",m.registry));assertFalse(m.fps.isEnabled());
    }
    @Test void unknownKeysSurviveRegularSave() throws Exception {
        Path f=write("client.json","{\"configVersion\":2,\"custom\":{\"value\":19},\"modules\":{\"futureMod\":{\"flag\":true}}}");
        ConfigStore s=new ConfigStore(f);TrsModules m=new TrsModules();s.load(m.registry);s.save(m.registry);
        assertEquals(19,TflMigration.read(f).getAsJsonObject("custom").get("value").getAsInt());assertTrue(TflMigration.read(f).getAsJsonObject("modules").has("futureMod"));
    }
    @Test void corruptBackupsAreNeverReplaced() throws Exception {
        Path f=write("client.json","broken");write("client.json.broken","older backup");
        ConfigStore s=new ConfigStore(f);s.load(new TrsModules().registry);
        assertEquals("older backup",new String(Files.readAllBytes(dir.resolve("client.json.broken")),StandardCharsets.UTF_8));
    }
    @Test void decoderFailureIsClosedAndStaticModeNeverStartsDecoder() {
        MenuBackground bg=new MenuBackground();final int[] calls={0,0};
        bg.install(new MenuBackground.Player(){public void start(MenuBackground.Mode m){calls[0]++;throw new IllegalStateException("decoder");}public TextureRef latest(){return null;}public void close(){calls[1]++;}});
        bg.draw(new NoopCanvas(),320,240,true);assertEquals(0,calls[0]);
        bg.setMode(MenuBackground.Mode.HIGH);assertDoesNotThrow(()->bg.draw(new NoopCanvas(),320,240,true));assertFalse(bg.videoActive());
        bg.draw(new NoopCanvas(),320,240,true);assertEquals(1,calls[0]);assertEquals(1,calls[1]);
    }
    @Test void leavingTitleClosesDecoderExactlyOnce() {
        MenuBackground bg=new MenuBackground();final int[] calls={0};bg.install(new MenuBackground.Player(){public void start(MenuBackground.Mode m){}public TextureRef latest(){return null;}public void close(){calls[0]++;}});
        bg.setMode(MenuBackground.Mode.BALANCED);bg.draw(new NoopCanvas(),320,240,true);assertTrue(bg.videoActive());bg.close();bg.close();assertEquals(1,calls[0]);
    }
    @Test void knownNullFieldsCanBeResetWithoutLosingUnknownData() throws Exception {
        Path f=write("client.json","{\"configVersion\":2,\"custom\":1,\"clientState\":{\"lookTheme\":\"light\"}}");
        ConfigStore s=new ConfigStore(f);TrsModules m=new TrsModules();s.load(m.registry);
        // A new registry captures an explicit reset of all known optional fields.
        s.save(new TrsModules().registry);
        JsonObject stored=TflMigration.read(f);assertEquals(1,stored.get("custom").getAsInt());
        assertTrue(stored.get("clientState").isJsonNull() || stored.getAsJsonObject("clientState").get("lookTheme").isJsonNull());
    }
    @Test void failingDecoderCloseCannotBreakMenuFallback() {
        MenuBackground bg=new MenuBackground();bg.install(new MenuBackground.Player(){public void start(MenuBackground.Mode m){throw new IllegalStateException();}public TextureRef latest(){return null;}public void close(){throw new LinkageError();}});
        bg.setMode(MenuBackground.Mode.HIGH);assertDoesNotThrow(()->bg.draw(new NoopCanvas(),320,240,true));assertFalse(bg.videoActive());
    }
    @Test void pauseMenuContainsCoreAndLocalUtilitiesOnly() {
        assertArrayEquals(new String[]{"core","clips","serverInfo"},Product.pauseEntries());
        for(String entry:Product.pauseEntries())assertFalse(entry.equals("accounts")||entry.equals("friends")||entry.equals("wardrobe"));
    }
    @Test void resetBacksUpAndRestoresDefaultModules() throws Exception {
        Path f=dir.resolve("client.json");ConfigStore s=new ConfigStore(f);TrsModules m=new TrsModules();s.load(m.registry);
        m.fps.setEnabled(false);s.save(m.registry);byte[] before=Files.readAllBytes(f);s.resetToDefaults(m.registry);assertTrue(m.fps.isEnabled());
        try(java.util.stream.Stream<Path> paths=Files.list(dir)) {
            Path backup=paths.filter(p->p.getFileName().toString().contains(".pre-reset-")).findFirst().get();assertArrayEquals(before,Files.readAllBytes(backup));
        }
    }
    @Test void installedClientDoesNotLinkToMissingDevelopmentHarness() {
        String previous=System.getProperty("trsclient.autotest");System.clearProperty("trsclient.autotest");
        try {
            assertDoesNotThrow(DevelopmentHooks::install);
            System.setProperty("trsclient.autotest","true");
            assertThrows(IllegalStateException.class,DevelopmentHooks::install);
        } finally {if(previous==null)System.clearProperty("trsclient.autotest");else System.setProperty("trsclient.autotest",previous);}
    }
    static class NoopCanvas implements Canvas {
        public void fill(int a,int b,int c,int d,int color){} public void text(String s,int x,int y,int c,boolean shadow){}
        public int textWidth(String s){return s.length()*5;}public int lineHeight(){return 9;}public String clip(String s,int w){return s.substring(0,Math.min(s.length(),Math.max(0,w/5)));}
        public void flush(){}public void scissor(int a,int b,int c,int d){}public void noScissor(){}public void raise(float z){}public void push(){}public void translate(float x,float y){}public void scale(float s){}public void pop(){}
    }
}
