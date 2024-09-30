package oldfiles;

import org.example.BuildingEvacuation;
import org.example.Leader;
import org.example.Room;
import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecipientIdentificationHandler {

    private final BuildingEvacuation buildingEvacuation;
    private final List<Leader> leaders;
    private static final double LEADER_SPEED = 1.5; // meters per second
    private final Room notificationRoom;
    private final Graph graph;
    private final Dijkstra dijkstra;

    public RecipientIdentificationHandler(BuildingEvacuation buildingEvacuation, List<Leader> leaders, Room notificationRoom) {
        this.buildingEvacuation = buildingEvacuation;
        this.leaders = deepCopyLeaders(leaders);
        this.notificationRoom = notificationRoom;
        this.graph = buildingEvacuation.graph;
        this.dijkstra = new Dijkstra(Dijkstra.Element.EDGE, null, "weight");
        dijkstra.init(graph);
        Room exitRoom = buildingEvacuation.getExitRoom();

        System.out.println("Room for notification: " + notificationRoom.name);
        printAllLeaderPathsAndWorkloads(notificationRoom, exitRoom);
        findClosestLeader(notificationRoom);
    }

    private List<Leader> deepCopyLeaders(List<Leader> originalLeaders) {
        List<Leader> copiedLeaders = new ArrayList<>();
        for (Leader leader : originalLeaders) {
            Leader newLeader = new Leader(leader.getId(), "evacuation leader", leader.getRoomLocation());
            newLeader.setAssignedRooms(new ArrayList<>(leader.getAssignedRooms()));
            copiedLeaders.add(newLeader);
        }
        return copiedLeaders;
    }

    private void printAllLeaderPathsAndWorkloads(Room notificationRoom, Room exitRoom) {
        double minimumTotalWorkload = Double.MAX_VALUE;
        Leader optimalLeaderForNotification = null;
        List<Path> optimalPathsForNotificationLeader = new ArrayList<>();

        long startTime = System.nanoTime(); // Start timing

        for (Leader notifyingLeader : leaders) {
            double totalWorkloadForAllLeaders = calculateTotalWorkloadForAllLeaders(notifyingLeader, notificationRoom, exitRoom);
            if (totalWorkloadForAllLeaders < minimumTotalWorkload) {
                minimumTotalWorkload = totalWorkloadForAllLeaders;
                optimalLeaderForNotification = notifyingLeader;
                optimalPathsForNotificationLeader = calculatePaths(notifyingLeader, notificationRoom, exitRoom);
            }
        }

        long endTime = System.nanoTime(); // End timing
        double computationTimeInMilliseconds = (endTime - startTime) / 1_000_000.0;
        System.out.println("Computation time for finding the optimal leader: " + computationTimeInMilliseconds + " ms");

        // Highlight the optimal paths for the leader chosen for notification
        if (optimalLeaderForNotification != null) {
            System.out.println("Optimal Leader for Notification: Leader " + optimalLeaderForNotification.getId());
            System.out.println("Total workload: " + minimumTotalWorkload + " seconds");
            highlightPaths(optimalPathsForNotificationLeader);
        }
    }

    private double calculateTotalWorkloadForAllLeaders(Leader notifyingLeader, Room notificationRoom, Room exitRoom) {
        double totalWorkloadForAllLeaders = 0.0;

        for (Leader leader : leaders) {
            List<Path> paths = leader.equals(notifyingLeader)
                    ? calculatePaths(leader, notificationRoom, exitRoom)
                    : calculatePathsWithoutNotification(leader, exitRoom);

            double totalWorkload = calculateTotalWorkload(paths);
            totalWorkloadForAllLeaders += totalWorkload;

            printPaths(paths, leader.getId(), totalWorkload);
        }

        System.out.println("Total workload for all leaders combined: " + totalWorkloadForAllLeaders + " seconds");
        System.out.println("=======================================================\n");
        return totalWorkloadForAllLeaders;
    }

    private void printPaths(List<Path> paths, int leaderId, double totalWorkload) {
        System.out.println("Leader " + leaderId + ":");
        for (Path path : paths) {
            System.out.print("Path: ");
            for (Node node : path.getNodePath()) {
                System.out.print(node.getId() + " ");
            }
            System.out.println(" | Time to travel: " + calculateTimeForPath(path) + " seconds");
        }
        System.out.println("Total workload: " + totalWorkload + " seconds\n");
        for (int i = 0; i < leaders.size(); i++) {
            if(leaders.get(i).id==leaderId){
                leaders.get(i).setWorkLoad(totalWorkload);
            }
        }
    }

    public void findClosestLeader(Room notificationRoom) {
        long startTime = System.nanoTime(); // Start timing

        Leader closestLeader = null;
        double shortestDistance = Double.MAX_VALUE;

        Node notificationNode = graph.getNode(notificationRoom.getName());

        for (Leader leader : leaders) {
            Node leaderNode = graph.getNode(leader.getRoomLocation().getName());
            if (leaderNode == null || notificationNode == null) {
                continue;
            }
            double distance = calculateShortestDistance(leaderNode, notificationNode);
            if (distance < shortestDistance) {
                shortestDistance = distance;
                closestLeader = leader;
            }
        }

        long endTime = System.nanoTime(); // End timing
        double computationTimeInMilliseconds = (endTime - startTime) / 1_000_000.0;
        System.out.println("Computation time for finding the closest leader: " + computationTimeInMilliseconds + " ms");

        if (closestLeader != null) {
            System.out.println("Closest Leader for Notification: Leader " + closestLeader.getId());
            System.out.println("Shortest distance: " + shortestDistance + " meters");
        }
    }

    private double calculateShortestDistance(Node leaderNode, Node notificationNode) {
        dijkstra.setSource(leaderNode);
        dijkstra.compute();
        Path pathToNotification = dijkstra.getPath(notificationNode);
        if (pathToNotification != null) {
            return pathToNotification.getPathWeight("weight");
        }
        return Double.MAX_VALUE;
    }

    private void highlightPaths(List<Path> paths) {
        for (Path path : paths) {
            for (Edge edge : path.getEdgeSet()) {
                edge.setAttribute("ui.class", "highlighted");
            }
        }
    }

    public List<Path> calculatePaths(Leader leader, Room notificationRoom, Room exitRoom) {
        List<Path> paths = new ArrayList<>();

        Node startNode = graph.getNode(leader.getRoomLocation().getName());
        Node notificationNode = graph.getNode(notificationRoom.getName());

        if (startNode == null || notificationNode == null) {
            return paths;
        }

        // Calculate path to the notification room
        Path pathToNotification = calculatePath(startNode, notificationNode);
        if (pathToNotification != null) {
            paths.add(pathToNotification);
        }

        List<Room> assignedRooms = new ArrayList<>(leader.getAssignedRooms());
        Node currentNode = notificationNode;

        while (!assignedRooms.isEmpty()) {
            Path shortestPath = calculateShortestPathToAssignedRoom(currentNode, assignedRooms);
            if (shortestPath != null) {
                paths.add(shortestPath);
                Node finalCurrentNode = graph.getNode(shortestPath.getNodePath().get(shortestPath.getNodePath().size() - 1).getId());
                assignedRooms.removeIf(room -> room.getName().equals(finalCurrentNode.getId()));
                currentNode = finalCurrentNode;  // Update currentNode after the lambda expression
            } else {
                break; // If no more reachable rooms, exit the loop
            }
        }

        // Calculate path to the exit room
        Path pathToExit = calculatePath(currentNode, graph.getNode(exitRoom.getName()));
        if (pathToExit != null) {
            paths.add(pathToExit);
        }

        return paths;
    }


    private Path calculateShortestPathToAssignedRoom(Node currentNode, List<Room> assignedRooms) {
        Room closestRoom = null;
        Path shortestPath = null;
        double shortestDistance = Double.MAX_VALUE;

        for (Room room : assignedRooms) {
            Node roomNode = graph.getNode(room.getName());
            if (roomNode == null) continue;

            Path pathToRoom = calculatePath(currentNode, roomNode);
            if (pathToRoom != null) {
                double distance = pathToRoom.getPathWeight("weight");
                if (distance < shortestDistance) {
                    shortestDistance = distance;
                    shortestPath = pathToRoom;
                    closestRoom = room;
                }
            }
        }

        return shortestPath;
    }

    private Path calculatePath(Node sourceNode, Node targetNode) {
        dijkstra.setSource(sourceNode);
        dijkstra.compute();
        return dijkstra.getPath(targetNode);
    }

    public List<Path> calculatePathsWithoutNotification(Leader leader, Room exitRoom) {
        List<Path> paths = new ArrayList<>();

        Node currentNode = graph.getNode(leader.getRoomLocation().getName());
        List<Room> assignedRooms = leader.getAssignedRooms();
        for (Room room : assignedRooms) {
            Path pathToRoom = calculatePath(currentNode, graph.getNode(room.getName()));
            if (pathToRoom != null) {
                paths.add(pathToRoom);
                currentNode = graph.getNode(room.getName());
            }
        }

        Path pathToExit = calculatePath(currentNode, graph.getNode(exitRoom.getName()));
        if (pathToExit != null) {
            paths.add(pathToExit);
        }

        return paths;
    }

    public double calculateTotalWorkload(List<Path> paths) {
        double totalDistance = paths.stream().mapToDouble(path -> path.getPathWeight("weight")).sum();
        return totalDistance / LEADER_SPEED;
    }

    public double calculateTimeForPath(Path path) {
        return path.getPathWeight("weight") / LEADER_SPEED;
    }
}
