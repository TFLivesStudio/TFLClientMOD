package dev.theredstonee.trsclient.map;
/** The TFL module switch and a one-time, idempotent import into Diorama's per-world store. */
public final class TflDiorama {
 private TflDiorama(){}
 //? if >=26.3 {
 /*private static Object importedStore;private static dev.diorama.Settings.Quality appliedQuality;
 public static void saveFromNative(dev.diorama.Settings settings){
  var client=dev.theredstonee.trsclient.TrsClient.get();if(client==null)return;var m=client.modules();
  m.minimap.setEnabled(settings.minimap);m.minimapSize.set(settings.minimapSize);m.minimapOpacity.set(settings.minimapOpacity);m.minimapCoords.set(settings.coordinates);m.minimapBiome.set(settings.biome);m.minimapCompass.set(settings.compass);
  dev.theredstonee.trsclient.core.module.TrsModules.MinimapZoom zoom=dev.theredstonee.trsclient.core.module.TrsModules.MinimapZoom.NORMAL;double difference=Double.MAX_VALUE;for(var option:dev.theredstonee.trsclient.core.module.TrsModules.MinimapZoom.values()){double d=Math.abs(option.pixelsPerBlock()-settings.zoom());if(d<difference){difference=d;zoom=option;}}m.minimapZoom.set(zoom);
  m.minimapMobIcons.set(settings.radarStyle==dev.diorama.Settings.RadarStyle.DOTS?dev.theredstonee.trsclient.core.module.TrsModules.MobIcons.SYMBOLS:dev.theredstonee.trsclient.core.module.TrsModules.MobIcons.HEADS);
  m.minimapPlayers.set(settings.radarPlayers);m.worldMapPlayers.set(settings.radarPlayers);m.minimapHostile.set(settings.radarHostile);m.worldMapHostile.set(settings.radarHostile);m.minimapPassive.set(settings.radarPassive);m.worldMapPassive.set(settings.radarPassive);m.minimapWaypoints.set(settings.waypointsOnMap);m.worldMapWaypoints.set(settings.waypointsOnMap);
  m.minimapShape.set(settings.minimapShape==dev.diorama.Settings.Shape.ROUND?dev.theredstonee.trsclient.core.module.TrsModules.MapShape.ROUND:dev.theredstonee.trsclient.core.module.TrsModules.MapShape.SQUARE);
  m.minimapCaveMode.set(settings.autoCaves?dev.theredstonee.trsclient.core.module.TrsModules.CaveMode.AUTO:dev.theredstonee.trsclient.core.module.TrsModules.CaveMode.OFF);
  m.worldMapCloudQuality.set(settings.quality==dev.diorama.Settings.Quality.FAST?dev.theredstonee.trsclient.core.map.volume.CloudQuality.LOW:dev.theredstonee.trsclient.core.map.volume.CloudQuality.HIGH);
  client.saveConfig();
 }
 public static void tick(){
  var client=dev.theredstonee.trsclient.TrsClient.get();if(client==null)return;
  var settings=dev.diorama.Settings.get();var m=client.modules();
  settings.minimap=m.minimap.isEnabled();settings.minimapSize=m.minimapSize.getInt();settings.minimapOpacity=m.minimapOpacity.getInt();settings.coordinates=m.minimapCoords.get();settings.biome=m.minimapBiome.get();settings.compass=m.minimapCompass.get();
  settings.minimapShape=m.minimapShape.get()==dev.theredstonee.trsclient.core.module.TrsModules.MapShape.ROUND?dev.diorama.Settings.Shape.ROUND:dev.diorama.Settings.Shape.SQUARE;
  double desiredZoom=m.minimapZoom.get().pixelsPerBlock();double difference=Double.MAX_VALUE;for(int i=0;i<dev.diorama.Settings.ZOOMS.length;i++){double d=Math.abs(dev.diorama.Settings.ZOOMS[i]-desiredZoom);if(d<difference){difference=d;settings.minimapZoom=i;}}
  settings.radarStyle=m.minimapMobIcons.get()==dev.theredstonee.trsclient.core.module.TrsModules.MobIcons.SYMBOLS?dev.diorama.Settings.RadarStyle.DOTS:dev.diorama.Settings.RadarStyle.HEADS;
  settings.autoCaves=m.minimapCaveMode.get()==dev.theredstonee.trsclient.core.module.TrsModules.CaveMode.AUTO;
  var quality=m.worldMapCloudQuality.get()==dev.theredstonee.trsclient.core.map.volume.CloudQuality.LOW?dev.diorama.Settings.Quality.FAST:dev.diorama.Settings.Quality.FANCY;
  if(appliedQuality!=quality){settings.quality=quality;settings.apply();appliedQuality=quality;}
  var store=dev.diorama.waypoint.WaypointStore.get();
  if(store==null){importedStore=null;return;}if(store==importedStore)return;
  var legacy=client.waypoints();if(legacy==null||legacy.worldKey().isEmpty())return;
  java.util.Set<java.util.UUID> present=new java.util.HashSet<>();for(var waypoint:store.all())present.add(waypoint.id);
  java.util.List<dev.diorama.waypoint.Waypoint> added=new java.util.ArrayList<>();
  for(var old:legacy.store().all(legacy.worldKey())){
   String identity="tfl:"+legacy.worldKey()+":"+old.dimension+":"+old.name+":"+old.x+":"+old.y+":"+old.z+":"+old.created;
   java.util.UUID id=java.util.UUID.nameUUIDFromBytes(identity.getBytes(java.nio.charset.StandardCharsets.UTF_8));if(present.contains(id))continue;
   String dim=switch(old.dimension){case "dim0"->"minecraft:overworld";case "dim-1"->"minecraft:the_nether";case "dim1"->"minecraft:the_end";case ""->dev.diorama.waypoint.Waypoints.dimension();default->old.dimension;};
   var waypoint=new dev.diorama.waypoint.Waypoint(old.name,old.x,old.y,old.z,dim,0xff000000|old.color);waypoint.id=id;waypoint.created=old.created;waypoint.visible=old.visible;if(old.death)waypoint.kind=dev.diorama.waypoint.Waypoint.Kind.DEATH;added.add(waypoint);
  }
  store.addAll(added);importedStore=store;
 }
 *///?} else {
 public static void tick(){}
 //?}
}
