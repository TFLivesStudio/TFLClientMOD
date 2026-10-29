package dev.theredstonee.trsclient.ornithe;

import net.minecraft.block.Block;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.state.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.WorldGeneratorType;

/** Copies state-dependent legacy bounds on the client thread, never during a worker bake. */
final class LegacyBlockGeometry implements WorldView {
    private BlockState state;
    private static final BlockPos ORIGIN = new BlockPos(0, 0, 0);

    float[] boxes(BlockState state) {
        this.state = state;
        Block block = state.getBlock();
        try {
            block.updateShape(this, ORIGIN);
            if (block instanceof StairsBlock) {
                boolean upper = state.get(StairsBlock.HALF) == StairsBlock.Half.TOP;
                float y0 = upper ? .5f : 0, y1 = upper ? 1 : .5f;
                float x0 = 0, z0 = 0, x1 = 1, z1 = 1;
                switch (state.get(StairsBlock.FACING)) {
                    case EAST -> x0 = .5f;
                    case WEST -> x1 = .5f;
                    case SOUTH -> z0 = .5f;
                    case NORTH -> z1 = .5f;
                    default -> { }
                }
                return new float[] {0,y0,0,1,y1,1,x0,upper?0:.5f,z0,x1,upper?.5f:1,z1};
            }
            return new float[] {(float)block.getMinX(),(float)block.getMinY(),(float)block.getMinZ(),
                    (float)block.getMaxX(),(float)block.getMaxY(),(float)block.getMaxZ()};
        } finally {
            block.resetShape();
        }
    }

    public net.minecraft.block.entity.BlockEntity getBlockEntity(BlockPos pos) { return null; }
    public int getLightColor(BlockPos pos,int min) { return 0xF000F0; }
    public BlockState getBlockState(BlockPos pos) {
        return pos.equals(ORIGIN) ? state : net.minecraft.block.Blocks.AIR.getStateFromMetadata(0);
    }
    public boolean isAir(BlockPos pos) { return !pos.equals(ORIGIN); }
    public Biome getBiome(BlockPos pos) { return Biome.PLAINS; }
    public boolean isEmpty() { return false; }
    public int getDirectSignal(BlockPos pos,Direction direction) { return 0; }
    public WorldGeneratorType getGeneratorType() { return WorldGeneratorType.DEFAULT; }
}
