package dev.theredstonee.trsclient.e4mcbridge.relay;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.quic.QuicTokenHandler;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Stateless address validation before allocating QUIC connection state. Keys live only in this process. */
final class RetryTokens implements QuicTokenHandler {
    private final byte[] key=new byte[32];
    RetryTokens() { new SecureRandom().nextBytes(key); }
    public boolean writeToken(ByteBuf out,ByteBuf cid,InetSocketAddress remote) {
        int length=cid.readableBytes(); if(length<1 || length>20)return false;
        long time=System.currentTimeMillis()/1000;
        byte[] connection=new byte[length]; cid.getBytes(cid.readerIndex(),connection);
        out.writeLong(time).writeBytes(signature(remote,time,connection)).writeBytes(connection); return true;
    }
    public int validateToken(ByteBuf token,InetSocketAddress remote) {
        int length=token.readableBytes(); if(length<41 || length>60)return -1;
        long time=token.getLong(token.readerIndex()), now=System.currentTimeMillis()/1000;
        if(time>now+1 || now-time>30)return -1;
        byte[] sig=new byte[32],cid=new byte[length-40];
        token.getBytes(token.readerIndex()+8,sig);token.getBytes(token.readerIndex()+40,cid);
        return MessageDigest.isEqual(sig,signature(remote,time,cid)) ? token.readerIndex()+40 : -1;
    }
    public int maxTokenLength() { return 60; }
    private byte[] signature(InetSocketAddress remote,long time,byte[] cid) {
        try {
            Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key,"HmacSHA256"));
            mac.update(remote.getAddress().getAddress());mac.update(ByteBuffer.allocate(12).putInt(remote.getPort()).putLong(time).array());
            return mac.doFinal(cid);
        } catch(GeneralSecurityException e) { throw new IllegalStateException(e); }
    }
}
