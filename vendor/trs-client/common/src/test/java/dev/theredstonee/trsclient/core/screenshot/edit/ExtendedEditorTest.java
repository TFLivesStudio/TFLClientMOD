package dev.theredstonee.trsclient.core.screenshot.edit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
class ExtendedEditorTest {
 @Test void solidRedactionIsOpaqueAndCanBeUndone(){int[] pixels=new int[64*64];Arrays.fill(pixels,0xffabcdef);Shape redaction=new Shape.BoxTool(Shape.Kind.FILL,12,12,44,44,0xff172031,2);EditRender.draw(pixels,64,64,Collections.singletonList(redaction),1);assertEquals(0xff172031,pixels[32*64+32]);assertEquals(0xffabcdef,pixels[0]);EditHistory history=new EditHistory(EditState.initial(64,64));history.push(history.current().withShape(redaction));assertTrue(history.undo());assertTrue(history.current().pristine());}
 @Test void highlighterLeavesUnderlyingDetailVisible(){int[] pixels=new int[32*32];Arrays.fill(pixels,0xff0000ff);EditRender.draw(pixels,32,32,Collections.singletonList(new Shape.BoxTool(Shape.Kind.HIGHLIGHT,0,0,32,32,0xffff0000,1)),1);int p=pixels[16*32+16];assertTrue((p&255)>100);assertTrue((p>>16&255)>20);}
 @Test void rotationKeepsNewToolsInTheImage(){Shape box=new Shape.BoxTool(Shape.Kind.ELLIPSE,3,5,12,18,0xffffffff,2);Shape rotated=box.rotated(30);assertArrayEquals(new float[]{12,3,25,12},rotated.bounds());assertEquals(Shape.Kind.ELLIPSE,rotated.kind);}
}
