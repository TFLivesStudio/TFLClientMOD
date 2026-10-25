package dev.theredstonee.trsclient.core.map.volume;
import dev.theredstonee.trsclient.core.module.ChoiceSetting;
public enum CloudQuality implements ChoiceSetting.Option {
    OFF("Off",0,0,0), LOW("Low",128,16,4), BALANCED("Balanced",224,28,8), HIGH("High",384,48,8);
    public final int width,steps,fps;private final String label;
    CloudQuality(String label,int width,int steps,int fps){this.label=label;this.width=width;this.steps=steps;this.fps=fps;}
    public String label(){return label;}
}
