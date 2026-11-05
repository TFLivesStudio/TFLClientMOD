package dev.theredstonee.trsclient.core.terrain;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;

/** A separate, bounded derived cache. Never modifies Diorama snapshots, catalogs or waypoints. */
public final class TerrainStore {
    private static final int MAGIC=0x54464c35, VERSION=2, MAX_FILES=131072;
    private final Path root,folder;
    private volatile long budget;
    public TerrainStore(Path root,String worldAndDimension,long budget) {
        if(worldAndDimension==null || worldAndDimension.isEmpty() || budget<1)throw new IllegalArgumentException("LOD cache requires world identity");
        this.root=root;this.folder=root.resolve(hash(worldAndDimension));this.budget=budget;
    }
    public Path folder(){return folder;}
    public void budget(long bytes){if(bytes<1)throw new IllegalArgumentException("LOD disk budget");budget=bytes;}
    /** A recreated local save must not inherit an older world's derived cache merely by folder name. */
    public static String localSaveIdentity(Path save) throws IOException {
        Path file=save.resolve("tfl-far-terrain-id.txt");Files.createDirectories(save);
        if(!Files.exists(file))try{Files.write(file,UUID.randomUUID().toString().getBytes(StandardCharsets.US_ASCII),StandardOpenOption.CREATE_NEW);}catch(FileAlreadyExistsException raced){}
        if(Files.size(file)>64)throw new IOException("Invalid terrain save identity");
        String id=new String(Files.readAllBytes(file),StandardCharsets.US_ASCII).trim();
        try{return UUID.fromString(id).toString();}catch(IllegalArgumentException e){throw new IOException("Invalid terrain save identity; original preserved",e);}
    }
    private static String hash(String key) {
        try {byte[] bytes=MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));StringBuilder b=new StringBuilder();for(byte v:bytes)b.append(String.format(Locale.ROOT,"%02x",v&255));return b.toString();}
        catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    private Path file(int x,int z){return folder.resolve(x+"_"+z+".lod");}
    public void write(TerrainColumns columns) throws IOException {
        if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();
        Files.createDirectories(folder);
        ByteArrayOutputStream bytes=new ByteArrayOutputStream(2048);
        try(DataOutputStream data=new DataOutputStream(bytes)){
            data.writeInt(MAGIC);data.writeInt(VERSION);data.writeInt(columns.x);data.writeInt(columns.z);
            for(int i=0;i<256;i++){data.writeShort(columns.height(i));data.writeInt(columns.color(i));data.writeByte(columns.packedLight(i));data.writeInt(columns.state(i));data.writeInt(columns.tint(i));}
        }
        CRC32 checksum=new CRC32();checksum.update(bytes.toByteArray());
        Path temp=Files.createTempFile(folder,columns.x+"_"+columns.z+".",".tmp");
        try {
            try(DataOutputStream out=new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(temp)))){out.write(bytes.toByteArray());out.writeLong(checksum.getValue());}
            try {Files.move(temp,file(columns.x,columns.z),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException e){Files.move(temp,file(columns.x,columns.z),StandardCopyOption.REPLACE_EXISTING);}
        } finally {Files.deleteIfExists(temp);}
    }
    public TerrainColumns read(int x,int z) throws IOException {
        Path file=file(x,z);if(!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS))return null;
        if(Files.size(file)>65536)throw new IOException("LOD entry too large");
        byte[] bytes=new byte[16+256*15];long checksum;
        try(DataInputStream in=new DataInputStream(new GZIPInputStream(Files.newInputStream(file)))) {in.readFully(bytes);checksum=in.readLong();if(in.read()!=-1)throw new IOException("Trailing LOD data");}
        CRC32 crc=new CRC32();crc.update(bytes);if(crc.getValue()!=checksum)throw new IOException("LOD checksum mismatch");
        try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes))) {
            if(in.readInt()!=MAGIC || in.readInt()!=VERSION || in.readInt()!=x || in.readInt()!=z)throw new IOException("LOD identity/version mismatch");
            short[] heights=new short[256];int[] colors=new int[256],states=new int[256],tints=new int[256];byte[] lights=new byte[256];
            for(int i=0;i<256;i++){heights[i]=in.readShort();colors[i]=in.readInt();lights[i]=in.readByte();states[i]=in.readInt();tints[i]=in.readInt();}
            try{return new TerrainColumns(x,z,heights,colors,lights,states,tints);}catch(IllegalArgumentException e){throw new IOException("Invalid LOD data",e);}
        }
    }
    public Set<Long> index() throws IOException {
        Set<Long> keys=new HashSet<Long>();if(!Files.isDirectory(folder))return keys;
        try(DirectoryStream<Path> files=Files.newDirectoryStream(folder,"*.lod")){
            for(Path path:files){if(Thread.currentThread().isInterrupted())break;if(keys.size()>=MAX_FILES)break;
                String[] xy=path.getFileName().toString().replace(".lod","").split("_");
                if(xy.length!=2)continue;
                try {int x=Integer.parseInt(xy[0]),z=Integer.parseInt(xy[1]);if(Math.abs((long)x)<=2000000 && Math.abs((long)z)<=2000000)keys.add(TerrainColumns.key(x,z));}catch(NumberFormatException ignored){}
            }
        }return keys;
    }
    /** Only evicts this derived format under this dedicated root. Call exclusively on a disk worker. */
    public void trim() throws IOException {
        if(!Files.isDirectory(root))return;
        final class Entry {final Path path;final long bytes,time;Entry(Path p) throws IOException {path=p;bytes=Files.size(p);time=Files.getLastModifiedTime(p).toMillis();}}
        while(!Thread.currentThread().isInterrupted()){
            PriorityQueue<Entry> oldest=new PriorityQueue<Entry>(Comparator.comparingLong((Entry e)->e.time).reversed());long total=0;int count=0;
            try(java.util.stream.Stream<Path> paths=Files.walk(root,2)){
                Iterator<Path> it=paths.iterator();while(it.hasNext()){
                    if(Thread.currentThread().isInterrupted())return;Path p=it.next();
                    if(!p.getFileName().toString().matches("-?\\d+_-?\\d+\\.lod") || p.getParent()==null || !p.getParent().getFileName().toString().matches("[a-f0-9]{64}") || !Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS))continue;
                    Entry e=new Entry(p);total+=e.bytes;count++;oldest.offer(e);if(oldest.size()>4096)oldest.poll();
                }
            }
            if(total<=budget && count<=MAX_FILES)return;
            List<Entry> batch=new ArrayList<Entry>(oldest);batch.sort(Comparator.comparingLong(e->e.time));boolean removed=false;
            for(Entry e:batch){if(total<=budget && count<=MAX_FILES)return;if(Thread.currentThread().isInterrupted())return;if(Files.deleteIfExists(e.path)){total-=e.bytes;count--;removed=true;}}
            if(!removed)return;
        }
    }
}
