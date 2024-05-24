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
    RecipientIdentificationHandler recipientIdentificationHandler;
    public ArrayList<Room> allRooms = new ArrayList<>(); // Assuming this is populated elsewhere in your code
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
        layout.setForce(1000);            // Default is 1.0, increase to spread nodes further apart
        layout.setQuality(1);             // Default is 1.0, lower values speed up stabilization
        layout.setStabilizationLimit(1);  // Default is 1, increase for longer stabilization
        layout.setGravityFactor(5000);    // Default is 0.1, increase to pull nodes toward center
        viewer.enableAutoLayout(layout);

        // Creating an instance of Random with the given seed
        Random random = new Random(seed);

        // Select a random notification room that is not an entrance room
        int randomNum;
        do {
            randomNum = random.nextInt(allRooms.size());
        } while (isEntranceRoom(allRooms.get(randomNum)));
        Room notificationRoom = allRooms.get(randomNum);

        // Programmatically creating leader room assignments
        int numLeaders = 5; // Number of leaders
        List<List<Room>> leaderRoomAssignments = new ArrayList<>();
        for (int i = 0; i < numLeaders; i++) {
            leaderRoomAssignments.add(new ArrayList<>());
        }

        // Randomly place leaders in rooms
        int leaderIndex = 0;
        List<Integer> usedIndexes = new ArrayList<>();
        usedIndexes.add(randomNum); // Notification room index
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

        initializeLeaders(allRooms, notificationRoom, leaderRoomAssignments);

        if(optimal) {
            RecipientIdentificationHandler3 recipientIdentificationHandler3 = new RecipientIdentificationHandler3(this, leaders, notificationRoom);
        } else {
            RecipientIdentificationHandler4 recipientIdentificationHandler4 = new RecipientIdentificationHandler4(this, leaders, notificationRoom);
        }
    }

    private boolean isEntranceRoom(Room room) {
        // Implement logic to determine if a room is an entrance room
        return "entrance".equals(room.getType());
    }

    public void initializeLeaders(List<Room> rooms, Room notificationRoom, List<List<Room>> leaderRoomAssignments) {
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
        availableRooms.remove(notificationRoom); // Exclude the notification room

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
        // Make a copy of the assigned rooms list
        ArrayList<Room> roomsWithExit = new ArrayList<>(assignedRooms);

        // Get the exit room

        // Assign the rooms to the leader
        leader.setAssignedRooms(roomsWithExit); // Assuming Leader has a setAssignedRooms method

        // Print out the leader's location and the rooms assigned to the leader
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
        node.setAttribute("ui.label", formatNodeLabel(room));  // Set label to room name only
        node.setAttribute("ui.class", room.getType());  // Set the class for styling based on room type
        if (room.isBlocked) {
            node.setAttribute("ui.class", "blocked");
        }
    }

    public static String formatNodeLabel(Room room) {
        return room.name;  // Just return the room name for the node label
    }

    private void connectRooms(String from, String to, double meter) {
        // Add edge from 'from' to 'to'
        Edge e = graph.addEdge(from + "-" + to, from, to, false);
        if (e != null) {
            e.setAttribute("weight", meter);
            e.setAttribute("ui.label", String.format("%.1f", meter));
        }
    }

    private String styleSheet() {
        return
                "node {" +
                        "   size: 20px, 20px;" + // Reduced size
                        "   text-size: 12px;" +  // Reduced text size
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
                        "   fill-color: #FFD700;" +  // Gold color
                        "   shape: circle;" +
                        "}" +
                        "node.stairs {" +
                        "   fill-color: #1E90FF;" +  // DodgerBlue color
                        "   shape: circle;" +
                        "}" +
                        "node.entrance {" +
                        "   fill-color: #32CD32;" +  // LimeGreen color
                        "   shape: circle;" +
                        "}" +
                        "node.exit {" +
                        "   fill-color: #FF4500;" +  // OrangeRed color
                        "   shape: circle;" +
                        "}" +
                        "node.blocked {" +
                        "   fill-color: #A9A9A9;" +  // DarkGray color
                        "   shape: circle;" +
                        "}" +
                        "edge {" +
                        "   fill-color: #696969;" +  // DimGray color
                        "   size: 0.5px;" + // Reduced edge size
                        "   arrow-size: 2px, 2px;" + // Reduced arrow size
                        "   shape: cubic-curve;" + // Use cubic-curve for smoother edges
                        "}" +
                        "edge.highlighted {" +
                        "   fill-color: #FFA500;" +  // Orange color
                        "   size: 4px;" + // Thicker highlighted edges
                        "}";
    }

    private void createBuildingStructure() {
        createRooms();
        // Floor 0 setup
        // entrance A connections
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

        // stairsA connections
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
        connectRooms("StairsA1", "StairsA2", 2);

        // floor 2 connections
        connectRooms("StairsA1", "C1E03", 56);
        connectRooms("StairsA1", "C1E42", 65);
        connectRooms("StairsA1", "C1E11", 64);
        connectRooms("StairsA1", "C1E27", 44);
        connectRooms("StairsA1", "B1E02", 32);
        connectRooms("StairsA1", "B1E07", 49);
        connectRooms("StairsA1", "B1E15", 42);
        connectRooms("StairsA1", "B1E22", 36);

        connectRooms("StairsB1", "C1E03", 21);
        connectRooms("StairsB1", "C1E42", 13);
        connectRooms("StairsB1", "C1E11", 22);
        connectRooms("StairsB1", "C1E27", 27);
        connectRooms("StairsB1", "B1E02", 43);
        connectRooms("StairsB1", "B1E07", 59);
        connectRooms("StairsB1", "B1E15", 68);
        connectRooms("StairsB1", "B1E22", 62);

        connectRooms("C1E03", "C1E42", 27);
        connectRooms("C1E03", "C1E11", 38);
        connectRooms("C1E03", "C1E27", 31);
        connectRooms("C1E03", "B1E02", 41);
        connectRooms("C1E03", "B1E07", 56);
        connectRooms("C1E03", "B1E15", 65);
        connectRooms("C1E03", "B1E22", 61);

        connectRooms("C1E42", "B1E22", 64);
        connectRooms("C1E42", "B1E15", 67);
        connectRooms("C1E42", "B1E07", 60);
        connectRooms("C1E42", "B1E02", 44);
        connectRooms("C1E42", "C1E27", 28);
        connectRooms("C1E42", "C1E11", 24);

        connectRooms("C1E11", "C1E27", 28);
        connectRooms("C1E11", "B1E02", 46);
        connectRooms("C1E11", "B1E07", 62);
        connectRooms("C1E11", "B1E15", 69);
        connectRooms("C1E11", "B1E22", 66);

        connectRooms("C1E27", "B1E02", 26);
        connectRooms("C1E27", "B1E07", 41);
        connectRooms("C1E27", "B1E15", 47);
        connectRooms("C1E27", "B1E22", 45);

        connectRooms("B1E02", "B1E07", 26);
        connectRooms("B1E02", "B1E15", 33);
        connectRooms("B1E02", "B1E22", 31);

        connectRooms("B1E07", "B1E22", 30);
        connectRooms("B1E07", "B1E15", 17);
        connectRooms("B1E15", "B1E22", 16);

        // Floor 3 connections
        connectRooms("StairsA2", "C2E03", 56);
        connectRooms("StairsA2", "C2E42", 65);
        connectRooms("StairsA2", "C2E11", 64);
        connectRooms("StairsA2", "C2E27", 44);
        connectRooms("StairsA2", "B2E02", 32);
        connectRooms("StairsA2", "B2E07", 49);
        connectRooms("StairsA2", "B2E15", 42);
        connectRooms("StairsA2", "B2E22", 36);

        connectRooms("StairsB2", "C2E03", 21);
        connectRooms("StairsB2", "C2E42", 13);
        connectRooms("StairsB2", "C2E11", 22);
        connectRooms("StairsB2", "C2E27", 27);
        connectRooms("StairsB2", "B2E02", 43);
        connectRooms("StairsB2", "B2E07", 59);
        connectRooms("StairsB2", "B2E15", 68);
        connectRooms("StairsB2", "B2E22", 62);

        connectRooms("C2E03", "C2E42", 27);
        connectRooms("C2E03", "C2E11", 38);
        connectRooms("C2E03", "C2E27", 31);
        connectRooms("C2E03", "B2E02", 41);
        connectRooms("C2E03", "B2E07", 56);
        connectRooms("C2E03", "B2E15", 65);
        connectRooms("C2E03", "B2E22", 61);

        connectRooms("C2E42", "B2E22", 64);
        connectRooms("C2E42", "B2E15", 67);
        connectRooms("C2E42", "B2E07", 60);
        connectRooms("C2E42", "B2E02", 44);
        connectRooms("C2E42", "C2E27", 28);
        connectRooms("C2E42", "C2E11", 24);

        connectRooms("C2E11", "C2E27", 28);
        connectRooms("C2E11", "B2E02", 46);
        connectRooms("C2E11", "B2E07", 62);
        connectRooms("C2E11", "B2E15", 69);
        connectRooms("C2E11", "B2E22", 66);

        connectRooms("C2E27", "B2E02", 26);
        connectRooms("C2E27", "B2E07", 41);
        connectRooms("C2E27", "B2E15", 47);
        connectRooms("C2E27", "B2E22", 45);

        connectRooms("B2E02", "B2E07", 26);
        connectRooms("B2E02", "B2E15", 33);
        connectRooms("B2E02", "B2E22", 31);
        connectRooms("B2E07", "B2E22", 30);
        connectRooms("B2E07", "B2E15", 17);
        connectRooms("B2E15", "B2E22", 16);

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
        connectRooms("StairsB1", "StairsB2", 2);

        connectRooms("Infodesk", "C0E03", 17);
        connectRooms("Infodesk", "C0E42", 31);
        connectRooms("Infodesk", "C0E11", 38);
        connectRooms("Infodesk", "C0E27", 24);

        connectRooms("Infodesk", "B0E02", 33);
        connectRooms("Infodesk", "B0E07", 49);
        connectRooms("Infodesk", "B0E15", 56);
        connectRooms("Infodesk", "B0E22", 54);

        connectRooms("C0E03", "C0E42", 27);
        connectRooms("C0E03", "C0E11", 38);
        connectRooms("C0E03", "C0E27", 31);

        connectRooms("C0E03", "B0E02", 41);
        connectRooms("C0E03", "B0E07", 56);
        connectRooms("C0E03", "B0E15", 65);
        connectRooms("C0E03", "B0E22", 61);

        connectRooms("C0E42", "B0E22", 64);
        connectRooms("C0E42", "B0E15", 67);
        connectRooms("C0E42", "B0E07", 60);
        connectRooms("C0E42", "B0E02", 44);

        connectRooms("C0E42", "C0E27", 28);
        connectRooms("C0E42", "C0E11", 24);

        connectRooms("C0E11", "C0E27", 28);

        connectRooms("C0E11", "B0E02", 46);
        connectRooms("C0E11", "B0E07", 62);
        connectRooms("C0E11", "B0E15", 69);
        connectRooms("C0E11", "B0E22", 66);

        connectRooms("C0E27", "B0E02", 26);
        connectRooms("C0E27", "B0E07", 41);
        connectRooms("C0E27", "B0E15", 47);
        connectRooms("C0E27", "B0E22", 45);

        connectRooms("B0E02", "B0E07", 26);
        connectRooms("B0E02", "B0E15", 33);
        connectRooms("B0E02", "B0E22", 31);
        connectRooms("B0E07", "B0E22", 30);
        connectRooms("B0E07", "B0E15", 17);
        connectRooms("B0E15", "B0E22", 16);

        // Disable edges for blocked stairs
        for (Room room : allRooms) {
            // disableEdges(room.name);
        }
    }

    public void createRooms() {
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

        Stairs stairsA = new Stairs("StairsA", graph);
        Stairs stairsA1 = new Stairs("StairsA1", graph);
        Stairs stairsA2 = new Stairs("StairsA2", graph);
        Stairs stairsB = new Stairs("StairsB", graph);
        Stairs stairsB1 = new Stairs("StairsB1", graph);
        Stairs stairsB2 = new Stairs("StairsB2", graph);

        Room roomC1E03 = new Room("C1E03", graph);
        Room roomC1E042 = new Room("C1E42", graph);
        Room roomC1E011 = new Room("C1E11", graph);
        Room roomC1E027 = new Room("C1E27", graph);
        Room roomB1E02 = new Room("B1E02", graph);
        Room roomB1E07 = new Room("B1E07", graph);
        Room roomB1E15 = new Room("B1E15", graph);
        Room roomB1E22 = new Room("B1E22", graph);

        Room roomC2E03 = new Room("C2E03", graph);
        Room roomC2E042 = new Room("C2E42", graph);
        Room roomC2E011 = new Room("C2E11", graph);
        Room roomC2E027 = new Room("C2E27", graph);
        Room roomB2E02 = new Room("B2E02", graph);
        Room roomB2E07 = new Room("B2E07", graph);
        Room roomB2E15 = new Room("B2E15", graph);
        Room roomB2E22 = new Room("B2E22", graph);

        addGraphRoom(entranceA); // 0
        addGraphRoom(infodesk); // 1
        addGraphRoom(roomC0E03); // 2
        addGraphRoom(roomC0E042); // 3
        addGraphRoom(roomC0E011); // 4
        addGraphRoom(roomC0E027); // 5
        addGraphRoom(roomB0E02); // 6
        addGraphRoom(roomB0E07); // 7
        addGraphRoom(roomB0E15); // 8
        addGraphRoom(roomB0E22); // 9
        addGraphRoom(stairsA); // 10
        addGraphRoom(stairsA1); // 11
        addGraphRoom(stairsA2); // 12
        addGraphRoom(stairsB); // 13
        addGraphRoom(stairsB1); // 14
        addGraphRoom(stairsB2); // 15
        addGraphRoom(roomC1E03); // 16
        addGraphRoom(roomC1E042); // 17
        addGraphRoom(roomC1E011); // 18
        addGraphRoom(roomC1E027); // 19
        addGraphRoom(roomB1E02); // 20
        addGraphRoom(roomB1E07); // 21
        addGraphRoom(roomB1E15); // 22
        addGraphRoom(roomB1E22); // 23
        addGraphRoom(roomC2E03); // 24
        addGraphRoom(roomC2E042); // 25
        addGraphRoom(roomC2E011); // 26
        addGraphRoom(roomC2E027); // 27
        addGraphRoom(roomB2E02); // 28
        addGraphRoom(roomB2E07); // 29
        addGraphRoom(roomB2E15); // 30
        addGraphRoom(roomB2E22); // 31

        for (int i = 0; i < allRooms.size(); i++) {
            allRooms.get(i).setIndex(i);
        }
    }
}
