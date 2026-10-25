package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.redstone.*;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.property.Property;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/** 1.8.9 received-state adapter, translated from the existing legacy adapter. No server queries. */
public final class OrnitheRedstoneWorld implements RedstoneWorld {
  private static final Direction[] DIRECTIONS = Direction.values();
  public World world;

  public boolean loaded(int x, int y, int z) {
    return world != null
        && y >= 0
        && y < 256
        && !(world.getChunkSource().getChunk(x >> 4, z >> 4)
            instanceof net.minecraft.world.chunk.EmptyChunk);
  }

  // Block-IDs (Minecraft 1.8–1.12)
  private static final int STICKY_PISTON = 29;
  private static final int PISTON = 33;
  private static final int PISTON_HEAD = 34;
  private static final int LEVER = 69;
  private static final int TORCH_OFF = 75;
  private static final int TORCH_ON = 76;
  private static final int REPEATER_OFF = 93;
  private static final int REPEATER_ON = 94;
  private static final int LAMP_OFF = 123;
  private static final int LAMP_ON = 124;
  private static final int COMPARATOR_OFF = 149;
  private static final int COMPARATOR_ON = 150;
  private static final int DAYLIGHT = 151;
  private static final int REDSTONE_BLOCK = 152;
  private static final int HOPPER = 154;
  private static final int DAYLIGHT_INVERTED = 178;

  // --- Blockzustand ---

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static Property property(BlockState state, String name) {
    for (Object key : state.properties()) {
      Property p = (Property) key;
      if (name.equals(p.getName())) return p;
    }
    return null;
  }

  /** Zahl-Eigenschaft (z. B. "power") oder -1. */
  @SuppressWarnings({"rawtypes", "unchecked"})
  private static int intValue(BlockState state, String name) {
    Property p = property(state, name);
    if (p == null) return -1;
    Object v = state.get(p);
    return v instanceof Integer ? ((Integer) v).intValue() : -1;
  }

  /** Wahr/falsch-Eigenschaft (z. B. "powered"); null, wenn der Block sie nicht hat. */
  @SuppressWarnings({"rawtypes", "unchecked"})
  private static Boolean boolValue(BlockState state, String name) {
    Property p = property(state, name);
    if (p == null) return null;
    Object v = state.get(p);
    return v instanceof Boolean ? (Boolean) v : null;
  }

  /** Wert einer Eigenschaft als Text ("subtract", "north" …) oder null. */
  @SuppressWarnings({"rawtypes", "unchecked"})
  private static String textValue(BlockState state, String name) {
    Property p = property(state, name);
    return p == null ? null : p.getName(state.get(p));
  }

  /** Richtung aus der Eigenschaft "facing" als Index (unten 0 … Osten 5) oder -1. */
  @SuppressWarnings({"rawtypes", "unchecked"})
  private static int facing(BlockState state) {
    Property p = property(state, "facing");
    if (p == null) return -1;
    Object v = state.get(p);
    return v instanceof Direction ? ((Direction) v).getId() : -1;
  }

  private BlockState state(int x, int y, int z) {
    return world.getBlockState(new BlockPos(x, y, z));
  }

  @Override
  public boolean probe(int x, int y, int z, BlockProbe out) {
    out.clear();
    if (!loaded(x, y, z)) return false;
    BlockPos pos = new BlockPos(x, y, z);
    BlockState state = world.getBlockState(pos);
    Block b = state.getBlock();
    int id = Block.getId(b);
    if (b instanceof RedstoneWireBlock) {
      out.kind = RedstoneKind.DUST;
      out.power = intValue(state, "power");
    } else if (id == REPEATER_OFF || id == REPEATER_ON) {
      out.kind = RedstoneKind.REPEATER;
      state = b.resolveVirtualProperties(state, world, pos);
      out.delay = intValue(state, "delay");
      out.locked = Boolean.TRUE.equals(boolValue(state, "locked"));
      out.powered(id == REPEATER_ON);
      out.facing = facing(state);
    } else if (id == COMPARATOR_OFF || id == COMPARATOR_ON) {
      out.kind = RedstoneKind.COMPARATOR;
      out.subtract = "subtract".equals(textValue(state, "mode"));
      Boolean on = boolValue(state, "powered");
      out.powered(on != null ? on.booleanValue() : id == COMPARATOR_ON);
      out.facing = facing(state);
    } else if (id == TORCH_OFF || id == TORCH_ON) {
      out.kind = RedstoneKind.TORCH;
      out.powered(id == TORCH_ON);
    } else if (id == LEVER) {
      out.kind = RedstoneKind.LEVER;
      out.powered(Boolean.TRUE.equals(boolValue(state, "powered")));
    } else if (b instanceof ButtonBlock) {
      out.kind = RedstoneKind.BUTTON;
      out.powered(Boolean.TRUE.equals(boolValue(state, "powered")));
    } else if (b instanceof AbstractPressurePlateBlock) {
      out.kind = RedstoneKind.PLATE;
      int power = intValue(state, "power");
      if (power >= 0) out.power = power;
      else out.powered(Boolean.TRUE.equals(boolValue(state, "powered")));
    } else if (id == PISTON || id == STICKY_PISTON) {
      out.kind = RedstoneKind.PISTON;
      out.extended = Boolean.TRUE.equals(boolValue(state, "extended"));
      out.powered(out.extended);
      out.received = neighborSignal(pos);
    } else if (id == PISTON_HEAD) {
      out.kind = RedstoneKind.PISTON;
      out.extended = true;
      out.powered(true);
      int f = facing(state);
      if (f >= 0) {
        Direction d = Direction.byId(f);
        out.received = neighborSignal(pos.offset(d.getOpposite()));
      }
    } else if (id == LAMP_OFF || id == LAMP_ON) {
      out.kind = RedstoneKind.LAMP;
      out.powered(id == LAMP_ON);
      out.received = neighborSignal(pos);

    } else if (id == DAYLIGHT || id == DAYLIGHT_INVERTED) {
      out.kind = RedstoneKind.DAYLIGHT;
      out.power = intValue(state, "power");
    } else if (id == REDSTONE_BLOCK) {
      out.kind = RedstoneKind.REDSTONE_BLOCK;
      out.powered(true);
    } else {
      probeOther(pos, state, id, out);
    }
    return out.kind != RedstoneKind.NONE;
  }

