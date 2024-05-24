package org.example;

import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Path;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RecipientIdentificationHandler3 {
    private final BuildingEvacuation buildingEvacuation;
    private final List<Leader> leaders;
    private final Room notificationRoom;
    private final double[][] shortestPaths;

    public RecipientIdentificationHandler3(BuildingEvacuation buildingEvacuation, List<Leader> leaders, Room notificationRoom) {
        System.out.println("bitmask tsp solution results");
        this.buildingEvacuation = buildingEvacuation;
        this.leaders = leaders;
        this.notificationRoom = notificationRoom;

        // Initialize shortest paths between all pairs of rooms
        this.shortestPaths = calculateShortestPaths();
        System.out.println(notificationRoom.name);
        // Calculate and print the optimal path for the first leader


        for (int i = 0; i < leaders.size(); i++) {
            if (!leaders.isEmpty()) {
                Leader leader = leaders.get(i);
                Room entrance = buildingEvacuation.getExitRoom();
                List<Room> tasks = leader.getAssignedRooms();
                long startTime = System.currentTimeMillis();
                calculateAndPrintOptimalPath(leader, notificationRoom, tasks, entrance);
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                System.out.printf("Computation time for optimal sol: %d ms\n", duration);
            }
        }


        // Calculate total workload for all leaders without notification
        calculateTotalWorkloadForAllLeaders();

        int bestLeaderIndex = -1;
        double minTotalWorkload = Double.MAX_VALUE;

        for (int i = 0; i < leaders.size(); i++) {
            double totalWorkload = leaders.get(i).getWorkloadNotification();
            System.out.println("Leader " + leaders.get(i).id + " chosen for notify:");
            System.out.println("Workload for notify: " + leaders.get(i).getWorkloadNotification());

            System.out.println("Workload for rest: ");
            for (int j = 0; j < leaders.size(); j++) {
                if (j != i) {
                    totalWorkload += leaders.get(j).getWorkLoad();
                    System.out.println("Leader id: " + leaders.get(j).id + " workload: " + leaders.get(j).getWorkLoad());
                }
            }
            System.out.println("Total workload for scenario with leader " + leaders.get(i).id + " chosen for notify: " + totalWorkload);
            System.out.println();

            // Update the minimum workload and best leader index
            if (totalWorkload < minTotalWorkload) {
                minTotalWorkload = totalWorkload;
                bestLeaderIndex = i;
            }
        }


        // Print out the leader with the lowest total workload
        if (bestLeaderIndex != -1) {
            System.out.println("Leader with the lowest total workload: Leader " + leaders.get(bestLeaderIndex).id);
            System.out.println("Total workload: " + minTotalWorkload);
        }
        System.out.println(leaders.get(bestLeaderIndex).getFullPath());
     //   deactivateEdgesInGraph(leaders.get(bestLeaderIndex).getFullPath());

        // Find the closest leader to the notification room
        findClosestLeaderToNotification();

    }

    private void findClosestLeaderToNotification() {
        double minDistance = Double.MAX_VALUE;
        Leader closestLeader = null;

        for (Leader leader : leaders) {
            Room initialLocation = leader.getRoomLocation();
            double distanceToNotification = getShortestPathLength(initialLocation.getName(), notificationRoom.getName());
            if (distanceToNotification < minDistance) {
                minDistance = distanceToNotification;
                closestLeader = leader;
            }
        }

        if (closestLeader != null) {
            System.out.printf("Closest leader to the notification room is Leader %d with a distance of %.2f units\n", closestLeader.getId(), minDistance);
        }
    }

    public void calculateTotalWorkloadForAllLeaders() {
        double totalWorkload = 0.0;
        Room entrance = buildingEvacuation.getExitRoom();

        for (int i = 0; i < 5; i++) {
            Leader leader = leaders.get(i);
            List<Room> tasks = leader.getAssignedRooms();
            double workload = calculateWorkloadForLeaderWithoutNotification(leader, tasks, entrance);
            System.out.printf("Workload for Leader %d: %.2f seconds\n", leader.getId(), workload);
            leaders.get(i).setWorkLoad(workload);
            totalWorkload += workload;
        }

        System.out.printf("Total workload for all leaders: %.2f seconds\n", totalWorkload);
    }

    private double calculateWorkloadForLeaderWithoutNotification(Leader leader, List<Room> tasks, Room entrance) {
        Room initialLocation = leader.getRoomLocation();

        // Prepare an array of rooms for TSP starting from the initial location and ending at the entrance
        int n = tasks.size() + 1; // +1 for the entrance
        Room[] rooms = new Room[n];
        rooms[0] = initialLocation;
        for (int i = 0; i < tasks.size(); i++) {
            rooms[i + 1] = tasks.get(i);
        }

        // Calculate distances between these rooms
        double[][] distances = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                distances[i][j] = shortestPaths[rooms[i].getIndex()][rooms[j].getIndex()];
            }
        }

        // Solve TSP starting from the initial location (index 0)
        List<Integer> tspPath = tsp(0, distances);

        // Calculate the total distance
        double totalDistance = 0;
        for (int i = 0; i < tspPath.size() - 1; i++) {
            int from = tspPath.get(i);
            int to = tspPath.get(i + 1);
            totalDistance += shortestPaths[from][to];
        }

        // Add the distance to the entrance
        totalDistance += shortestPaths[tspPath.get(tspPath.size() - 1)][entrance.getIndex()];

        return totalDistance / 1.5; // Assuming LEADER_SPEED = 1.5
    }

    private double[][] calculateShortestPaths() {
        int n = buildingEvacuation.allRooms.size();
        double[][] distances = new double[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i != j) {
                    distances[i][j] = getShortestPathLength(buildingEvacuation.allRooms.get(i).name, buildingEvacuation.allRooms.get(j).name);
                } else {
                    distances[i][j] = 0;
                }
            }
        }

        return distances;
    }

    private double getShortestPathLength(String from, String to) {
        Dijkstra dijkstra = new Dijkstra(Dijkstra.Element.EDGE, null, "weight");
        dijkstra.init(buildingEvacuation.graph);
        dijkstra.setSource(buildingEvacuation.graph.getNode(from));
        dijkstra.compute();
        return dijkstra.getPathLength(buildingEvacuation.graph.getNode(to));
    }

    private void calculateAndPrintOptimalPath(Leader leader, Room notificationRoom, List<Room> tasks, Room entrance) {
        // Step 1: Travel from the leader's initial location to the notification room
        Room initialLocation = leader.getRoomLocation();
        List<Room> pathToNotification = getFullPathBetweenRooms(initialLocation, notificationRoom);
        double distanceToNotification = calculateTotalDistance(pathToNotification);

        // Step 2: From the notification room, visit all task rooms in the optimal order, then end at the entrance
        List<Room> orderedTasks = new ArrayList<>(tasks);

        // Prepare an array of rooms for TSP starting from the notification room (excluding the entrance initially)
        int n = orderedTasks.size() + 1; // +1 for the notification room
        Room[] rooms = new Room[n];
        rooms[0] = notificationRoom;
        for (int i = 0; i < orderedTasks.size(); i++) {
            rooms[i + 1] = orderedTasks.get(i);
        }

        // Calculate distances between these rooms
        double[][] distances = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                distances[i][j] = shortestPaths[rooms[i].getIndex()][rooms[j].getIndex()];
            }
        }

        // Solve TSP starting from the notification room (index 0)
        List<Integer> tspPath = tsp(0, distances);

        // Add the entrance as the final stop in the path
        List<Integer> finalPath = new ArrayList<>(tspPath);
        finalPath.add(n);

        // Print the total travel time
        double totalDistance = distanceToNotification;
        for (int i = 0; i < finalPath.size() - 1; i++) {
            int from = finalPath.get(i);
            int to = (i == finalPath.size() - 2) ? entrance.getIndex() : finalPath.get(i + 1);
            totalDistance += shortestPaths[from][to];
        }
        System.out.printf("Total travel time for Leader %d: %.2f units\n", leader.getId(), totalDistance / 1.5);
       leader.setWorkloadNotification(totalDistance / 1.5);

        // Print the full sequence of rooms visited, including intermediary rooms and travel times
        System.out.println("Full sequence of rooms visited:");
        List<Room> fullPath = new ArrayList<>(pathToNotification);
        for (int i = 0; i < finalPath.size() - 1; i++) {
            List<Room> subPath = getFullPathBetweenRooms(rooms[finalPath.get(i)], (i == finalPath.size() - 2) ? entrance : rooms[finalPath.get(i + 1)]);
            if (i == 0) {
                fullPath.addAll(subPath); // add all rooms for the first subPath
            } else {
                subPath.remove(0); // remove the starting room to avoid duplication
                fullPath.addAll(subPath);
            }
        }
        leader.setFullPath((ArrayList<Room>) fullPath);

        // Print each room in the full path with travel times
        Room previousRoom = null;
        for (Room room : fullPath) {
            if (previousRoom != null) {
                double travelTime = getShortestPathLength(previousRoom.getName(), room.getName()) / 1.5;
                System.out.printf("Travel time to %s: %.2f units\n", room.getName(), travelTime);
            }
            System.out.println(room.getName());
            previousRoom = room;
        }


    }

    private double calculateTotalDistance(List<Room> rooms) {
        double totalDistance = 0;
        for (int i = 0; i < rooms.size() - 1; i++) {
            totalDistance += getShortestPathLength(rooms.get(i).getName(), rooms.get(i + 1).getName());
        }
        return totalDistance;
    }

    private List<Room> getFullPathBetweenRooms(Room from, Room to) {
        Dijkstra dijkstra = new Dijkstra(Dijkstra.Element.EDGE, null, "weight");
        dijkstra.init(buildingEvacuation.graph);
        dijkstra.setSource(buildingEvacuation.graph.getNode(from.getName()));
        dijkstra.compute();

        Path path = dijkstra.getPath(buildingEvacuation.graph.getNode(to.getName()));
        List<Room> fullPath = new ArrayList<>();
        path.getNodePath().forEach(node -> fullPath.add(buildingEvacuation.getRoomByName(node.getId())));

        return fullPath;
    }

    private List<Integer> tsp(int start, double[][] distances) {
        int n = distances.length;
        double[][] dp = new double[1 << n][n];
        int[][] parent = new int[1 << n][n]; // To track the path

        for (double[] row : dp) {
            Arrays.fill(row, Double.MAX_VALUE);
        }
        dp[1 << start][start] = 0;

        for (int mask = 1; mask < (1 << n); mask++) {
            for (int u = 0; u < n; u++) {
                if ((mask & (1 << u)) != 0) {
                    for (int v = 0; v < n; v++) {
                        if ((mask & (1 << v)) == 0) {
                            double newDist = dp[mask][u] + distances[u][v];
                            if (newDist < dp[mask | (1 << v)][v]) {
                                dp[mask | (1 << v)][v] = newDist;
                                parent[mask | (1 << v)][v] = u;
                            }
                        }
                    }
                }
            }
        }

        double minCost = Double.MAX_VALUE;
        int endState = (1 << n) - 1;
        int lastNode = -1;

        for (int i = 0; i < n; i++) {
            if (i != start) {
                double cost = dp[endState][i] + distances[i][start];
                if (cost < minCost) {
                    minCost = cost;
                    lastNode = i;
                }
            }
        }

        // Reconstruct the path
        List<Integer> path = new ArrayList<>();
        int current = lastNode;
        int currentState = endState;

        while (current != start) {
            path.add(0, current);
            int temp = currentState;
            currentState = currentState ^ (1 << current);
            current = parent[temp][current];
        }

        path.add(0, start);
        return path;
    }

    private void deactivateEdgesInGraph(List<Room> fullPath) {
        for (int i = 0; i < fullPath.size() - 1; i++) {
            String edgeId = fullPath.get(i).getName() + "-" + fullPath.get(i + 1).getName();
            String reverseEdgeId = fullPath.get(i + 1).getName() + "-" + fullPath.get(i).getName();
            if (buildingEvacuation.graph.getEdge(edgeId) != null) {
                buildingEvacuation.graph.getEdge(edgeId).setAttribute("ui.style", "fill-color: red;");
            }
            if (buildingEvacuation.graph.getEdge(reverseEdgeId) != null) {
                buildingEvacuation.graph.getEdge(reverseEdgeId).setAttribute("ui.style", "fill-color: red;");
            }
        }
    }

    // New method to calculate the total workload for each leader when chosen for notification
    private void calculateTotalWorkloadForAllLeadersWithNotification() {
        Room entrance = buildingEvacuation.getExitRoom();

        for (Leader chosenLeader : leaders) {
            double totalWorkload = 0.0;

            // Calculate workload for the chosen leader with notification
            List<Room> chosenLeaderTasks = chosenLeader.getAssignedRooms();
            double chosenLeaderWorkload = calculateWorkloadForLeaderWithNotification(chosenLeader, chosenLeaderTasks, entrance);
            totalWorkload += chosenLeaderWorkload;

            // Calculate workload for the rest of the leaders without notification
            for (Leader leader : leaders) {
                if (!leader.equals(chosenLeader)) {
                    List<Room> tasks = leader.getAssignedRooms();
                    double workload = calculateWorkloadForLeaderWithoutNotification(leader, tasks, entrance);
                    totalWorkload += workload;
                }
            }

            System.out.printf("Total workload with Leader %d chosen for notification: %.2f seconds\n", chosenLeader.getId(), totalWorkload);
        }
    }

    private double calculateWorkloadForLeaderWithNotification(Leader leader, List<Room> tasks, Room entrance) {
        Room initialLocation = leader.getRoomLocation();

        // Prepare an array of rooms for TSP starting from the initial location and ending at the entrance
        int n = tasks.size() + 2; // +1 for the notification room, +1 for the entrance
        Room[] rooms = new Room[n];
        rooms[0] = initialLocation;
        rooms[1] = notificationRoom;
        for (int i = 0; i < tasks.size(); i++) {
            rooms[i + 2] = tasks.get(i);
        }

        // Calculate distances between these rooms
        double[][] distances = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                distances[i][j] = shortestPaths[rooms[i].getIndex()][rooms[j].getIndex()];
            }
        }

        // Solve TSP starting from the initial location (index 0)
        List<Integer> tspPath = tsp(0, distances);

        // Calculate the total distance
        double totalDistance = 0;
        for (int i = 0; i < tspPath.size() - 1; i++) {
            int from = tspPath.get(i);
            int to = tspPath.get(i + 1);
            totalDistance += shortestPaths[from][to];
        }

        // Add the distance to the entrance
        totalDistance += shortestPaths[tspPath.get(tspPath.size() - 1)][entrance.getIndex()];

        return totalDistance / 1.5; // Assuming LEADER_SPEED = 1.5
    }
}
