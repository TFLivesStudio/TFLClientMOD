package dev.theredstonee.trsclient.core.hud.miniplayer;
/** Thread-local draw-only layer policy. Equipment and inventory are never modified to hide a layer. */
public final class MiniPlayerRenderScope {
    private static final ThreadLocal<MiniPlayerModule> ACTIVE=new ThreadLocal<MiniPlayerModule>();
    private MiniPlayerRenderScope(){}
    public static MiniPlayerModule current(){return ACTIVE.get();}
    public static MiniPlayerModule enter(MiniPlayerModule m){MiniPlayerModule old=ACTIVE.get();ACTIVE.set(m);return old;}
    public static void restore(MiniPlayerModule m){if(m==null)ACTIVE.remove();else ACTIVE.set(m);}
}
