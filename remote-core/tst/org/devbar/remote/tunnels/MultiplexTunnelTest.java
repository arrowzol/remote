package org.devbar.remote.tunnels;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MultiplexTunnelTest extends MockMultiplexSystem {

    @Test
    public void test() throws Exception {
        for (int chunkSize = 1; chunkSize < 20; chunkSize++) {
            init();

            MockAgentX agent1X = new MockAgentX();
            testTunnelA.registerAgent(agent1X);

            MockAgentY agent1Y = new MockAgentY();
            testTunnelA.registerAgent(agent1Y);

            quiescent(chunkSize);


            assertEquals(2, MockAgentX.agents.size(), "chunk " + chunkSize);
            assertEquals(2, MockAgentY.agents.size(), "chunk " + chunkSize);

            MockAgentX agent2X = MockAgentX.agents.get(1);
            MockAgentY agent2Y = MockAgentY.agents.get(1);

            assertEquals(1, agent1X.registerWriterCount, "chunk " + chunkSize);
            assertEquals(0, agent1X.closeAgentCount, "chunk " + chunkSize);
            assertEquals(1, agent1X.goCountFirst, "chunk " + chunkSize);
            assertEquals(0, agent1X.goCountSecond, "chunk " + chunkSize);
            assertEquals(0, agent1X.head, "chunk " + chunkSize);

            assertEquals(1, agent2X.registerWriterCount, "chunk " + chunkSize);
            assertEquals(0, agent2X.closeAgentCount, "chunk " + chunkSize);
            assertEquals(0, agent2X.goCountFirst, "chunk " + chunkSize);
            assertEquals(1, agent2X.goCountSecond, "chunk " + chunkSize);
            assertEquals(0, agent2X.head, "chunk " + chunkSize);

            assertEquals(1, agent1Y.registerWriterCount, "chunk " + chunkSize);
            assertEquals(0, agent1Y.closeAgentCount, "chunk " + chunkSize);
            assertEquals(1, agent1Y.goCountFirst, "chunk " + chunkSize);
            assertEquals(0, agent1Y.goCountSecond, "chunk " + chunkSize);
            assertEquals(0, agent1Y.head, "chunk " + chunkSize);

            assertEquals(1, agent2Y.registerWriterCount, "chunk " + chunkSize);
            assertEquals(0, agent2Y.closeAgentCount, "chunk " + chunkSize);
            assertEquals(0, agent2Y.goCountFirst, "chunk " + chunkSize);
            assertEquals(1, agent2Y.goCountSecond, "chunk " + chunkSize);
            assertEquals(0, agent2Y.head, "chunk " + chunkSize);

            String testString1 = "abcdefg";
            String testString2 = "12345";

            agent1X.write(testString1);
            agent1Y.write(testString2);
            agent1X.write(testString2);
            agent1Y.write(testString1);

            agent2X.write(testString1);
            agent2Y.write(testString2);

            quiescent(chunkSize);
            assertEquals(testString1 + testString2, agent2X.read(), "xfer1 chunk " + chunkSize);
            assertEquals(testString2 + testString1, agent2Y.read(), "xfer2 chunk " + chunkSize);
            assertEquals(testString1, agent1X.read(), "xfer3 chunk " + chunkSize);
            assertEquals(testString2, agent1Y.read(), "xfer4 chunk " + chunkSize);
        }
    }
}
