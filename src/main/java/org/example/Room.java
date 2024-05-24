package org.example;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

import java.util.ArrayList;
import java.util.List;

import static org.example.BuildingEvacuation.formatNodeLabel;

public class Room {

    boolean Checked;
    String name;
    int floorlevel;
    Graph graph;
    List<Occupant> occupants;
    boolean isBlocked;
    public boolean notForLeader;

    private int index; // Unique index for each room

    public Room(String name, Graph graph) {
        this.graph=graph;
        this.name = name;
        this.occupants = new ArrayList<>();
    }

    public void setChecked(boolean checked) {
        Checked = checked;
    }

    public boolean isChecked() {
        return Checked;
    }

    public void setNotForLeader(boolean notForLeader) {
        this.notForLeader = notForLeader;
    }

    public boolean isNotForLeader() {
        return notForLeader;
    }

    public void addOccupant(Occupant occupant) {
        occupants.add(occupant);
        updateGraphLabel();
    }

    private void updateGraphLabel() {
        Node node = graph.getNode(name);
        if (node != null) {
            node.setAttribute("ui.label", formatNodeLabel(this));
        }
    }

    public List<Occupant> getOccupants() {
        return occupants;
    }

    public void setBlocked(boolean blocked) {
        this.isBlocked = blocked;
        // Potentially update the graph node here as well
        Node node = graph.getNode(name);
        if (node != null) {
            node.setAttribute("blocked", blocked);
            node.setAttribute("ui.class", "blocked");
        }
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public String toString() {
        return "Room{" +
                "name='" + name + '\'' +
                ", floorlevel=" + floorlevel +
                ", graph=" + graph +
                ", occupants=" + occupants +
                '}';
    }



    public String getName() {
        return name;
    }

    public String getType() {
        return "room";
    }

    public boolean roomChecked;

    public boolean isBlocked() {
        return isBlocked;
    }
}
