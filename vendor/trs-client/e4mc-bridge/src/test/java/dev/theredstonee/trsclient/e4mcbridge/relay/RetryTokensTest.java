package dev.theredstonee.trsclient.e4mcbridge.relay;

import io.netty.buffer.*;
import java.net.InetSocketAddress;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RetryTokensTest {
    @Test void retryProofBindsAddressPortConnectionIdAndIssueTime() {
        RetryTokens tokens=new RetryTokens();ByteBuf cid=Unpooled.wrappedBuffer(new byte[]{1,2,3,4}),proof=Unpooled.buffer();
        try {
            InetSocketAddress origin=new InetSocketAddress("127.0.0.1",12345);
            assertTrue(tokens.writeToken(proof,cid,origin));assertEquals(40,tokens.validateToken(proof,origin));
            assertEquals(-1,tokens.validateToken(proof,new InetSocketAddress("127.0.0.1",12346)));
            assertEquals(-1,tokens.validateToken(proof,new InetSocketAddress("127.0.0.2",12345)));
            proof.setByte(proof.writerIndex()-1,99);assertEquals(-1,tokens.validateToken(proof,origin));
            proof.setByte(proof.writerIndex()-1,4);proof.setLong(0,System.currentTimeMillis()/1000-31);
            assertEquals(-1,tokens.validateToken(proof,origin));
            assertTrue(proof.readableBytes()<=tokens.maxTokenLength());
        } finally {cid.release();proof.release();}
    }
}
