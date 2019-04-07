package org.devbar.remote.tunnels;

import org.devbar.remote.agents.AgentFactory;
import org.devbar.remote.agents.Writer;
import org.devbar.remote.utils.Bytes;
import org.mockito.stubbing.Answer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class MockMultiplexSystem {
    protected MultiplexTunnel testTunnelA;
    protected MultiplexTunnel testTunnelB;

    private BytesForTesting ingressBufferA = new BytesForTesting(1024*16);
    private BytesForTesting egressBufferA = new BytesForTesting(1024*16);
    private BytesForTesting ingressBufferB = new BytesForTesting(1024*16);
    private BytesForTesting egressBufferB = new BytesForTesting(1024*16);

    protected void init() throws Exception {
        MockAgentX.agents.clear();
        MockAgentY.agents.clear();

        testTunnelA = new MultiplexTunnel();
        testTunnelB = new MultiplexTunnel();

        // Mock master writer

        Writer masterWriterA = mock(Writer.class);
        Writer masterWriterB = mock(Writer.class);

        testTunnelA.init(null, masterWriterA, true, true);
        testTunnelB.init(null, masterWriterB, false, false);

        doAnswer(invocation -> {
            byte[] buffer = invocation.getArgument(0);
            int start = invocation.getArgument(1);
            int len = invocation.getArgument(2);

            ingressBufferA.copy(buffer, start, len);
            return null;
        }).when(masterWriterA).write(any(byte[].class), any(int.class), any(int.class));

        doAnswer(invocation -> {
            byte[] buffer = invocation.getArgument(0);
            int start = invocation.getArgument(1);
            int len = invocation.getArgument(2);

            ingressBufferB.copy(buffer, start, len);
            return null;
        }).when(masterWriterB).write(any(byte[].class), any(int.class), any(int.class));

        AgentFactory.agentFactory = new AgentFactory();
        AgentFactory.agentFactory.register(MockAgentX.class);
        AgentFactory.agentFactory.register(MockAgentY.class);
    }

    protected boolean quiescent(int step) {
        if (ingressBufferA.empty() && ingressBufferB.empty()) {
            return false;
        }
        while (!ingressBufferA.empty() || !ingressBufferB.empty()) {
            BytesForTesting tmp = ingressBufferA;
            ingressBufferA = egressBufferA;
            egressBufferA = tmp;

            tmp = ingressBufferB;
            ingressBufferB = egressBufferB;
            egressBufferB = tmp;

            ingressBufferA.clear();
            ingressBufferB.clear();

            egressBufferA.begin();
            while (egressBufferA.advance(step)) {
                testTunnelB.consume(egressBufferA);
            }

            egressBufferB.begin();
            while (egressBufferB.advance(step)) {
                testTunnelA.consume(egressBufferB);
            }
        }
        return true;
    }
}
