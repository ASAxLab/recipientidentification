package org.example;

import org.graphstream.graph.Graph;

public class Exit extends Room{
    public Exit(String name, int floor, Graph graph) {
        super(name, graph);
    }

    @Override
    public String getType() {
        return "exit";
    }
}
