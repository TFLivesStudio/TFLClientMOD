package dev.theredstonee.trsclient.ornithe;

import dev.diorama.Projection;
import dev.diorama.bake.*;
import dev.diorama.data.ChunkSnapshot;
import dev.theredstonee.trsclient.core.ui.*;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;
import java.io.*;

/** Diorama's original voxel ray tracer over immutable received 1.8.9 chunk sections. */
public final class OrnitheDiorama implements ChunkSource, AutoCloseable {
 private final OrnitheClient client;
 private final dev.theredstonee.trsclient.core.map.MobHeads mobHeads=new dev.theredstonee.trsclient.core.map.MobHeads();
 private final ConcurrentHashMap<Long,ChunkSnapshot> chunks=new ConcurrentHashMap<>();
 private final LinkedHashMap<TileKey,Tile> tiles=new LinkedHashMap<>(32,.75f,true);
 private final Set<TileKey> baking=ConcurrentHashMap.newKeySet();
 private final ArrayBlockingQueue<Result> completed=new ArrayBlockingQueue<>(8);
 private final ThreadPoolExecutor worker=new ThreadPoolExecutor(2,2,10,TimeUnit.SECONDS,new ArrayBlockingQueue<>(16),r->{Thread t=new Thread(r,"TFL-Diorama-bake");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
 private final ThreadPoolExecutor disk=new ThreadPoolExecutor(1,1,10,TimeUnit.SECONDS,new ArrayBlockingQueue<>(256),r->{Thread t=new Thread(r,"TFL-Diorama-store");t.setDaemon(true);return t;},new ThreadPoolExecutor.DiscardPolicy());
 private final HashMap<Long,Long> captured=new HashMap<>();
 private final Set<Long> reading=ConcurrentHashMap.newKeySet(),stored=ConcurrentHashMap.newKeySet();
 private final ArrayBlockingQueue<ChunkSnapshot> restored=new ArrayBlockingQueue<>(64);
 private final dev.theredstonee.trsclient.core.map.MapEntity[] actors=new dev.theredstonee.trsclient.core.map.MapEntity[128];private int actorCount;
 private dev.theredstonee.trsclient.core.map.volume.CloudQuality quality;
 private BlockLooks looks; private Object world;private String identity="";private volatile Path folder;
 private final int[] knownPrefix=new int[257*257],unknownPrefix=new int[257*257];
 private DioramaFog fog;private long maskAt;private double oldMaskU,oldMaskV,oldExtent;private long maskRevision;
 private volatile long revision,epoch;private long lastCapture;private boolean closed;private TextureRef clouds;
 public long bakedTiles,bakeNanos;public volatile String failure="";
 private record Result(TileKey key,int[] pixels,long revision,long epoch){}
 private static final class Tile {TextureRef texture;long revision,lastRequest;byte[] alpha;}
 public OrnitheDiorama(OrnitheClient client){this.client=client;client.map.palette().setResources((ns,path)->client.mapPlatform.readResource(ns,path));for(int i=0;i<actors.length;i++)actors[i]=new dev.theredstonee.trsclient.core.map.MapEntity();}
 public ChunkSnapshot chunk(int x,int z){long key=ChunkSnapshot.key(x,z);ChunkSnapshot result=chunks.get(key);
  Path home=folder;long session=epoch;
  if(result==null&&home!=null&&stored.contains(key)&&reading.size()<4096&&disk.getQueue().remainingCapacity()>16&&reading.add(key))disk.execute(()->{ChunkSnapshot saved=load(home.resolve(x+"_"+z+".bin.gz"));if(session!=epoch){reading.remove(key);return;}if(saved!=null&&!restored.offer(saved))reading.remove(key);else if(saved==null&&session==epoch)stored.remove(key);});
  return result;
 }
 public int minY(){return 0;}public int maxY(){return 256;}public int topY(){return 256;}
 public int roof(){return client.mapPlatform.dimension().equals("dim-1")?128:NO_ROOF;}
 public boolean isSeen(int x,int y,int z){return chunks.containsKey(ChunkSnapshot.key(x>>4,z>>4));}
 public int snapshotCount(){return chunks.size();}
 public int autoLayer(){if(client.modules.minimapCaveMode.get()==dev.theredstonee.trsclient.core.module.TrsModules.CaveMode.OFF||!client.map.fairPlay().caveAllowed(roof()!=NO_ROOF))return TileKey.SURFACE;return client.mapPlatform.skyLight()<4&&roof()==NO_ROOF?Math.min(255,(int)client.mapPlatform.y()+16):TileKey.SURFACE;}
 public void tick(){
  if(closed)return;
  String next=client.mapPlatform.worldKey()+"/"+client.mapPlatform.dimension();
  if(world!=client.mc.world||!next.equals(identity)){
   clear();world=client.mc.world;identity=next;
   folder=client.mc.gameDir.toPath().resolve("tfl-maps").resolve(UUID.nameUUIDFromBytes(next.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString());
   if(world!=null){Path home=folder;long session=epoch;disk.execute(()->{if(Files.isDirectory(home))try(var files=Files.newDirectoryStream(home,"*.bin.gz")){for(Path file:files){if(session!=epoch)break;String[] parts=file.getFileName().toString().replace(".bin.gz","").split("_");if(parts.length==2)try{stored.add(ChunkSnapshot.key(Integer.parseInt(parts[0]),Integer.parseInt(parts[1])));}catch(NumberFormatException ignored){}}}catch(IOException ignored){}});looks=buildLooks();client.map.fairPlay().resetServer();client.map.fairPlay().onServerText(client.mapPlatform.serverMotd());}
  }
  if(world==null||client.mc.player==null)return;
  var requestedQuality=client.modules.worldMapCloudQuality.get();if(quality!=requestedQuality){quality=requestedQuality;TileBaker.setFast(quality==dev.theredstonee.trsclient.core.map.volume.CloudQuality.LOW);reload();}
  client.map.fairPlay().setManual(client.modules.minimapFairPlay.get());
  actorCount=0;client.mapPlatform.entities(()->actorCount>=actors.length?null:actors[actorCount++],128,!client.map.fairPlay().radarThroughWalls());
  for(int i=0;i<4;i++){ChunkSnapshot s=restored.poll();if(s==null)break;chunks.put(ChunkSnapshot.key(s.chunkX,s.chunkZ),s);revision++;}
  if(disk.getQueue().remainingCapacity()<8)return;
  if(System.nanoTime()-lastCapture<20_000_000L)return;lastCapture=System.nanoTime();
  int px=(int)Math.floor(client.mc.player.x)>>4,pz=(int)Math.floor(client.mc.player.z)>>4;
  int radius=Math.min(12,client.mc.options.viewDistance);
  long now=System.currentTimeMillis();
  for(int ring=0;ring<=radius;ring++)for(int dx=-ring;dx<=ring;dx++)for(int dz=-ring;dz<=ring;dz++){
   if(Math.abs(dx)!=ring&&Math.abs(dz)!=ring)continue;
   int x=px+dx,z=pz+dz;long key=ChunkSnapshot.key(x,z);
   if(now-captured.getOrDefault(key,0L)<15000)continue;
   var received=client.mc.world.getChunkSource().getChunk(x,z);
   if(received instanceof net.minecraft.world.chunk.EmptyChunk)continue;
   ChunkSnapshot snapshot=capture(received,x,z);
   chunks.put(key,snapshot);captured.put(key,now);revision++;
   if(chunks.size()>384){Long oldest=chunks.keySet().stream().filter(k->Math.abs((int)(k>>32)-px)>radius+2||Math.abs((int)(long)k-pz)>radius+2).findFirst().orElse(null);if(oldest!=null){chunks.remove(oldest);reading.remove(oldest);}}
   Path destination=folder.resolve(x+"_"+z+".bin.gz");
   disk.execute(()->save(destination,snapshot));return;
  }
 }
 private BlockLooks buildLooks(){
  BlockLooks l=new BlockLooks();
  LegacyBlockGeometry geometry=new LegacyBlockGeometry();var shapes=new ArrayList<float[]>();shapes.add(new float[]{0,0,0,1,1,1});var shapeIds=new HashMap<String,Integer>();
  for(Block block:Block.REGISTRY)for(int meta=0;meta<16;meta++)try{
   var state=block.getStateFromMetadata(meta);int id=Block.STATE_REGISTRY.getId(state);if(id<0||id>=65536)continue;
   Material material=block.getMaterial();
   l.kind[id]=material==Material.AIR?BlockLooks.AIR:material==Material.WATER?BlockLooks.WATER:material==Material.LAVA?BlockLooks.LAVA:material==Material.GLASS?BlockLooks.GLASS:material.isSolid()?BlockLooks.SOLID:BlockLooks.PLANT;
   if(l.kind[id]==BlockLooks.SOLID||l.kind[id]==BlockLooks.GLASS)try{float[] boxes=geometry.boxes(state);if(!Arrays.equals(boxes,shapes.get(0))){String key=Arrays.toString(boxes);Integer shape=shapeIds.get(key);if(shape==null){shape=shapes.size();shapeIds.put(key,shape);shapes.add(boxes);}l.shape[id]=shape;}}catch(RuntimeException ignored){}
   int rgb=block.getMapColor(state).color;
   try {var sprite=client.mc.getBlockRenderDispatcher().getModelShaper().getParticleIcon(state);if(sprite.getFrameCount()>0)rgb=dev.theredstonee.trsclient.core.map.TexturePalette.average(sprite.getFrame(0)[0],sprite.getWidth(),sprite.getHeight());}catch(RuntimeException ignored){}
   l.top[id]=l.side[id]=rgb;l.emission[id]=(byte)block.getLight();l.spawnOn[id]=block.isOpaque();l.spawnIn[id]=material==Material.AIR;
  }catch(RuntimeException ignored){}
  l.shapes=shapes.toArray(new float[0][]);return l;
 }
 private ChunkSnapshot capture(WorldChunk chunk,int x,int z){
  int[][] states=new int[16][];byte[][] block=new byte[16][],sky=new byte[16][];
  var sections=chunk.getSections();
  for(int i=0;i<Math.min(16,sections.length);i++){
   var section=sections[i];if(section==null)continue;
   char[] source=section.getBlockStates();states[i]=new int[4096];block[i]=new byte[4096];sky[i]=new byte[4096];
   for(int j=0;j<4096;j++){states[i][j]=source[j];block[i][j]=(byte)section.getBlockLight(j&15,j>>8,j>>4&15);sky[i][j]=(byte)(client.mapPlatform.dimension().equals("dim-1")?0:section.getSkyLight(j&15,j>>8,j>>4&15));}
  }
  int[] grass=new int[256],foliage=new int[256],water=new int[256];Arrays.fill(water,0x3F76E4);
  int top=0,floor=256;
  for(int dz=0;dz<16;dz++)for(int dx=0;dx<16;dx++){
   int y=chunk.getHeight(dx,dz);top=Math.max(top,y);floor=Math.min(floor,y);
   var pos=new BlockPos((x<<4)+dx,y,(z<<4)+dz);
   int color=client.mc.world.getBiome(pos).getGrassColor(pos);grass[dz*16+dx]=color;foliage[dz*16+dx]=color;
  }
  return new ChunkSnapshot(x,z,states,block,sky,top,floor,new ChunkSnapshot.Tints(grass,foliage,foliage,water));
 }
 public net.minecraft.util.math.BlockPos pick(double u,double v,int layer){return looks==null?null:MapPick.standingSpot(this,looks,u,v,layer,false);}
 public void draw(Canvas c,int x,int y,int width,int height,double centerX,double centerY,double centerZ,double pixelsPerBlock){draw(c,x,y,width,height,centerX,centerY,centerZ,pixelsPerBlock,TileKey.SURFACE);}
 public void changed(int x,int z){captured.remove(ChunkSnapshot.key(x,z));}
 public void reload(){epoch++;reading.clear();restored.clear();if(world!=null)looks=buildLooks();for(Tile tile:tiles.values())if(tile.texture!=null)client.canvas.release(tile.texture);tiles.clear();completed.clear();if(fog!=null)fog.close();fog=null;maskAt=0;}
 public boolean fogHealthy(){return fog!=null&&fog.healthy();}
 public void draw(Canvas c,int x,int y,int width,int height,double centerX,double centerY,double centerZ,double pixelsPerBlock,int layer){
  upload();ensureClouds();
  c.scissor(x,y,x+width,y+height);
  c.fill(x,y,x+width,y+height,0xFF718498);
  if(clouds!=null&&quality!=dev.theredstonee.trsclient.core.map.volume.CloudQuality.OFF){float time=client.modules.worldMapCloudMotion.get()?(System.nanoTime()%300_000_000_000L)/1e9f:0;for(int band=0;band<3;band++){
   int size=320+band*100,offset=(int)(time*(2+band))%size;
   for(int cy=y-size;cy<y+height;cy+=size)for(int cx=x-size;cx<x+width+size;cx+=size)Affine.image(c,clouds,cx-offset,cy+(band==1?offset:0),size,size,0,0,256,256,band==0?0xFFC8DAEB:0x75ECF4FC);
  }}
  if(looks!=null){double v=Projection.v(centerY,centerZ);int lod=Math.max(0,Math.min(TileKey.MAX_LOD,(int)Math.floor(Math.log(.5/pixelsPerBlock)/Math.log(2))));double units=Projection.TILE_UNITS*(1<<lod);
   double left=centerX-width/(2*pixelsPerBlock),top=v-height/(2*pixelsPerBlock);
   int minU=(int)Math.floor(left/units),maxU=(int)Math.floor((left+width/pixelsPerBlock)/units);
   int minV=(int)Math.floor(top/units),maxV=(int)Math.floor((top+height/pixelsPerBlock)/units);
   int requested=0;
   for(int tv=minV;tv<=maxV;tv++)for(int tu=minU;tu<=maxU;tu++){
    TileKey key=new TileKey(layer,tu,tv,lod);Tile tile=tiles.get(key);
    if(tile!=null&&tile.texture!=null){float px=(float)(x+(key.minU()-left)*pixelsPerBlock),py=(float)(y+(key.minV()-top)*pixelsPerBlock);Affine.image(c,tile.texture,px,py,(float)(units*pixelsPerBlock+.2),(float)(units*pixelsPerBlock+.2),0,0,Projection.TILE_PIXELS,Projection.TILE_PIXELS,0xFFFFFFFF);}
    if(requested<16&&(tile==null||tile.revision<revision&&System.nanoTime()-tile.lastRequest>1_000_000_000L)&&baking.size()<8){if(tile!=null)tile.lastRequest=System.nanoTime();request(key);requested++;}
   }
  }
  // The same eight-layer height-field shader as modern Diorama, at the UI frame rate.
  if(fog==null)fog=new DioramaFog(client);
  int maskLod=Math.max(0,Math.min(TileKey.MAX_LOD,(int)Math.floor(Math.log(.5/pixelsPerBlock)/Math.log(2))));double tileUnits=128*(1<<maskLod);
  double visible=Math.max(width,height)/pixelsPerBlock;
  int code=Math.max(0,Math.min(8,(int)Math.ceil(Math.log(visible*1.3/512)/Math.log(2))));
  double extent=512*(1<<code),texel=extent/256;
  double maskU=Math.floor((centerX-extent/2)/texel)*texel,maskV=Math.floor((Projection.v(centerY,centerZ)-extent/2)/texel)*texel;
  int[] mask=null;long now=System.nanoTime();
  if(now-maskAt>100_000_000L||maskU!=oldMaskU||maskV!=oldMaskV||extent!=oldExtent||maskRevision!=bakedTiles){
   mask=new int[256*256];
   for(int my=0;my<256;my++)for(int mx=0;mx<256;mx++){
    double u=maskU+(mx+.5)*texel,v=maskV+(my+.5)*texel;Tile tile=tiles.get(new TileKey(layer,(int)Math.floor(u/tileUnits),(int)Math.floor(v/tileUnits),maskLod));
    int known=0;if(tile!=null&&tile.alpha!=null){int tx=Math.floorMod((int)Math.floor(u*4/(1<<maskLod)),512),ty=Math.floorMod((int)Math.floor(v*4/(1<<maskLod)),512);known=tile.alpha[ty*512+tx]&255;}
    mask[my*256+mx]=0xff000000|known<<16;
   }
   flagMask(mask,texel);
   maskAt=now;oldMaskU=maskU;oldMaskV=maskV;oldExtent=extent;maskRevision=bakedTiles;
  }
  double left=centerX-width/(2*pixelsPerBlock),top=Projection.v(centerY,centerZ)-height/(2*pixelsPerBlock);
  if(quality!=dev.theredstonee.trsclient.core.map.volume.CloudQuality.OFF)fog.draw(x,y,width,height,mask,(left-maskU)/extent,(top-maskV)/extent,width/pixelsPerBlock/extent,height/pixelsPerBlock/extent,code,maskU,maskV,layer!=TileKey.SURFACE);
  boolean full=width>client.modules.minimapSize.getInt()+16;
  if(!full&&client.modules.minimapWaypoints.get())for(var point:client.waypoints.visible(client.mapPlatform.worldKey(),client.mapPlatform.dimension())){int px=(int)Math.round(x+width/2+(point.x-centerX)*pixelsPerBlock),py=(int)Math.round(y+height/2+(Projection.v(point.y,point.z)-Projection.v(centerY,centerZ))*pixelsPerBlock);if(px>=x&&py>=y&&px<x+width&&py<y+height)SelectionStyle.rounded(c,px-3,py-3,7,7,3,point.color|0xff000000);}
  var modules=client.modules;
  for(int i=0;i<actorCount;i++){
   var entity=actors[i];boolean player=entity.type==dev.theredstonee.trsclient.core.map.MapEntity.PLAYER;
   if(player&&!(full?modules.worldMapPlayers.get():modules.minimapPlayers.get()))continue;
   if(!player&&entity.type==dev.theredstonee.trsclient.core.map.MapEntity.HOSTILE&&!(full?modules.worldMapHostile.get():modules.minimapHostile.get()))continue;
   if(!player&&entity.type!=dev.theredstonee.trsclient.core.map.MapEntity.HOSTILE&&!(full?modules.worldMapPassive.get():modules.minimapPassive.get()))continue;
   int px=(int)Math.round(x+width/2+(entity.x-centerX)*pixelsPerBlock),py=(int)Math.round(y+height/2+(Projection.v(entity.y,entity.z)-Projection.v(centerY,centerZ))*pixelsPerBlock);
   if(px<x||py<y||px>=x+width||py>=y+height)continue;
   int color=player?0xff7dd3fc:entity.type==dev.theredstonee.trsclient.core.map.MapEntity.HOSTILE?0xfffb7185:0xff86efac;
   SelectionStyle.rounded(c,px-4,py-4,9,9,3,color);
   if(!player&&modules.minimapMobIcons.get()==dev.theredstonee.trsclient.core.module.TrsModules.MobIcons.HEADS){var head=mobHeads.head(entity.kind,client.map.palette(),client.canvas.resourceGeneration());if(head!=null)Affine.image(c,head.texture,px-3,py-3,7,7,head.u,head.v,head.w,head.h,0xffffffff);}
   if(player&&client.mc.getNetworkHandler()!=null){var info=client.mc.getNetworkHandler().getOnlinePlayer(entity.uuid);if(info!=null){TextureRef skin=client.canvas.game(info.getSkinTexture().toString(),64,64);Affine.image(c,skin,px-3,py-3,7,7,8,8,8,8,0xffffffff);}}
  }
  c.noScissor();
 }
 private void flagMask(int[] mask,double texel){
  java.util.Arrays.fill(knownPrefix,0);java.util.Arrays.fill(unknownPrefix,0);
  for(int y=0;y<256;y++){int known=0,unknown=0;for(int x=0;x<256;x++){int red=mask[y*256+x]>>>16&255;known+=red==255?1:0;unknown+=red==0?1:0;knownPrefix[(y+1)*257+x+1]=knownPrefix[y*257+x+1]+known;unknownPrefix[(y+1)*257+x+1]=unknownPrefix[y*257+x+1]+unknown;}}
  int reach=Math.min(256,Math.max(2,(int)Math.ceil(64/texel)));
  for(int y=0;y<256;y++)for(int x=0;x<256;x++){int x0=Math.max(0,x-reach),y0=Math.max(0,y-reach),x1=Math.min(256,x+reach+1),y1=Math.min(256,y+reach+1),count=(x1-x0)*(y1-y0);int a=y1*257+x1,b=y0*257+x1,c=y1*257+x0,d=y0*257+x0;int known=knownPrefix[a]-knownPrefix[b]-knownPrefix[c]+knownPrefix[d],unknown=unknownPrefix[a]-unknownPrefix[b]-unknownPrefix[c]+unknownPrefix[d];if(known==count)mask[y*256+x]|=0x0000ff00;else if(unknown==count)mask[y*256+x]|=0x000000ff;}
 }
 private void ensureClouds(){if(clouds!=null)return;int[] p=new int[256*256];java.util.Random random=new java.util.Random(1);float[] noise=new float[16*16];for(int i=0;i<noise.length;i++)noise[i]=random.nextFloat();
  for(int y=0;y<256;y++)for(int x=0;x<256;x++){double fx=x/16.0,fy=y/16.0;int ix=(int)fx,iy=(int)fy;double tx=fx-ix,ty=fy-iy;tx=tx*tx*(3-2*tx);ty=ty*ty*(3-2*ty);double a=noise[(iy&15)*16+(ix&15)]*(1-tx)+noise[(iy&15)*16+((ix+1)&15)]*tx,b=noise[((iy+1)&15)*16+(ix&15)]*(1-tx)+noise[((iy+1)&15)*16+((ix+1)&15)]*tx;int n=(int)(145+100*(a*(1-ty)+b*ty));p[y*256+x]=0xff000000|n<<16|Math.min(255,n+8)<<8|Math.min(255,n+16);}
  clouds=client.canvas.upload("diorama/clouds",256,256,p);
 }
 private void request(TileKey key){if(!baking.add(key))return;long version=revision,session=epoch;BlockLooks colors=looks;
  try{worker.execute(()->{try{long start=System.nanoTime();TileImage image=TileBaker.bake(key,this,colors,null,false);int[] p=image.mips()[0];for(int i=0;i<p.length;i++){int v=p[i];p[i]=v&0xFF00FF00|(v&255)<<16|(v>>>16&255);}if(session==epoch){completed.offer(new Result(key,p,version,session));bakeNanos+=System.nanoTime()-start;bakedTiles++;}}catch(Throwable e){failure=e.toString();}finally{baking.remove(key);}});}catch(RejectedExecutionException e){baking.remove(key);}
 }
 private void upload(){Result r;while((r=completed.poll())!=null){if(r.epoch!=epoch)continue;Tile tile=tiles.computeIfAbsent(r.key,k->new Tile());tile.texture=client.canvas.upload("diorama/tile-"+r.key.layer()+"-"+r.key.lod()+"-"+r.key.tileU()+"-"+r.key.tileV(),Projection.TILE_PIXELS,Projection.TILE_PIXELS,r.pixels);tile.revision=r.revision;tile.alpha=new byte[r.pixels.length];for(int i=0;i<r.pixels.length;i++)tile.alpha[i]=(byte)(r.pixels[i]>>>24);
  while(tiles.size()>64){var iterator=tiles.entrySet().iterator();Tile old=iterator.next().getValue();iterator.remove();if(old.texture!=null)client.canvas.release(old.texture);}
 }}
 private void save(Path path,ChunkSnapshot s){try{Files.createDirectories(path.getParent());Path temp=path.resolveSibling(path.getFileName()+".tmp");try(var out=new DataOutputStream(new java.util.zip.GZIPOutputStream(Files.newOutputStream(temp)))){out.writeInt(0x54464431);out.writeInt(s.chunkX);out.writeInt(s.chunkZ);out.writeInt(s.maxY);out.writeInt(s.floorY);for(int i=0;i<16;i++){int[] a=s.section(i);out.writeBoolean(a!=null);if(a!=null)for(int j=0;j<4096;j++){out.writeShort(a[j]);out.writeByte(s.blockLight(i,j&15,j>>8,j>>4&15)|(s.skyLight(i,j&15,j>>8,j>>4&15)<<4));}}for(int i:s.tints.grass())out.writeInt(i);}Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING);if(path.getParent().equals(folder))stored.add(ChunkSnapshot.key(s.chunkX,s.chunkZ));}catch(IOException e){failure="Map save: "+e.getMessage();}}
 private ChunkSnapshot load(Path path){try{
  if(!Files.isRegularFile(path)||Files.size(path)>1024*1024)return null;
  try(var in=new DataInputStream(new java.util.zip.GZIPInputStream(Files.newInputStream(path)))){
   if(in.readInt()!=0x54464431)return null;int x=in.readInt(),z=in.readInt(),top=in.readInt(),floor=in.readInt();int[][] states=new int[16][];byte[][] block=new byte[16][],sky=new byte[16][];
   for(int i=0;i<16;i++)if(in.readBoolean()){states[i]=new int[4096];block[i]=new byte[4096];sky[i]=new byte[4096];for(int j=0;j<4096;j++){states[i][j]=in.readUnsignedShort();int light=in.readUnsignedByte();block[i][j]=(byte)(light&15);sky[i][j]=(byte)(light>>4);}}
   int[] grass=new int[256],water=new int[256];for(int i=0;i<256;i++)grass[i]=in.readInt();Arrays.fill(water,0x3F76E4);
   return new ChunkSnapshot(x,z,states,block,sky,top,floor,new ChunkSnapshot.Tints(grass,grass,grass,water));
  }
 }catch(IOException e){return null;}}
 private void clear(){if(fog!=null)fog.close();fog=null;maskAt=0;epoch++;revision=0;chunks.clear();captured.clear();reading.clear();stored.clear();restored.clear();completed.clear();for(Tile tile:tiles.values())if(tile.texture!=null)client.canvas.release(tile.texture);tiles.clear();if(clouds!=null)client.canvas.release(clouds);clouds=null;looks=null;}
 public void close(){closed=true;clear();worker.shutdownNow();disk.shutdown();try{if(!disk.awaitTermination(2,TimeUnit.SECONDS))OrnitheClient.LOG.warn("Map save queue is still draining at shutdown");}catch(InterruptedException interrupted){Thread.currentThread().interrupt();}}
}
