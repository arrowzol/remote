package org.devbar.remote.tunnels;

import java.util.ArrayList;
import java.util.List;

public class MockAgentX extends MockAgentBase {
    public static final List<MockAgentX> agents = new ArrayList();

    public MockAgentX() {
        agents.add(this);
    }
}
