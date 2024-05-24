package org.example;

import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RecipientIdentificationHandler4 {
    private final BuildingEvacuation buildingEvacuation;
    private final List<Leader> leaders;
    private final Room notificationRoom;
    private final double[][] shortestPaths;

    public RecipientIdentificationHandler4(BuildingEvacuation buildingEvacuation, List<Leader> leaders, Room notificationRoom) {
        System.out.println("nearst heuristic algo results");
        this.buildingEvacuation = buildingEvacuation;
        this.leaders = leaders;
        this.notificationRoom = notificationRoom;

        // Initialize shortest paths between all pairs of rooms
        this.shortestPaths = calculateShortestPaths();
        System.out.println(notificationRoom.name);


        for (int i = 0; i < leaders.size(); i++) {
            if (!leaders.isEmpty()) {
                Leader leader = leaders.get(i);
                Room entrance = buildingEvacuation.getExitRoom();
                List<Room> tasks = leader.getAssignedRooms();
                long startTime = System.currentTimeMillis();
                calculateAndPrintOptimalPath(leader, notificationRoom, tasks, entrance);
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                System.out.printf("Computation time for heuristic solution: %d ms\n", duration);
            }
        }



        calculateTotalWorkloadForAllLeaders();


        // Calculate the total workload for each leader chosen for notification

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
        deactivateEdgesInGraph(leaders.get(bestLeaderIndex).getFullPath());
    }



    public void calculateTotalWorkloadForAllLeaders() {
        double totalWorkload = 0.0;
        Room entrance = buildingEvacuation.getExitRoom();

        for (int i = 0; i < 5; i++) {
            Leader leader = leaders.get(i);
            List<Room> tasks = leader.getAssignedRooms();
            double workload = calculateWorkloadForLeaderWithoutNotification(leader, tasks, entrance);
            //  System.out.printf("Workload for Leader if not chosen for notification %d: %.2f seconds\n", leader.getId(), workload);
            leaders.get(i).setWorkLoad(workload);
            totalWorkload += workload;
        }

        System.out.printf("Total workload for all leaders if not chosen for notification: %.2f seconds\n", totalWorkload);
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
        List<Integer> tspPath = nearestNeighborHeuristic(0, distances);

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

        // Print the distance matrix for verification
        //     System.out.println("Distance matrix:");
        //     for (int i = 0; i < n; i++) {
        //       for (int j = 0; j < n; j++) {
        //            System.out.printf("%.2f ", distances[i][j]);
        //         }
        //        System.out.println();
        //     }

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

        // Step 2: From the notification room, visit all task rooms in the optimal order using nearest neighbor heuristic
        List<Room> orderedTasks = new ArrayList<>(tasks);

        // Prepare an array of rooms for the heuristic starting from the notification room
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

        // Solve using the Nearest Neighbor heuristic
        List<Integer> heuristicPath = nearestNeighborHeuristic(0, distances);

        // Convert heuristic path indices to rooms
        List<Room> taskPath = new ArrayList<>();
        for (int index : heuristicPath) {
            taskPath.add(rooms[index]);
        }

        // Step 3: Add the shortest path from the last task to the entrance
        Room lastTask = taskPath.get(taskPath.size() - 1);
        List<Room> pathToEntrance = getFullPathBetweenRooms(lastTask, entrance);

        // Combine the paths
        List<Room> fullPath = new ArrayList<>(pathToNotification);
        taskPath.remove(0); // remove the notification room from taskPath to avoid duplication
        fullPath.addAll(taskPath);
        pathToEntrance.remove(0); // remove the starting room to avoid duplication
        fullPath.addAll(pathToEntrance);

        // Print the total travel time
        double totalDistance = calculateTotalDistance(fullPath);
        System.out.printf("Total travel time for Leader if chosen for notification %d: %.2f units\n", leader.getId(), totalDistance / 1.5);
        leader.setWorkloadNotification(totalDistance / 1.5);

        // Print the full sequence of rooms visited, including intermediary rooms and travel times
        System.out.println("Full sequence of rooms visited:");
        Room previousRoom = null;
        for (Room room : fullPath) {
            if (previousRoom != null) {
                double travelTime = getShortestPathLength(previousRoom.getName(), room.getName()) / 1.5;
                System.out.printf("Travel time to %s: %.2f units\n", room.getName(), travelTime);
            }
            System.out.println(room.getName());
            previousRoom = room;
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

    private List<Integer> nearestNeighborHeuristic(int start, double[][] distances) {
        int n = distances.length;
        boolean[] visited = new boolean[n];
        List<Integer> path = new ArrayList<>();
        int current = start;

        for (int i = 0; i < n; i++) {
            path.add(current);
            visited[current] = true;

            double nearestDistance = Double.MAX_VALUE;
            int nearestNeighbor = -1;

            for (int j = 0; j < n; j++) {
                if (!visited[j] && distances[current][j] < nearestDistance) {
                    nearestDistance = distances[current][j];
                    nearestNeighbor = j;
                }
            }

            if (nearestNeighbor != -1) {
                current = nearestNeighbor;
            }
        }

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
}