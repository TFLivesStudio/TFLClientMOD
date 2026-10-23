package dev.theredstonee.trsclient.core.ui;

/** Shared native TFL Selection primitives. Original TFL modifications, GPL-3.0-only. */
public final class SelectionStyle {
    public static final int SPACE = 8;
    public static final int RADIUS = 5;
    public static final int BLUE = 0xFF3B82F6;
    private SelectionStyle() {}
    /** Bounded scanline rounded rectangle; no textures, allocations or GL state changes. */
    public static void rounded(Canvas c,int x,int y,int w,int h,int radius,int color) {
        if(w<=0||h<=0||(color>>>24)==0) return;
        int r=Math.max(0,Math.min(radius,Math.min(w,h)/2));
        if(r==0) {c.fill(x,y,x+w,y+h,color);return;}
        c.fill(x,y+r,x+w,y+h-r,color);
        for(int row=0;row<r;row++) {
            double dy=r-row-0.5;
            int inset=(int)Math.ceil(r-Math.sqrt(r*r-dy*dy));
            c.fill(x+inset,y+row,x+w-inset,y+row+1,color);
            c.fill(x+inset,y+h-row-1,x+w-inset,y+h-row,color);
        }
    }
    public static void panel(Canvas c,int x,int y,int w,int h,int fill,int border) {
        rounded(c,x,y,w,h,RADIUS,border);
        rounded(c,x+1,y+1,w-2,h-2,RADIUS-1,fill);
    }
    public static void button(Canvas c,int x,int y,int w,int h,boolean primary,boolean hover) {
        Theme t=Theme.get();
        panel(c,x,y,w,h,primary?(hover?t.accentHover:t.accent):(hover?t.surfaceHover:t.surfaceHigh),hover?t.accent:t.border);
    }
}
