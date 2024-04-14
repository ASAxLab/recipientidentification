package examproject.recipientidentification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;


@SpringBootApplication
public class RecipientidentificationApplication {

	public static void main(String[] args) {
		// Array of leader counts to test
		int[] leaderCounts = {
				// Start with fine granularity for lower values
				5000, 7500, 10000, 15000, 20000, 30000, 40000, 50000, 60000, 70000, 80000, 90000,
				// Transition to medium steps
				100000, 120000, 140000, 160000, 180000, 200000, 225000, 250000, 275000, 300000, 350000, 400000, 450000, 500000,
				// Larger steps for higher values, yet more points than initially
				550000, 600000, 650000, 700000, 750000, 800000, 850000, 900000, 950000, 1000000, 1100000, 1200000, 1300000, 1400000, 1500000,
				1600000, 1700000, 1800000, 1900000, 2000000, 2250000, 2500000, 2750000, 3000000,
				// Continue trend for very large values with broader steps
				3500000, 4000000, 4500000, 5000000, 5500000, 6000000, 6500000, 7000000, 7500000, 8000000, 8500000, 9000000, 9500000,
				10000000, 11000000, 12000000, 13000000, 14000000, 15000000, 16000000, 17000000, 18000000, 19000000,
				20000000, 22000000, 24000000, 26000000, 28000000, 30000000, 32000000
		};


		Notification notification = new Notification(50, 25, 9, "Help request", "medium-threat", 1);

		// Loop through each leader count and test prioritizeLeaders
		for (int count : leaderCounts) {
			testTimeComplex(count, notification);
		}
	}


	public static void testTimeComplex(int count, Notification notification) {
		// Generate a list of leaders based on the count
		List<EvacuationLeader> leaders = generateLeaders(count);

		// Start timing immediately before the prioritizeLeaders call
		long startTime = System.nanoTime();
		prioritizeLeaders(leaders, notification);
		// End timing immediately after the prioritizeLeaders call
		long endTime = System.nanoTime();

		// Calculate duration based on the start and end times and convert to milliseconds
		long durationMs = (endTime - startTime) / 1_000_000; // 1,000,000 nanoseconds = 1 millisecond
		System.out.println("Execution time for " + count/1000 + " leaders: " + durationMs + " ms");
	}





	public static void testAccruacy(){

		List<EvacuationLeader> leaders = new ArrayList<>();
		Notification notification = new Notification(50, 25, 0, "Help request", "medium-threat", 1);

		leaders.add(new EvacuationLeader("Leader1", 48, 23, 3, 0.10));
		leaders.add(new EvacuationLeader("Leader2", 40, 27, 6, 0.90));
		leaders.add(new EvacuationLeader("Leader3", 51, 26, 18, 0.30));
		leaders.add(new EvacuationLeader("Leader4", 49, 24, 9, 0.40));
		leaders.add(new EvacuationLeader("Leader5", 53, 28, 0, 0.50));
		leaders.add(new EvacuationLeader("Leader6", 47, 22, 18, 0.60));
		leaders.add(new EvacuationLeader("Leader7", 46, 21, 15, 0.70));
		leaders.add(new EvacuationLeader("Leader8", 45, 20, 12, 0.80));
		leaders.add(new EvacuationLeader("Leader9", 55, 30, 3, 0.90));
		leaders.add(new EvacuationLeader("Leader10", 56, 31, 12, 1.00));


		prioritizeLeaders(leaders, notification);
		System.out.println("Priority list for notification: " +notification.toString());
		int rank = 1; // Start ranking from 1
		for (EvacuationLeader leader : leaders) {
			System.out.println("Rank " + rank + ": " + leader.toString());
			rank++; // Increment rank for the next leader
		}
	}


	private static List<EvacuationLeader> generateLeaders(int numberOfLeaders) {
		Random random = new Random();
		List<EvacuationLeader> leaders = new ArrayList<>();
		for (int i = 0; i < numberOfLeaders; i++) {
			// Example coordinates within the building's footprint and floors
			double x = 100 * random.nextDouble();
			double y = 50 * random.nextDouble();
			double z = 18 * random.nextDouble(); // Assuming 6 floors
			double workload = random.nextDouble(); // Workload between 0 and 1
			EvacuationLeader leader = new EvacuationLeader("Leader" + i, x, y, workload, z);
			leaders.add(leader);
		}
		return leaders;
	}

	private static double calculateDistance(double lat1, double lon1, double z1, double lat2, double lon2, double z2) {
		// Calculate the difference in each dimension
		double latDistance = Math.pow(lat2 - lat1, 2);
		double lonDistance = Math.pow(lon2 - lon1, 2);
		double zDistance = Math.pow(z2 - z1, 2);

		// Calculate 3D distance
		return Math.sqrt(latDistance + lonDistance + zDistance);
	}



	public static void prioritizeLeaders(List<EvacuationLeader> leaders, Notification notification) {
		// Adjust weights for distance and workload based on the danger level of the notification
		double distanceWeight, workloadWeight;

		if ("life-threatening".equals(notification.dangerLevel)) {
			// For life-threatening danger levels, prioritize proximity heavily
			distanceWeight = 1.0;
			workloadWeight = 0.001;
		} else if ("medium-threat".equals(notification.dangerLevel)) {
			// For high danger levels, balance between proximity and workload
			distanceWeight = 0.3;
			workloadWeight = 0.7;
		} else{
			// For less critical notifications, prioritize workload to distribute tasks more evenly
			distanceWeight = 0.2;
			workloadWeight = 0.8;
		}



		for (EvacuationLeader leader : leaders) {
			// Include the z coordinate in the distance calculation
			double distance = calculateDistance(leader.latitude, leader.longitude, leader.elevation,
					notification.latitude, notification.longitude, notification.elevation);
			double distanceScore = 1 / (distance + 1); // Adjusted to ensure no division by zero

			// Since a higher workload means more availability, it's used directly
			double workloadScore = leader.workload;

			// Calculate the overall priority score with adjusted weights
			leader.priorityScore = distanceWeight * distanceScore + workloadWeight * workloadScore;
		}



		// Sort leaders based on priority score in descending order, so higher scores are first
		leaders.sort((l1, l2) -> Double.compare(l2.priorityScore, l1.priorityScore));
	}




}
