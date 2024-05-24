package org.example;

import org.graphstream.graph.Graph;

class Entrance extends Room {
    public Entrance(String name, Graph graph) {
        super(name, graph);
    }

    @Override
    public String getType() {
        return "entrance";
    }
}
