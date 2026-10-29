package dev.theredstonee.trsclient.core.ui.background;
/** Diagnostic snapshot: reading this object does not start playback or change saved preferences. */
public final class PlaybackMetrics {
    public String decoderImplementation="";
    public String mode="",decoderState="not installed",fallbackReason="",failure="";
    public long sessions,framesDecoded,framesUploaded,framesPresented,loops,decodedFingerprint,uploadedFingerprint;
    public long decodeNanos,uploadNanos,decoderCpuNanos,uploadBytes,discardedFrames,elapsedNanos;
    public long pixelBufferAllocations,imageBufferAllocations;
    public int pendingFrames,ownedTextures;
    public double decoderFps,presentationFps;
    public long drawCalls;
}
