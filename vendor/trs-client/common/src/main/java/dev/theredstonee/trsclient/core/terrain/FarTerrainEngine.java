package dev.theredstonee.trsclient.core.terrain;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Main-thread coordinator over immutable columns; all disk I/O and meshing run on bounded workers. */
public final class FarTerrainEngine implements AutoCloseable {
    public static final int MAX_COLUMNS=4096;
    private final TerrainStore store;
    private final java.util.function.UnaryOperator<TerrainColumns> palette;
    private final ThreadPoolExecutor work=pool(2,24,"TFL-LOD-build"),disk=pool(1,64,"TFL-LOD-disk");
    private final ArrayBlockingQueue<Runnable> completed=new ArrayBlockingQueue<Runnable>(64);
    private final ArrayBlockingQueue<LodMesh> meshes=new ArrayBlockingQueue<LodMesh>(8);
    private final LinkedHashMap<Long,TerrainColumns> columns=new LinkedHashMap<Long,TerrainColumns>(64,.75f,true);
    private final Set<Long> known=new LinkedHashSet<Long>(),loading=ConcurrentHashMap.newKeySet();
    private final Set<String> building=ConcurrentHashMap.newKeySet();
    private final Map<Long,Long> versions=new HashMap<Long,Long>(),regions=new HashMap<Long,Long>();
    private final AtomicBoolean planning=new AtomicBoolean(),closed=new AtomicBoolean();
    private final AtomicBoolean saving=new AtomicBoolean();
    private final ConcurrentHashMap<Long,TerrainColumns> unsaved=new ConcurrentHashMap<Long,TerrainColumns>();
    private final Set<Long> saveFailed=ConcurrentHashMap.newKeySet();
    private long[] targets=new long[0];
    private int cursor,centerX=Integer.MIN_VALUE,centerZ,range,saveCount;
    private long serial;
    private long diskBudget;
    public volatile long generationNanos,diskHits,meshesBuilt;
    public volatile String failure="";
    public FarTerrainEngine(Path root,String identity,long diskBudget) {
        this(root,identity,diskBudget,c->c);
    }
    public FarTerrainEngine(Path root,String identity,long diskBudget,java.util.function.UnaryOperator<TerrainColumns> palette) {
        this.palette=palette;
        this.diskBudget=diskBudget;
        store=new TerrainStore(root,identity,diskBudget);
        submit(disk,()->{
            try {Set<Long> index=store.index();deliver(()->{known.addAll(index);centerX=Integer.MIN_VALUE;});store.trim();}
            catch(IOException e){failure="cache-read";}
        });
    }
    private static ThreadPoolExecutor pool(int count,int queue,String name) {
        ThreadPoolExecutor result=new ThreadPoolExecutor(0,count,1,TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(queue),r->{Thread t=new Thread(r,name);t.setDaemon(true);return t;});
        // A nonzero core allows real concurrent meshing; time out all threads when idle.
        result.setCorePoolSize(count);result.allowCoreThreadTimeOut(true);return result;
    }
    private boolean submit(ThreadPoolExecutor executor,Runnable task) {if(closed.get())return false;try{executor.execute(task);return true;}catch(RejectedExecutionException e){return false;}}
    private void deliver(Runnable result) {if(closed.get())return;try{while(!closed.get()&&!completed.offer(result,50,TimeUnit.MILLISECONDS)){} }catch(InterruptedException e){Thread.currentThread().interrupt();}}
    /** Supplier must contain received immutable data only, not a live Minecraft world/chunk. */
    public boolean capture(int x,int z,Callable<TerrainColumns> capture) {
        if(closed.get() || unsaved.size()>=MAX_COLUMNS)return false;long key=TerrainColumns.key(x,z),version=++serial;
        Long before=versions.put(key,version);
        boolean accepted=submit(work,()->{
            try {TerrainColumns c=capture.call();if(c==null||closed.get())return;deliver(()->accept(c,version,true));}
            catch(Exception e){if(!(e instanceof InterruptedException))failure="capture";else Thread.currentThread().interrupt();}
        });
        if(!accepted){if(before==null)versions.remove(key);else versions.put(key,before);}return accepted;
    }
    private void accept(TerrainColumns c,long version,boolean persist) {
        if(closed.get() || !Objects.equals(versions.get(c.key()),version))return;
        TerrainColumns previous=columns.get(c.key());
        if(c.same(previous)){if(persist&&saveFailed.contains(c.key())){unsaved.put(c.key(),c);save();}return;}
        known.add(c.key());while(known.size()>131072){Iterator<Long> it=known.iterator();it.next();it.remove();}
        columns.put(c.key(),c);invalidate(c.x,c.z);
        while(columns.size()>MAX_COLUMNS){Iterator<Map.Entry<Long,TerrainColumns>> it=columns.entrySet().iterator();TerrainColumns old=it.next().getValue();it.remove();versions.remove(old.key());invalidate(old.x,old.z);}
        if(persist){unsaved.put(c.key(),c);save();}
    }
    private void save() {
        if(unsaved.isEmpty() || !saving.compareAndSet(false,true))return;
        if(!submit(disk,()->{
            try {
                while(!unsaved.isEmpty()){
                    Map.Entry<Long,TerrainColumns> entry=unsaved.entrySet().iterator().next();
                    try {store.write(entry.getValue());saveFailed.remove(entry.getKey());if(++saveCount%1024==0)store.trim();}
                    catch(IOException e){failure="cache-write";saveFailed.add(entry.getKey());}
                    unsaved.remove(entry.getKey(),entry.getValue());
                }
            } finally {saving.set(false);}
        }))saving.set(false);
    }
    private void invalidate(int x,int z) {
        // Region mesh skirts also consult adjacent columns. Bump every region that can depend on this chunk.
        for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){
            int rx=(x+dx)>>2,rz=(z+dz)>>2;long key=TerrainColumns.key(rx,rz);boolean present=false;
            for(int cz=0;cz<4&&!present;cz++)for(int cx=0;cx<4;cx++)if(columns.containsKey(TerrainColumns.key(rx*4+cx,rz*4+cz))){present=true;break;}
            if(present)regions.put(key,++serial);else regions.remove(key);
        }
    }
    public void tick(int chunkX,int chunkZ,int distance) {
        if(closed.get())return;
        for(int i=0;i<24;i++){Runnable result=completed.poll();if(result==null)break;result.run();}
        save();
        if(((chunkX>>2)!=centerX || (chunkZ>>2)!=centerZ || distance!=range) && planning.compareAndSet(false,true)) {
            centerX=chunkX>>2;centerZ=chunkZ>>2;range=distance;
            final Long[] points=known.toArray(new Long[0]);
            if(!submit(work,()->{
                try {
                    Arrays.sort(points,Comparator.comparingLong(k->distance(k,chunkX,chunkZ)));
                    long[] selected=new long[Math.min(MAX_COLUMNS,points.length)];int n=0;
                    for(long point:points){if(Math.abs((long)(int)(point>>32)-chunkX)<=distance+8 && Math.abs((long)(int)point-chunkZ)<=distance+8){selected[n++]=point;if(n==selected.length)break;}}
                    final long[] plan=Arrays.copyOf(selected,n);deliver(()->{targets=plan;cursor=0;});
                } finally {planning.set(false);}
            }))planning.set(false);
        }
        int queued=0;
        while(cursor<targets.length && queued<4) {
            long key=targets[cursor++];if(columns.containsKey(key)||!loading.add(key))continue;
            if(disk.getQueue().remainingCapacity()<2){loading.remove(key);cursor--;break;}
            long version=versions.containsKey(key)?versions.get(key):0;versions.put(key,version);
            if(!submit(disk,()->{try {TerrainColumns raw=store.read((int)(key>>32),(int)key);if(raw!=null){TerrainColumns c=palette.apply(raw);diskHits++;deliver(()->accept(c,version,false));}}
                catch(IOException e){if(!closed.get())failure="cache-read";}finally{loading.remove(key);}})){loading.remove(key);cursor--;break;}queued++;
        }
    }
    private static long distance(long key,int x,int z){long dx=(long)(int)(key>>32)-x,dz=(long)(int)key-z;return dx*dx+dz*dz;}
    public Map<Long,Long> regions() {return Collections.unmodifiableMap(regions);}
    public int columnCount(){return columns.size();}
    public int knownCount(){return known.size();}
    public void diskBudget(long bytes){if(bytes==diskBudget || closed.get())return;store.budget(bytes);if(submit(disk,()->{try{store.trim();}catch(IOException e){failure="cache-trim";}}))diskBudget=bytes;}
    public TerrainColumns column(int x,int z){return columns.get(TerrainColumns.key(x,z));}
    public boolean request(int x,int z,int step) {
        long region=TerrainColumns.key(x,z),revision=regions.containsKey(region)?regions.get(region):0;
        String job=x+":"+z+":"+step+":"+revision;if(!building.add(job))return false;
        Map<Long,TerrainColumns> input=new HashMap<Long,TerrainColumns>();
        for(int dz=-1;dz<=4;dz++)for(int dx=-1;dx<=4;dx++){long k=TerrainColumns.key(x*4+dx,z*4+dz);TerrainColumns c=columns.get(k);if(c!=null)input.put(k,c);}
        boolean accepted=submit(work,()->{
            long start=System.nanoTime();
            try {LodMesh result=LodMesh.build(x,z,step,revision,input);generationNanos+=System.nanoTime()-start;meshesBuilt++;
                while(!closed.get()&&!meshes.offer(result,50,TimeUnit.MILLISECONDS)){}
            }catch(InterruptedException e){Thread.currentThread().interrupt();}finally{building.remove(job);}
        });if(!accepted)building.remove(job);return accepted;
    }
    public LodMesh pollMesh(){LodMesh m;while((m=meshes.poll())!=null){Long revision=regions.get(TerrainColumns.key(m.regionX,m.regionZ));if(revision!=null&&revision==m.revision)return m;}return null;}
    public boolean stopped(){return closed.get() && work.isTerminated() && disk.isTerminated();}
    public boolean awaitStopped(long millis) throws InterruptedException{return disk.awaitTermination(millis,TimeUnit.MILLISECONDS) && work.isTerminated();}
    public void close(){
        if(closed.get())return;
        // Queue final accepted writes while reserving capacity, then let that bounded disk work finish.
        save();if(!closed.compareAndSet(false,true))return;work.shutdownNow();disk.shutdown();
        completed.clear();meshes.clear();columns.clear();known.clear();regions.clear();versions.clear();targets=new long[0];
    }
}
