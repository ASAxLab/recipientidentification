package org.example;

import org.example.BuildingEvacuation;
import org.example.Leader;
import org.example.Room;
import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Path;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RecipientIdentificationHandler4 {
    private final BuildingEvacuation buildingEvacuation;
    private final List<Leader> leaders;
    private final List<Room> notificationRooms;
    private final double[][] shortestPaths;

    public RecipientIdentificationHandler4(BuildingEvacuation buildingEvacuation, List<Leader> leaders, List<Room> notificationRooms) {
        System.out.println("\n--- NN heuristic Solution Results ---\n");
        this.buildingEvacuation = buildingEvacuation;
        this.leaders = leaders;
        this.notificationRooms = notificationRooms;


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
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        System.out.printf("Computation Time for NN heuristic Solution: %d ms\n\n", duration);

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

            // Print the full path with travel times of the chosen leader after notification is assigned
            System.out.println("Full Path for Chosen Leader " + bestLeader.getId() + " after Notification:");
            List<Room> fullPath = bestLeader.getFullPath();
            double totalDistanceInSeconds = 0.0;
            for (int k = 0; k < fullPath.size() - 1; k++) {
                Room fromRoom = fullPath.get(k);
                Room toRoom = fullPath.get(k + 1);
                double distance = getShortestPathLength(fromRoom.getName(), toRoom.getName());
                double timeInSeconds = distance / 1.5;
                totalDistanceInSeconds += timeInSeconds;
                System.out.printf(" - %s to %s: %.2f seconds\n", fromRoom.getName(), toRoom.getName(), timeInSeconds);
            }
    //        System.out.printf("Total Time for Leader %d after Notification: %.2f seconds\n", bestLeader.getId(), totalDistanceInSeconds);
        }
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

        // Prepare an array of rooms for the heuristic starting from the initial location
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

        // Solve using the Nearest Neighbor heuristic
        List<Integer> heuristicPath = nearestNeighborHeuristic(0, distances);

        // Calculate the total distance
        double totalDistance = 0;
        List<Room> fullPath = new ArrayList<>();
        fullPath.add(rooms[0]);

        for (int i = 0; i < heuristicPath.size() - 1; i++) {
            int from = heuristicPath.get(i);
            int to = heuristicPath.get(i + 1);
            totalDistance += distances[from][to];
            fullPath.add(rooms[to]);
        }

        // Add the entrance as the final destination
        totalDistance += shortestPaths[heuristicPath.get(heuristicPath.size() - 1)][entrance.getIndex()];
        fullPath.add(entrance);

        // Store the full path in the leader object
        leader.setFullPath((ArrayList<Room>) fullPath);

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

        // Calculate the total distance including the initial travel to notification room
        double totalDistance = distanceToNotification;
        List<Room> fullPath = new ArrayList<>(pathToNotification);

        for (int i = 0; i < heuristicPath.size() - 1; i++) {
            List<Room> subPath = getFullPathBetweenRooms(rooms[heuristicPath.get(i)], rooms[heuristicPath.get(i + 1)]);
            totalDistance += calculateTotalDistance(subPath);
            if (i == 0) {
                fullPath.addAll(subPath); // Add all rooms for the first subPath
            } else {
                subPath.remove(0); // Remove the starting room to avoid duplication
                fullPath.addAll(subPath);
            }
        }

        // Ensure the entrance is the final destination
        List<Room> pathToEntrance = getFullPathBetweenRooms(rooms[heuristicPath.get(heuristicPath.size() - 1)], entrance);
        totalDistance += calculateTotalDistance(pathToEntrance);
        fullPath.addAll(pathToEntrance);

        leader.setWorkloadNotification(totalDistance / 1.5);



        // Store the full path in the leader object
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
        List<Integer> path = new ArrayList<>();
        boolean[] visited = new boolean[n];
        int current = start;
        path.add(current);
        visited[current] = true;

        for (int i = 1; i < n; i++) {
            double minDist = Double.MAX_VALUE;
            int next = -1;
            for (int j = 0; j < n; j++) {
                if (!visited[j] && distances[current][j] < minDist) {
                    minDist = distances[current][j];
                    next = j;
                }
            }
            if (next != -1) {
                path.add(next);
                visited[next] = true;
                current = next;
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
