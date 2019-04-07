package org.devbar.remote.tunnels;

import org.devbar.remote.agents.AgentFactory;
import org.devbar.remote.agents.Writer;
import org.mockito.stubbing.Answer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class MockMultiplexSystem {
    protected MultiplexTunnel testTunnelA;
    protected MultiplexTunnel testTunnelB;

    private byte[] bufferA = new byte[1024*16];
    private byte[] spareBufferA = new byte[1024*16];
    private int headA;
    private byte[] bufferB = new byte[1024*16];
    private byte[] spareBufferB = new byte[1024*16];
    private int headB;

    protected void init() throws Exception {
        MockAgentX.agents.clear();
        MockAgentY.agents.clear();

        testTunnelA = new MultiplexTunnel(0);
        testTunnelB = new MultiplexTunnel(0);

        // Mock master writer

        Writer masterWriterA = mock(Writer.class);
        Writer masterWriterB = mock(Writer.class);

        testTunnelA.init(null, masterWriterA, true, true);
        testTunnelB.init(null, masterWriterB, false, false);

        doAnswer((Answer) invocation -> {
            byte[] buffer = invocation.getArgument(0);
            int start = invocation.getArgument(1);
            int len = invocation.getArgument(2);

            System.arraycopy(buffer, start, bufferA, headA, len);
            headA += len;
            return null;
        }).when(masterWriterA).write(any(byte[].class), any(int.class), any(int.class));

        doAnswer((Answer) invocation -> {
            byte[] buffer = invocation.getArgument(0);
            int start = invocation.getArgument(1);
            int len = invocation.getArgument(2);

            System.arraycopy(buffer, start, bufferB, headB, len);
            headB += len;
            return null;
        }).when(masterWriterB).write(any(byte[].class), any(int.class), any(int.class));

        AgentFactory.agentFactory = new AgentFactory();
        AgentFactory.agentFactory.register(MockAgentX.class);
        AgentFactory.agentFactory.register(MockAgentY.class);
    }

    protected boolean quiescent(int step) throws InterruptedException {
        if (headA == 0 && headB == 0) {
            return false;
        }
        while (headA != 0 || headB != 0) {
            byte[] tmp = bufferA;
            bufferA = spareBufferA;
            spareBufferA = tmp;
            int endA = headA;
            headA = 0;

            tmp = bufferB;
            bufferB = spareBufferB;
            spareBufferB = tmp;
            int endB = headB;
            headB = 0;

            for (int scan = 0; scan < endA; scan += step) {
                int leftover = endA - scan;
                testTunnelB.consume(spareBufferA, scan, step < leftover ? step : leftover);
            }

            for (int scan = 0; scan < endB; scan += step) {
                int leftover = endB - scan;
                testTunnelA.consume(spareBufferB, scan, step < leftover ? step : leftover);
            }
        }
        return true;
    }
}
