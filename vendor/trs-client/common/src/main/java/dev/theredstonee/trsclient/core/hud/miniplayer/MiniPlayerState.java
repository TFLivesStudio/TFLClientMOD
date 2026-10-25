package dev.theredstonee.trsclient.core.hud.miniplayer;

/** Renderer-independent cache/update policy. It never changes an entity or a world camera. */
public final class MiniPlayerState {
    private Object world,player;private long lastKey=Long.MIN_VALUE,lastUpdate,serial;private float yaw;
    public long updates,nanos;public String failure="";
    public boolean update(Object world,Object player,long key,boolean animating,long now,MiniPlayerModule settings){
        boolean changed=this.world!=world||this.player!=player||lastKey!=key;
        long period=settings.fps.getInt()<=0?0:1_000_000_000L/settings.fps.getInt();
        if(changed||(animating||Math.abs(wrap(settings.cameraYaw()-yaw))>.05)&&now-lastUpdate>=period){
            double dt=lastUpdate==0?1:Math.min(.2,(now-lastUpdate)/1e9);float target=settings.cameraYaw(),delta=wrap(target-yaw);
            yaw=settings.smoothing.get()?(float)(yaw+delta*(1-Math.exp(-dt*16))):target;
            this.world=world;this.player=player;lastKey=key;lastUpdate=now;serial++;updates++;return true;
        }
        return false;
    }
    private static float wrap(float f){f%=360;return f>180?f-360:f< -180?f+360:f;}
    public float yaw(){return yaw;}public long serial(){return serial;}
    public void invalidate(){world=player=null;lastKey=Long.MIN_VALUE;lastUpdate=0;failure="";}
    public static long optionsKey(MiniPlayerModule m){
        long h=m.angle.index();h=31*h+Double.doubleToLongBits(m.rotation.get());h=31*h+(m.armor.get()?1:0);h=31*h+(m.heldItems.get()?1:0);h=31*h+(m.cape.get()?1:0);return h;
    }
}
