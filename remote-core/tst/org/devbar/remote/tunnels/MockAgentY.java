package org.devbar.remote.tunnels;

import java.util.ArrayList;
import java.util.List;

public class MockAgentY extends MockAgentBase {
    public static final List<MockAgentY> agents = new ArrayList();

    public MockAgentY() {
        agents.add(this);
    }
}
