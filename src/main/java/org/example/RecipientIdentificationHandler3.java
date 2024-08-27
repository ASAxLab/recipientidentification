package org.example;

import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Path;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RecipientIdentificationHandler3 {
    private final BuildingEvacuation buildingEvacuation;
    private final List<Leader> leaders;
    private final List<Room> notificationRooms;
    private final double[][] shortestPaths;

    public RecipientIdentificationHandler3(BuildingEvacuation buildingEvacuation, List<Leader> leaders, List<Room> notificationRooms) {
        System.out.println("\n--- Bitmask TSP Solution Results ---\n");
        this.buildingEvacuation = buildingEvacuation;
        this.leaders = leaders;
        this.notificationRooms = notificationRooms;

        // Initialize shortest paths between all pairs of rooms
        this.shortestPaths = calculateShortestPaths();
        // Calculate total workload for all leaders without notification
        calculateTotalWorkloadForAllLeaders();
        long startTime = System.currentTimeMillis();

        // Iterate over each notification room
        for (Room notificationRoom : notificationRooms) {
            System.out.println("Notification Room: " + notificationRoom.name);

            // Calculate and print the optimal path for each leader with the current notification
            for (Leader leader : leaders) {
                Room entrance = buildingEvacuation.getExitRoom();
                List<Room> tasks = new ArrayList<>(leader.getAssignedRooms());
                tasks.add(notificationRoom);  // Temporarily add the notification room to the leader's tasks
                calculateAndPrintOptimalPath(leader, notificationRoom, tasks, entrance);
            }

            // Find and assign the best leader for the current notification
            findBestLeaderForNotification(notificationRoom);

            // After assigning the notification, recalculate the total workload for all leaders
        }


        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        System.out.printf("Computation Time for Optimal Solution: %d ms\n\n", duration);


        // Evaluate the best leader for each notification
        for (Room notificationRoom : notificationRooms) {
           // findBestLeaderForNotification(notificationRoom);
        }

        deactivateEdgesInGraph(leaders.get(4).getFullPath());
    }

    private void findBestLeaderForNotification(Room notificationRoom) {
        int bestLeaderIndex = -1;
        double minTotalWorkload = Double.MAX_VALUE;

        for (int i = 0; i < leaders.size(); i++) {
            double totalWorkload = leaders.get(i).getWorkloadNotification();
            System.out.println("\n--- Evaluation for Leader " + leaders.get(i).id + " for Notification Room " + notificationRoom.getName() + " ---");
            System.out.printf("Workload with Notification: %.2f seconds\n", totalWorkload);

            System.out.println("Workload for Rest:");
            for (int j = 0; j < leaders.size(); j++) {
                if (j != i) {
                    totalWorkload += leaders.get(j).getWorkLoad();
                    System.out.printf("Leader ID: %d, Workload: %.2f seconds\n", leaders.get(j).id, leaders.get(j).getWorkLoad());
                }
            }
            System.out.printf("Total Workload for Scenario with Leader %d Notified: %.2f seconds\n", leaders.get(i).id, totalWorkload);

            List<Room> fullPath = leaders.get(i).getFullPath();
           double totalDistanceInSeconds = 0.0;
            for (int k = 0; k < fullPath.size() - 1; k++) {
                Room fromRoom = fullPath.get(k);
                Room toRoom = fullPath.get(k + 1);
                double distance = getShortestPathLength(fromRoom.getName(), toRoom.getName());
                double timeInSeconds = distance / 1.5;
                totalDistanceInSeconds += timeInSeconds;
                System.out.printf(" - %s to %s: %.2f seconds\n", fromRoom.getName(), toRoom.getName(), timeInSeconds);
            }
           System.out.printf("Total Time for Leader %d: %.2f seconds\n", leaders.get(i).id, totalDistanceInSeconds);

            if (totalWorkload < minTotalWorkload) {
                minTotalWorkload = totalWorkload;
                bestLeaderIndex = i;
            }
        }

        if (bestLeaderIndex != -1) {
            leaders.get(bestLeaderIndex).setWorkLoad(leaders.get(bestLeaderIndex).getWorkloadNotification());
            Leader bestLeader = leaders.get(bestLeaderIndex);

            for (int i = 0; i < leaders.size(); i++) {
                leaders.get(i).setWorkloadNotification(0);
            }
            bestLeader.addAssignedRoom(notificationRoom);  // Assign the notification room to the leader

            System.out.printf("\n--- Leader with Lowest Total Workload for Notification Room %s ---\nLeader ID: %d\nTotal Workload: %.2f seconds\n",
                    notificationRoom.getName(), bestLeader.getId(), minTotalWorkload);

            //            Print the full path with travel times of the chosen leader again after notification is assigned
   //         System.out.println("Full Path for Chosen Leader " + bestLeader.getId() + " after Notification:");
   //         List<Room> fullPath = bestLeader.getFullPath();
   //         double totalDistanceInSeconds = 0.0;
  //          for (int k = 0; k < fullPath.size() - 1; k++) {
  //              Room fromRoom = fullPath.get(k);
   //             Room toRoom = fullPath.get(k + 1);
   //             double timeInSeconds = distance / 1.5;
   //            totalDistanceInSeconds += timeInSeconds;
  //              System.out.printf(" - %s to %s: %.2f seconds\n", fromRoom.getName(), toRoom.getName(), timeInSeconds);
 //       }
            }
          //  System.out.printf("Total Time for Leader %d after Notification: %.2f seconds\n", bestLeader.getId(), totalDistanceInSeconds);
        }


    public void calculateTotalWorkloadForAllLeaders() {
        double totalWorkload = 0.0;
        Room entrance = buildingEvacuation.getExitRoom();

        System.out.println("\n--- Workload Calculation for All Leaders ---");
        for (Leader leader : leaders) {
            List<Room> tasks = leader.getAssignedRooms();
            double workload = calculateWorkloadForLeaderWithoutNotification(leader, tasks, entrance);

            System.out.printf("Leader ID: %d, Workload: %.2f seconds\n", leader.getId(), workload);
            leader.setWorkLoad(workload);
            totalWorkload += workload;
        }

        System.out.printf("Total Workload for All Leaders: %.2f seconds\n", totalWorkload);
    }

    private double calculateWorkloadForLeaderWithoutNotification(Leader leader, List<Room> tasks, Room entrance) {
        Room initialLocation = leader.getRoomLocation();

        // Prepare an array of rooms for TSP starting from the initial location and ending at the entrance
        int n = tasks.size() + 2; // +1 for the initial location, +1 for the entrance
        Room[] rooms = new Room[n];
        rooms[0] = initialLocation;
        for (int i = 0; i < tasks.size(); i++) {
            rooms[i + 1] = tasks.get(i);
        }
        rooms[n - 1] = entrance;

        // Calculate distances between these rooms
        double[][] distances = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                distances[i][j] = shortestPaths[rooms[i].getIndex()][rooms[j].getIndex()];
            }
        }

        // Solve TSP starting from the initial location (index 0) and ending at the entrance
        List<Integer> tspPath = tsp(0, distances, n - 1); // Pass entrance index

        // Calculate the total distance
        double totalDistance = 0;
        List<Room> fullPath = new ArrayList<>();

        for (int i = 0; i < tspPath.size() - 1; i++) {
            int from = tspPath.get(i);
            int to = tspPath.get(i + 1);
            totalDistance += distances[from][to];
            List<Room> subPath = getFullPathBetweenRooms(rooms[from], rooms[to]);

            if (i == 0) {
                fullPath.addAll(subPath); // Add all rooms for the first subPath
            } else {
                subPath.remove(0); // Remove the starting room to avoid duplication
                fullPath.addAll(subPath);
            }
        }

        fullPath.add(entrance); // Add entrance as the final stop

        // Store the full path in the leader object
        leader.setFullPath((ArrayList<Room>) fullPath);

 //Print the full path with travel times
        System.out.println("Full Path for Leader " + leader.getId() + " without Notification:");
        double totalDistanceInSeconds = 0.0;

        for (int i = 0; i < fullPath.size() - 1; i++) {
            Room fromRoom = fullPath.get(i);
            Room toRoom = fullPath.get(i + 1);
            double distance = getShortestPathLength(fromRoom.getName(), toRoom.getName());
            double timeInSeconds = distance / 1.5; // Assuming LEADER_SPEED = 1.5 m/s
            totalDistanceInSeconds += timeInSeconds;
            System.out.printf(" - %s to %s: %.2f seconds\n", fromRoom.getName(), toRoom.getName(), timeInSeconds);
        }
        System.out.printf("Total Time for Leader %d without Notification: %.2f seconds\n", leader.getId(), totalDistanceInSeconds);

        return totalDistance / 1.5; // Assuming LEADER_SPEED = 1.5 m/s
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

        // Check if either the 'from' or 'to' room is blocked
    //    Room fromRoom = buildingEvacuation.getRoomByName(from);
   //     Room toRoom = buildingEvacuation.getRoomByName(to);
    //    if (fromRoom.isBlocked() || toRoom.isBlocked()) {
   //         return Double.MAX_VALUE; // Return a very high value if a room is blocked
   //     }

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
        int n = tasks.size() + 2; // +1 for the notification room, +1 for the entrance
        Room[] rooms = new Room[n];
        rooms[0] = notificationRoom;
        for (int i = 0; i < tasks.size(); i++) {
            rooms[i + 1] = tasks.get(i);
        }
        rooms[n - 1] = entrance;

        // Calculate distances between these rooms
        double[][] distances = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                distances[i][j] = shortestPaths[rooms[i].getIndex()][rooms[j].getIndex()];
            }
        }

        // Solve TSP starting from the notification room (index 0) and ending at the entrance
        List<Integer> tspPath = tsp(0, distances, n - 1); // Pass entrance index

        // Calculate the total distance including the initial travel to notification room
        double totalDistance = distanceToNotification;
        for (int i = 0; i < tspPath.size() - 1; i++) {
            int from = tspPath.get(i);
            int to = tspPath.get(i + 1);
            totalDistance += distances[from][to];
        }

        leader.setWorkloadNotification(totalDistance / 1.5);

        // Print the full sequence of rooms visited, including intermediary rooms and travel times
        List<Room> fullPath = new ArrayList<>(pathToNotification);
        for (int i = 0; i < tspPath.size() - 1; i++) {
            List<Room> subPath = getFullPathBetweenRooms(rooms[tspPath.get(i)], rooms[tspPath.get(i + 1)]);
            if (i == 0) {
                fullPath.addAll(subPath); // Add all rooms for the first subPath
            } else {
                subPath.remove(0); // Remove the starting room to avoid duplication
                fullPath.addAll(subPath);
            }
        }
        leader.setFullPath((ArrayList<Room>) fullPath);
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

    private List<Integer> tsp(int start, double[][] distances, int entranceIndex) {
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
                        if ((mask & (1 << v)) == 0 && v != entranceIndex) { // Ensure not to add entrance in the middle of the path
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

        // Find the best path to the entrance
        for (int i = 0; i < n; i++) {
            if (i != start && i != entranceIndex) {
                double cost = dp[endState ^ (1 << entranceIndex)][i] + distances[i][entranceIndex];
                if (cost < minCost) {
                    minCost = cost;
                    lastNode = i;
                }
            }
        }

        // Reconstruct the path
        List<Integer> path = new ArrayList<>();
        int current = lastNode;
        int currentState = endState ^ (1 << entranceIndex);

        while (current != start) {
            path.add(0, current);
            int temp = currentState;
            currentState = currentState ^ (1 << current);
            current = parent[temp][current];
        }

        path.add(0, start);
        path.add(entranceIndex); // Add the entrance as the final stop
        return path;
    }
}