  /** Alles andere über die Eigenschaften: Haken, Türen, Werfer, Schienen, Notenblock, Behälter … */
  private void probeOther(BlockPos pos, BlockState state, int id, BlockProbe out) {
    BlockEntity te = world.getBlockEntity(pos);
    out.container = te instanceof Inventory;
    int power = intValue(state, "power");
    Boolean powered = boolValue(state, "powered");
    Boolean triggered = boolValue(state, "triggered");
    if (canProvidePower(state)) {
      out.kind = RedstoneKind.SOURCE;
      if (power >= 0) {
        out.power = power;
      } else {
        int best = 0;
        for (Direction f : DIRECTIONS) best = Math.max(best, weakPower(state, pos, f));
        out.power = best;
        if (powered != null) out.powered(powered.booleanValue());
      }
    } else if (powered != null) {
      out.kind = RedstoneKind.CONSUMER;
      out.powered(powered.booleanValue());
    } else if (triggered != null) {
      out.kind = RedstoneKind.CONSUMER;
      out.powered(triggered.booleanValue());
    } else if (id == HOPPER) {
      out.kind = RedstoneKind.CONSUMER;
      out.powered(Boolean.FALSE.equals(boolValue(state, "enabled")));
    } else if (out.container) {
      out.kind = RedstoneKind.CONTAINER;
    } else if (te == null && hasComparatorOutput(state)) {
      out.kind = RedstoneKind.ANALOG;
      out.analog = comparatorOutput(state, pos);
    }
    if (out.kind.receives()) out.received = neighborSignal(pos);
  }

  private static boolean canProvidePower(BlockState s) {
    return s.getBlock().isSignalSource();
  }

  private int weakPower(BlockState s, BlockPos p, Direction d) {
    return s.getBlock().getSignal(world, p, s, d);
  }

  private static boolean hasComparatorOutput(BlockState s) {
    return s.getBlock().isAnalogSignalSource();
  }

  private int comparatorOutput(BlockState s, BlockPos p) {
    return s.getBlock().getAnalogSignal(world, p);
  }

  private int neighborSignal(BlockPos p) {
    int best = 0;
    for (Direction d : DIRECTIONS) {
      BlockPos n = p.offset(d);
      if (loaded(n.getX(), n.getY(), n.getZ())) best = Math.max(best, world.getSignal(n, d));
    }
    return best;
  }

  public String name(int x, int y, int z) {
    return loaded(x, y, z) ? state(x, y, z).getBlock().getName() : "";
  }

  public int dustPower(int x, int y, int z) {
    if (!loaded(x, y, z)) return -1;
    BlockState s = state(x, y, z);
    return s.getBlock() instanceof RedstoneWireBlock ? s.get(RedstoneWireBlock.POWER) : -1;
  }

  public int signal(int x, int y, int z, int dir) {
    return loaded(x, y, z) ? world.getSignal(new BlockPos(x, y, z), Direction.byId(dir)) : 0;
  }

  public int sideSignal(int x, int y, int z, int dir) {
    if (!loaded(x, y, z)) return 0;
    BlockState s = state(x, y, z);
    if (!s.getBlock().isSignalSource()) return 0;
    if (Block.getId(s.getBlock()) == REDSTONE_BLOCK) return 15;
    if (s.getBlock() instanceof RedstoneWireBlock) return s.get(RedstoneWireBlock.POWER);
    return world.getDirectSignal(new BlockPos(x, y, z), Direction.byId(dir));
  }

  public int analog(int x, int y, int z) {
    if (!loaded(x, y, z)) return NO_ANALOG;
    BlockPos p = new BlockPos(x, y, z);
    BlockState s = world.getBlockState(p);
    if (!hasComparatorOutput(s)) return NO_ANALOG;
    return world.getBlockEntity(p) != null ? CONTAINER_ANALOG : comparatorOutput(s, p);
  }

  public boolean conductor(int x, int y, int z) {
    return loaded(x, y, z) && world.isFullCube(new BlockPos(x, y, z));
  }

  public boolean opaque(int x, int y, int z) {
    return conductor(x, y, z) && state(x, y, z).getBlock().isOpaque();
  }
}
