package org.example;

import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class RecipientIdentificationHandler2 {

    BuildingEvacuation buildingEvacuation;
    List<Leader> leaders;
    private static final double LEADER_SPEED = 1.5; // meters per second
    Room notificationRoom;
    Graph graph;
    double[][] distanceMatrix;
    List<Node> nodes;
    ConcurrentMap<Node, ConcurrentMap<Node, Path>> pathCache = new ConcurrentHashMap<>();

    public RecipientIdentificationHandler2(BuildingEvacuation buildingEvacuation, ArrayList<Leader> leaders, Room notificationRoom) {
        this.buildingEvacuation = buildingEvacuation;
        this.graph = buildingEvacuation.graph;
        this.notificationRoom = notificationRoom;
        this.nodes = new ArrayList<>(graph.nodes().toList());

        // Clone leaders to ensure new objects
        this.leaders = cloneLeaders(leaders);

        // Precompute the distance matrix
        precomputeDistanceMatrix();

        System.out.println("Room for notification: " + notificationRoom.name);

        Room exitRoom = buildingEvacuation.getExitRoom();
        printAllLeaderPathsAndWorkloads(notificationRoom, exitRoom);
        findClosestLeader(notificationRoom);
    }

    private List<Leader> cloneLeaders(List<Leader> leaders) {
        return leaders.stream()
                .map(leader -> {
                    Leader newLeader = new Leader(leader.getId(), leader.type, leader.getRoomLocation());
                    newLeader.setAssignedRooms(new ArrayList<>(leader.getAssignedRooms()));
                    return newLeader;
                })
                .collect(Collectors.toList());
    }

    private void precomputeDistanceMatrix() {
        int n = nodes.size();
        distanceMatrix = new double[n][n];

        // Initialize the distance matrix with infinity
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                distanceMatrix[i][j] = (i == j) ? 0 : Double.POSITIVE_INFINITY;
            }
        }

        // Set the initial distances based on the edges
        for (Edge edge : graph.edges().toList()) {
            int u = nodes.indexOf(edge.getSourceNode());
            int v = nodes.indexOf(edge.getTargetNode());
            double weight = edge.getNumber("weight");
            distanceMatrix[u][v] = weight;
            distanceMatrix[v][u] = weight; // Assuming undirected graph
        }

        // Floyd-Warshall algorithm
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (distanceMatrix[i][j] > distanceMatrix[i][k] + distanceMatrix[k][j]) {
                        distanceMatrix[i][j] = distanceMatrix[i][k] + distanceMatrix[k][j];
                    }
                }
            }
        }
    }

    private double getDistance(Node u, Node v) {
        int i = nodes.indexOf(u);
        int j = nodes.indexOf(v);
        return distanceMatrix[i][j];
    }

    public void printAllLeaderPathsAndWorkloads(Room notificationRoom, Room exitRoom) {
        double minimumTotalWorkload = Double.MAX_VALUE;
        Leader optimalLeaderForNotification = null;
        List<Path> optimalPathsForNotificationLeader = new ArrayList<>();

        long startTime = System.nanoTime(); // Start timing

        for (Leader notifyingLeader : leaders) {
            System.out.println("Leader " + notifyingLeader.getId() + " chosen for notification:");
            double totalWorkloadForAllLeaders = 0.0;
            List<Path> pathsForCurrentLeader = new ArrayList<>();

            for (Leader leader : leaders) {
                System.out.println("Leader " + leader.getId() + ":");
                List<Path> paths;
                if (leader.equals(notifyingLeader)) {
                    paths = calculatePaths(leader, notificationRoom, exitRoom);
                    pathsForCurrentLeader = paths;
                } else {
                    paths = calculatePathsWithoutNotification(leader, exitRoom);
                }

                double totalWorkload = calculateTotalWorkload(paths);
                totalWorkloadForAllLeaders += totalWorkload;

                printPaths(paths);
                System.out.println("Total workload for Leader " + leader.getId() + ": " + totalWorkload + " seconds");
                System.out.println();
            }

            System.out.println("Total workload for all leaders combined: " + totalWorkloadForAllLeaders + " seconds");
            System.out.println("=======================================================\n");

            // Update the optimal leader if the current total workload is lower than the minimum found so far
            if (totalWorkloadForAllLeaders < minimumTotalWorkload) {
                minimumTotalWorkload = totalWorkloadForAllLeaders;
                optimalLeaderForNotification = notifyingLeader;
                optimalPathsForNotificationLeader = pathsForCurrentLeader;
            }
        }

        long endTime = System.nanoTime(); // End timing
        long computationTime = endTime - startTime; // Calculate the computation time

        // Convert nanoseconds to milliseconds for easier reading
        double computationTimeInMilliseconds = computationTime / 1_000_000.0;
        System.out.println("Computation time for finding the optimal leader: " + computationTimeInMilliseconds + " ms");

        // Highlight the optimal paths for the leader chosen for notification
        if (optimalLeaderForNotification != null) {
            System.out.println("Optimal Leader for Notification: Leader " + optimalLeaderForNotification.getId());
            System.out.println("Total workload: " + minimumTotalWorkload + " seconds");
            highlightPaths(optimalPathsForNotificationLeader);
        }
    }

    // New method to find the closest leader
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

            double distance = getDistance(leaderNode, notificationNode);
            if (distance < shortestDistance) {
                shortestDistance = distance;
                closestLeader = leader;
            }
        }

        long endTime = System.nanoTime(); // End timing
        long computationTime = endTime - startTime; // Calculate the computation time

        // Convert nanoseconds to milliseconds for easier reading
        double computationTimeInMilliseconds = computationTime / 1_000_000.0;
        System.out.println("Computation time for finding the closest leader: " + computationTimeInMilliseconds + " ms");

        if (closestLeader != null) {
            System.out.println("Closest Leader for Notification: Leader " + closestLeader.getId());
            System.out.println("Shortest distance: " + shortestDistance + " meters");
        }
    }

    // Method to highlight paths
    public void highlightPaths(List<Path> paths) {
        for (Path path : paths) {
            for (Edge edge : path.getEdgeSet()) {
                edge.setAttribute("ui.class", "highlighted");
            }
        }
    }

    public void printPaths(List<Path> paths) {
        for (Path path : paths) {
            System.out.print("Path: ");
            for (Node node : path.getNodePath()) {
                System.out.print(node.getId() + " ");
            }
            System.out.println(" | Time to travel: " + calculateTimeForPath(path) + " seconds");
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
        Path pathToNotification = getPath(startNode, notificationNode);
        if (pathToNotification != null) {
            paths.add(pathToNotification);
        }

        List<Room> assignedRooms = new ArrayList<>(leader.getAssignedRooms());
        Node currentNode = notificationNode;

        while (!assignedRooms.isEmpty()) {
            Room closestRoom = null;
            Path shortestPath = null;
            double shortestDistance = Double.MAX_VALUE;

            // Find the closest room
            for (Room room : assignedRooms) {
                Node roomNode = graph.getNode(room.getName());
                if (roomNode == null) continue;

                Path pathToRoom = getPath(currentNode, roomNode);
                if (pathToRoom != null) {
                    double distance = getDistance(currentNode, roomNode);
                    if (distance < shortestDistance) {
                        shortestDistance = distance;
                        shortestPath = pathToRoom;
                        closestRoom = room;
                    }
                }
            }

            if (shortestPath != null && closestRoom != null) {
                paths.add(shortestPath);
                currentNode = graph.getNode(closestRoom.getName());
                assignedRooms.remove(closestRoom);
            } else {
                break; // If no more reachable rooms, exit the loop
            }
        }

        // Calculate path to the exit room
        Node exitNode = graph.getNode(exitRoom.getName());
        if (exitNode != null) {
            Path pathToExit = getPath(currentNode, exitNode);
            if (pathToExit != null) {
                paths.add(pathToExit);
            }
        }

        return paths;
    }

    public List<Path> calculatePathsWithoutNotification(Leader leader, Room exitRoom) {
        List<Path> paths = new ArrayList<>();
        Node currentNode = graph.getNode(leader.getRoomLocation().getName());
        List<Room> assignedRooms = leader.getAssignedRooms();

        for (Room room : assignedRooms) {
            Node roomNode = graph.getNode(room.getName());
            if (roomNode == null) {
                continue;
            }

            Path pathToRoom = getPath(currentNode, roomNode);
            if (pathToRoom != null) {
                paths.add(pathToRoom);
                currentNode = roomNode;
            }
        }

        Node exitNode = graph.getNode(exitRoom.getName());
        if (exitNode != null) {
            Path pathToExit = getPath(currentNode, exitNode);
            if (pathToExit != null) {
                paths.add(pathToExit);
            }
        }

        return paths;
    }

    private Path getPath(Node source, Node target) {
        // Check cache first
        pathCache.putIfAbsent(source, new ConcurrentHashMap<>());
        ConcurrentMap<Node, Path> sourceCache = pathCache.get(source);

        if (sourceCache.containsKey(target)) {
            return sourceCache.get(target);
        }

        Dijkstra dijkstra = new Dijkstra(Dijkstra.Element.EDGE, null, "weight");
        dijkstra.init(graph);
        dijkstra.setSource(source);
        dijkstra.compute();
        Path path = dijkstra.getPath(target);

        sourceCache.put(target, path);
        return path;
    }

    public double calculateTotalWorkload(List<Path> paths) {
        double totalDistance = 0.0;
        for (Path path : paths) {
            totalDistance += path.getPathWeight("weight");
        }
        return totalDistance / LEADER_SPEED;
    }

    public double calculateTimeForPath(Path path) {
        double totalDistance = path.getPathWeight("weight");
        return totalDistance / LEADER_SPEED;
    }
}
