package org.example;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.layout.springbox.implementations.SpringBox;
import org.graphstream.ui.view.Viewer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BuildingEvacuation {
    Graph graph;
    ArrayList<Leader> leaders;
    public ArrayList<Room> allRooms = new ArrayList<>();
    private long seed;

    public BuildingEvacuation(boolean optimal, long seed) {

        System.setProperty("org.graphstream.ui", "swing");
        this.seed = seed;
        graph = new SingleGraph("Niagara university");
        graph.setAttribute("ui.stylesheet", styleSheet());
        createBuildingStructure();

        // Use the SpringBox layout
        Viewer viewer = graph.display();
        SpringBox layout = new SpringBox();
        layout.setForce(1000);
        layout.setQuality(1);
        layout.setStabilizationLimit(1);
        layout.setGravityFactor(5000);
        viewer.enableAutoLayout(layout);

        // Creating an instance of Random with the given seed
        Random random = new Random(seed);

        // Generate a list of notification rooms that are not entrance rooms
        int numNotifications = 3; // Adjust the number of notifications as needed
        List<Room> notificationRooms = new ArrayList<>();
        List<Integer> usedIndexes = new ArrayList<>();

        for (int i = 0; i < numNotifications; i++) {
            int randomNum;
            do {
                randomNum = random.nextInt(allRooms.size());
            } while (isEntranceRoom(allRooms.get(randomNum)) || usedIndexes.contains(randomNum));
            notificationRooms.add(allRooms.get(randomNum));
            usedIndexes.add(randomNum);
        }


        int numLeaders = 5; // Number of leaders
        List<List<Room>> leaderRoomAssignments = new ArrayList<>();
        for (int i = 0; i < numLeaders; i++) {
            leaderRoomAssignments.add(new ArrayList<>());
        }

        // Randomly place leaders in rooms
        int leaderIndex = 0;
        for (int i = 0; i < numLeaders; i++) {
            int leaderRoomIndex;
            do {
                leaderRoomIndex = random.nextInt(allRooms.size());
            } while (usedIndexes.contains(leaderRoomIndex) || isEntranceRoom(allRooms.get(leaderRoomIndex)));
            usedIndexes.add(leaderRoomIndex);

            leaderRoomAssignments.get(leaderIndex).add(allRooms.get(leaderRoomIndex));
            leaderIndex = (leaderIndex + 1) % numLeaders;
        }

        // Assign remaining rooms to leaders
        for (int i = 0; i < allRooms.size(); i++) {
            if (!usedIndexes.contains(i)) {
                leaderRoomAssignments.get(leaderIndex).add(allRooms.get(i));
                leaderIndex = (leaderIndex + 1) % numLeaders;
            }
        }

        initializeLeaders(allRooms, notificationRooms, leaderRoomAssignments);



        blockRoom(getRoomByName("StairsB3"));

        if (optimal) {

            RecipientIdentificationHandler3 recipientIdentificationHandler3 = new RecipientIdentificationHandler3(this, leaders, notificationRooms);
        } else {

            RecipientIdentificationHandler4 recipientIdentificationHandler4 = new RecipientIdentificationHandler4(this, leaders, notificationRooms);
        }
    }


    private boolean isEntranceRoom(Room room) {

        return "entrance".equals(room.getType());
    }

    public void blockRoom(Room room) {
        // Mark the room as blocked
        room.setBlocked(true);

        // Visually highlight the room in red
        Node node = graph.getNode(room.getName());
        if (node != null) {
            node.setAttribute("ui.class", "blocked");
        }


        for (Room otherRoom : allRooms) {
            if (!otherRoom.equals(room)) {
                String edgeId = room.getName() + "-" + otherRoom.getName();
                String reverseEdgeId = otherRoom.getName() + "-" + room.getName();

                // Check if the edge exists in the graph (both directions)
                Edge edge = graph.getEdge(edgeId);
                Edge reverseEdge = graph.getEdge(reverseEdgeId);

                if (edge != null) {
                    edge.setAttribute("weight", 10000);
                    edge.setAttribute("ui.label", "∞");
                 //   edge.setAttribute("ui.style", "fill-color: red;");
                }

                if (reverseEdge != null) {
                    reverseEdge.setAttribute("weight", 10000);
                    reverseEdge.setAttribute("ui.label", "∞");
                 //   reverseEdge.setAttribute("ui.style", "fill-color: red;");
                }
            }
        }

     
        for (Leader leader : leaders) {
            if (leader.getAssignedRooms().remove(room)) {
                System.out.println("Removed blocked room " + room.getName() + " from Leader " + leader.getId() + "'s assignments.");
            }
        }
    }









    public void initializeLeaders(List<Room> rooms, List<Room> notificationRooms, List<List<Room>> leaderRoomAssignments) {
        int numLeaders = leaderRoomAssignments.size(); // Number of leaders

        // Check if enough rooms are provided for leaders
        if (rooms.size() < numLeaders + 1) { // +1 to account for the notification room
            throw new IllegalArgumentException("Not enough rooms provided for leaders");
        }

        // Check if leader room assignments match the number of leaders
        if (leaderRoomAssignments.size() != numLeaders) {
            throw new IllegalArgumentException("Leader room assignments do not match the number of leaders");
        }

        leaders = new ArrayList<>();
        List<Room> availableRooms = new ArrayList<>(rooms);
        availableRooms.remove(notificationRooms); // Exclude the notification room

        // Initialize leaders with their assigned rooms
        for (int i = 0; i < numLeaders; i++) {
            Leader leader = new Leader(i + 1, "evacuation leader", leaderRoomAssignments.get(i).get(0));
            leaders.add(leader);
        }

        // Assign rooms to each leader based on the provided assignments
        for (int i = 0; i < numLeaders; i++) {
            assignRoomsToLeader(leaders.get(i), leaderRoomAssignments.get(i));
        }
    }

    public Room getRoomByName(String name) {
        for (Room room : allRooms) {
            if (room.getName().equals(name)) {
                return room;
            }
        }
        return null; // or throw an exception if preferred
    }

    private void assignRoomsToLeader(Leader leader, List<Room> assignedRooms) {

        ArrayList<Room> roomsWithExit = new ArrayList<>(assignedRooms);




        leader.setAssignedRooms(roomsWithExit);


        System.out.println("Leader " + leader.getId() + " located at: " + leader.getRoomLocation().getName());
        System.out.println("Leader " + leader.getId() + " assigned rooms:");
        for (Room room : roomsWithExit) {
            System.out.println(" - " + room.getName());
        }
    }

    public Room getExitRoom() {
        return allRooms.get(0);
    }

    private void addGraphRoom(Room room) {
        allRooms.add(room);
        Node node = graph.addNode(room.name);
        node.setAttribute("ui.label", formatNodeLabel(room));
        node.setAttribute("ui.class", room.getType());
        if (room.isBlocked) {
            node.setAttribute("ui.class", "blocked");
        }
    }

    public static String formatNodeLabel(Room room) {
        return room.name;
    }

    private void connectRooms(String from, String to, double meter) {

        Edge e = graph.addEdge(from + "-" + to, from, to, false);
        if (e != null) {
            e.setAttribute("weight", meter);
            e.setAttribute("ui.label", String.format("%.1f", meter));
        }
    }

    private String styleSheet() {
        return
                "node {" +
                        "   size: 20px, 20px;" +
                        "   text-size: 12px;" +
                        "   text-style: bold;" +
                        "   text-color: black;" +
                        "   text-alignment: above;" +
                        "   text-background-mode: plain;" +
                        "   text-background-color: white;" +
                        "   text-padding: 3px;" +
                        "   stroke-mode: plain;" +
                        "   stroke-color: black;" +
                        "   stroke-width: 2px;" +
                        "}" +
                        "node.room {" +
                        "   fill-color: #FFD700;" +
                        "   shape: circle;" +
                        "}" +
                        "node.stairs {" +
                        "   fill-color: #1E90FF;" +
                        "   shape: circle;" +
                        "}" +
                        "node.entrance {" +
                        "   fill-color: #32CD32;" +
                        "   shape: circle;" +
                        "}" +
                        "node.exit {" +
                        "   fill-color: #FF4500;" +
                        "   shape: circle;" +
                        "}" +
                        "node.blocked {" +
                        "   fill-color: red;" +  // Blocked nodes will be red
                        "   shape: circle;" +
                        "}" +
                        "edge {" +
                        "   fill-color: #696969;" +
                        "   size: 0.5px;" +
                        "   arrow-size: 2px, 2px;" +
                        "   shape: cubic-curve;" +
                        "}" +
                        "edge.highlighted {" +
                        "   fill-color: #FFA500;" +  // Highlighted edges will be orange
                        "   size: 4px;" +  // Thicker highlighted edges
                        "}";
    }



    private void createBuildingStructure() {
        createRooms();
        // Floor 0 setup
        // Entrance A connections
        connectRooms("EntranceA0E20", "StairsA", 32);
        connectRooms("EntranceA0E20", "Infodesk", 24);
        connectRooms("EntranceA0E20", "C0E03", 31);
        connectRooms("EntranceA0E20", "C0E42", 41);
        connectRooms("EntranceA0E20", "C0E11", 43);
        connectRooms("EntranceA0E20", "C0E27", 30);
        connectRooms("EntranceA0E20", "B0E02", 38);
        connectRooms("EntranceA0E20", "B0E07", 57);
        connectRooms("EntranceA0E20", "B0E15", 51);
        connectRooms("EntranceA0E20", "B0E22", 45);

        // StairsA connections
        connectRooms("StairsA", "Infodesk", 47);
        connectRooms("StairsA", "C0E03", 56);
        connectRooms("StairsA", "C0E42", 65);
        connectRooms("StairsA", "C0E11", 64);
        connectRooms("StairsA", "C0E27", 44);
        connectRooms("StairsA", "B0E02", 32);
        connectRooms("StairsA", "B0E07", 49);
        connectRooms("StairsA", "B0E15", 42);
        connectRooms("StairsA", "B0E22", 36);
        connectRooms("StairsA", "StairsA1", 2);

        // Floor 1 connections
        connectRooms("StairsA1", "C1E03", 56);
        connectRooms("StairsA1", "C1E42", 65);
        connectRooms("StairsA1", "C1E11", 64);
        connectRooms("StairsA1", "C1E27", 44);
        connectRooms("StairsA1", "B1E02", 32);
        connectRooms("StairsA1", "B1E07", 49);
        connectRooms("StairsA1", "B1E15", 42);
        connectRooms("StairsA1", "B1E22", 36);
        connectRooms("StairsA1", "StairsA2", 2);

        // Floor 2 connections
        connectRooms("StairsA2", "C2E03", 56);
        connectRooms("StairsA2", "C2E42", 65);
        connectRooms("StairsA2", "C2E11", 64);
        connectRooms("StairsA2", "C2E27", 44);
        connectRooms("StairsA2", "B2E02", 32);
        connectRooms("StairsA2", "B2E07", 49);
        connectRooms("StairsA2", "B2E15", 42);
        connectRooms("StairsA2", "B2E22", 36);
        connectRooms("StairsA2", "StairsA3", 2);

        // Floor 3 connections
        connectRooms("StairsA3", "C3E03", 56);
        connectRooms("StairsA3", "C3E42", 65);
        connectRooms("StairsA3", "C3E11", 64);
        connectRooms("StairsA3", "C3E27", 44);
        connectRooms("StairsA3", "B3E02", 32);
        connectRooms("StairsA3", "B3E07", 49);
        connectRooms("StairsA3", "B3E15", 42);
        connectRooms("StairsA3", "B3E22", 36);
        connectRooms("StairsA3", "StairsA4", 2);

        // Floor 4 connections
        connectRooms("StairsA4", "C4E03", 56);
        connectRooms("StairsA4", "C4E42", 65);
        connectRooms("StairsA4", "C4E11", 64);
        connectRooms("StairsA4", "C4E27", 44);
        connectRooms("StairsA4", "B4E02", 32);
        connectRooms("StairsA4", "B4E07", 49);
        connectRooms("StairsA4", "B4E15", 42);
        connectRooms("StairsA4", "B4E22", 36);
        connectRooms("StairsA4", "StairsA5", 2);

        // Floor 5 connections
        connectRooms("StairsA5", "C5E03", 56);
        connectRooms("StairsA5", "C5E42", 65);
        connectRooms("StairsA5", "C5E11", 64);
        connectRooms("StairsA5", "C5E27", 44);
        connectRooms("StairsA5", "B5E02", 32);
        connectRooms("StairsA5", "B5E07", 49);
        connectRooms("StairsA5", "B5E15", 42);
        connectRooms("StairsA5", "B5E22", 36);

        // Floor 0 connections
        connectRooms("StairsB", "EntranceA0E20", 37);
        connectRooms("StairsB", "StairsA", 61);
        connectRooms("StairsB", "Infodesk", 23);
        connectRooms("StairsB", "C0E03", 21);
        connectRooms("StairsB", "C0E42", 13);
        connectRooms("StairsB", "C0E11", 22);
        connectRooms("StairsB", "C0E27", 27);
        connectRooms("StairsB", "B0E02", 43);
        connectRooms("StairsB", "B0E07", 59);
        connectRooms("StairsB", "B0E15", 68);
        connectRooms("StairsB", "B0E22", 62);
        connectRooms("StairsB", "StairsB1", 2);

        // Floor 1 connections
        connectRooms("StairsB1", "C1E03", 21);
        connectRooms("StairsB1", "C1E42", 13);
        connectRooms("StairsB1", "C1E11", 22);
        connectRooms("StairsB1", "C1E27", 27);
        connectRooms("StairsB1", "B1E02", 43);
        connectRooms("StairsB1", "B1E07", 59);
        connectRooms("StairsB1", "B1E15", 68);
        connectRooms("StairsB1", "B1E22", 62);
        connectRooms("StairsB1", "StairsB2", 2);

        // Floor 2 connections
        connectRooms("StairsB2", "C2E03", 21);
        connectRooms("StairsB2", "C2E42", 13);
        connectRooms("StairsB2", "C2E11", 22);
        connectRooms("StairsB2", "C2E27", 27);
        connectRooms("StairsB2", "B2E02", 43);
        connectRooms("StairsB2", "B2E07", 59);
        connectRooms("StairsB2", "B2E15", 68);
        connectRooms("StairsB2", "B2E22", 62);
        connectRooms("StairsB2", "StairsB3", 2);

        // Floor 3 connections
        connectRooms("StairsB3", "C3E03", 21);
        connectRooms("StairsB3", "C3E42", 13);
        connectRooms("StairsB3", "C3E11", 22);
        connectRooms("StairsB3", "C3E27", 27);
        connectRooms("StairsB3", "B3E02", 43);
        connectRooms("StairsB3", "B3E07", 59);
        connectRooms("StairsB3", "B3E15", 68);
        connectRooms("StairsB3", "B3E22", 62);
        connectRooms("StairsB3", "StairsB4", 2);

        // Floor 4 connections
        connectRooms("StairsB4", "C4E03", 21);
        connectRooms("StairsB4", "C4E42", 13);
        connectRooms("StairsB4", "C4E11", 22);
        connectRooms("StairsB4", "C4E27", 27);
        connectRooms("StairsB4", "B4E02", 43);
        connectRooms("StairsB4", "B4E07", 59);
        connectRooms("StairsB4", "B4E15", 68);
        connectRooms("StairsB4", "B4E22", 62);
        connectRooms("StairsB4", "StairsB5", 2);

        // Floor 5 connections
        connectRooms("StairsB5", "C5E03", 21);
        connectRooms("StairsB5", "C5E42", 13);
        connectRooms("StairsB5", "C5E11", 22);
        connectRooms("StairsB5", "C5E27", 27);
        connectRooms("StairsB5", "B5E02", 43);
        connectRooms("StairsB5", "B5E07", 59);
        connectRooms("StairsB5", "B5E15", 68);
        connectRooms("StairsB5", "B5E22", 62);
    }

    public void createRooms() {
        // Rooms for Floor 0
        Entrance entranceA = new Entrance("EntranceA0E20", graph);
        Room infodesk = new Room("Infodesk", graph);
        Room roomC0E03 = new Room("C0E03", graph);
        Room roomC0E042 = new Room("C0E42", graph);
        Room roomC0E011 = new Room("C0E11", graph);
        Room roomC0E027 = new Room("C0E27", graph);
        Room roomB0E02 = new Room("B0E02", graph);
        Room roomB0E07 = new Room("B0E07", graph);
        Room roomB0E15 = new Room("B0E15", graph);
        Room roomB0E22 = new Room("B0E22", graph);

        // Stairs
        Stairs stairsA = new Stairs("StairsA", graph);
        Stairs stairsA1 = new Stairs("StairsA1", graph);
        Stairs stairsA2 = new Stairs("StairsA2", graph);
        Stairs stairsA3 = new Stairs("StairsA3", graph);
        Stairs stairsA4 = new Stairs("StairsA4", graph);
        Stairs stairsA5 = new Stairs("StairsA5", graph);
        Stairs stairsB = new Stairs("StairsB", graph);
        Stairs stairsB1 = new Stairs("StairsB1", graph);
        Stairs stairsB2 = new Stairs("StairsB2", graph);
        Stairs stairsB3 = new Stairs("StairsB3", graph);
        Stairs stairsB4 = new Stairs("StairsB4", graph);
        Stairs stairsB5 = new Stairs("StairsB5", graph);

        // Rooms for Floor 1
        Room roomC1E03 = new Room("C1E03", graph);
        Room roomC1E042 = new Room("C1E42", graph);
        Room roomC1E011 = new Room("C1E11", graph);
        Room roomC1E027 = new Room("C1E27", graph);
        Room roomB1E02 = new Room("B1E02", graph);
        Room roomB1E07 = new Room("B1E07", graph);
        Room roomB1E15 = new Room("B1E15", graph);
        Room roomB1E22 = new Room("B1E22", graph);

        // Rooms for Floor 2
        Room roomC2E03 = new Room("C2E03", graph);
        Room roomC2E042 = new Room("C2E42", graph);
        Room roomC2E011 = new Room("C2E11", graph);
        Room roomC2E027 = new Room("C2E27", graph);
        Room roomB2E02 = new Room("B2E02", graph);
        Room roomB2E07 = new Room("B2E07", graph);
        Room roomB2E15 = new Room("B2E15", graph);
        Room roomB2E22 = new Room("B2E22", graph);

        // Rooms for Floor 3
        Room roomC3E03 = new Room("C3E03", graph);
        Room roomC3E042 = new Room("C3E42", graph);
        Room roomC3E011 = new Room("C3E11", graph);
        Room roomC3E027 = new Room("C3E27", graph);
        Room roomB3E02 = new Room("B3E02", graph);
        Room roomB3E07 = new Room("B3E07", graph);
        Room roomB3E15 = new Room("B3E15", graph);
        Room roomB3E22 = new Room("B3E22", graph);

        // Rooms for Floor 4
        Room roomC4E03 = new Room("C4E03", graph);
        Room roomC4E042 = new Room("C4E42", graph);
        Room roomC4E011 = new Room("C4E11", graph);
        Room roomC4E027 = new Room("C4E27", graph);
        Room roomB4E02 = new Room("B4E02", graph);
        Room roomB4E07 = new Room("B4E07", graph);
        Room roomB4E15 = new Room("B4E15", graph);
        Room roomB4E22 = new Room("B4E22", graph);

        // Rooms for Floor 5
        Room roomC5E03 = new Room("C5E03", graph);
        Room roomC5E042 = new Room("C5E42", graph);
        Room roomC5E011 = new Room("C5E11", graph);
        Room roomC5E027 = new Room("C5E27", graph);
        Room roomB5E02 = new Room("B5E02", graph);
        Room roomB5E07 = new Room("B5E07", graph);
        Room roomB5E15 = new Room("B5E15", graph);
        Room roomB5E22 = new Room("B5E22", graph);

        // Add all rooms to the graph
        addGraphRoom(entranceA);
        addGraphRoom(infodesk);
        addGraphRoom(roomC0E03);
        addGraphRoom(roomC0E042);
        addGraphRoom(roomC0E011);
        addGraphRoom(roomC0E027);
        addGraphRoom(roomB0E02);
        addGraphRoom(roomB0E07);
        addGraphRoom(roomB0E15);
        addGraphRoom(roomB0E22);
        addGraphRoom(stairsA);
        addGraphRoom(stairsA1);
        addGraphRoom(stairsA2);
        addGraphRoom(stairsA3);
        addGraphRoom(stairsA4);
        addGraphRoom(stairsA5);
        addGraphRoom(stairsB);
        addGraphRoom(stairsB1);
        addGraphRoom(stairsB2);
        addGraphRoom(stairsB3);
        addGraphRoom(stairsB4);
        addGraphRoom(stairsB5);
        addGraphRoom(roomC1E03);
        addGraphRoom(roomC1E042);
        addGraphRoom(roomC1E011);
        addGraphRoom(roomC1E027);
        addGraphRoom(roomB1E02);
        addGraphRoom(roomB1E07);
        addGraphRoom(roomB1E15);
        addGraphRoom(roomB1E22);
        addGraphRoom(roomC2E03);
        addGraphRoom(roomC2E042);
        addGraphRoom(roomC2E011);
        addGraphRoom(roomC2E027);
        addGraphRoom(roomB2E02);
        addGraphRoom(roomB2E07);
        addGraphRoom(roomB2E15);
        addGraphRoom(roomB2E22);
        addGraphRoom(roomC3E03);
        addGraphRoom(roomC3E042);
        addGraphRoom(roomC3E011);
        addGraphRoom(roomC3E027);
        addGraphRoom(roomB3E02);
        addGraphRoom(roomB3E07);
        addGraphRoom(roomB3E15);
        addGraphRoom(roomB3E22);
        addGraphRoom(roomC4E03);
        addGraphRoom(roomC4E042);
        addGraphRoom(roomC4E011);
        addGraphRoom(roomC4E027);
        addGraphRoom(roomB4E02);
        addGraphRoom(roomB4E07);
        addGraphRoom(roomB4E15);
        addGraphRoom(roomB4E22);
        addGraphRoom(roomC5E03);
        addGraphRoom(roomC5E042);
        addGraphRoom(roomC5E011);
        addGraphRoom(roomC5E027);
        addGraphRoom(roomB5E02);
        addGraphRoom(roomB5E07);
        addGraphRoom(roomB5E15);
        addGraphRoom(roomB5E22);

        for (int i = 0; i < allRooms.size(); i++) {
            allRooms.get(i).setIndex(i);
        }
    }
}
