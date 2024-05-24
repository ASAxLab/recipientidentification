package org.example;

import org.graphstream.graph.Graph;

class Stairs extends Room {
    public Stairs(String name, Graph graph) {
        super(name, graph);
    }

    @Override
    public String getType() {
        return "stairs";
    }
}