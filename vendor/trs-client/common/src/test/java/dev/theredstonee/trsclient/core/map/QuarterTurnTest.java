package dev.theredstonee.trsclient.core.map;
import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;
class QuarterTurnTest {
 @Test void allFourCameraAnglesRoundTripPositionsAndNegativeBlockCells(){double[] positions={-32.5,-16,-1,-.5,0,.5,15,16,32.25};for(int q=0;q<4;q++)for(double x:positions)for(double z:positions){double u=QuarterTurn.u(x,z,q),d=QuarterTurn.depth(x,z,q);assertEquals(x,QuarterTurn.x(u,d,q),1e-10);assertEquals(z,QuarterTurn.z(u,d,q),1e-10);int bx=(int)Math.floor(x),bz=(int)Math.floor(z);int bu=QuarterTurn.blockU(bx,bz,q),bd=QuarterTurn.blockDepth(bx,bz,q);assertEquals(bx,QuarterTurn.blockX(bu,bd,q));assertEquals(bz,QuarterTurn.blockZ(bu,bd,q));assertEquals(bu,(int)Math.floor(QuarterTurn.u(bx+.5,bz+.5,q)));assertEquals(bd,(int)Math.floor(QuarterTurn.depth(bx+.5,bz+.5,q)));}}
 @Test void transformedChangeBoxesKeepTheirMetadataFlags(){int[] box={2,3,5,8,12,17,3,91};assertArrayEquals(new int[]{-18,3,2,-6,12,8,3,91},QuarterTurn.box(box,1));assertArrayEquals(new int[]{2,3,5,8,12,17,3,91},box);}
 @Test void clockwiseTurnChangesTheViewingDirectionRatherThanOnlySpinningTheFinalImage(){assertEquals(-10,QuarterTurn.u(2,10,1));assertEquals(2,QuarterTurn.depth(2,10,1));assertEquals(2,QuarterTurn.u(2,10,4));}
}
