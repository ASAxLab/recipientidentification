package org.example;

import java.util.Random;

public class Main {

    public static void main(String[] args) {

        long seed = System.currentTimeMillis() ^ System.nanoTime() ^ Thread.currentThread().getId() ^ new Random().nextLong();

        seed = (long) 2.5;

        // scenario 6 seed 2.5
        // scenario 7 seed 3.7
        // scenario 8 seed 4.5
       // BuildingEvacuation buildingEvacuation1 = new BuildingEvacuation(false, seed);
        BuildingEvacuation buildingEvacuation2 = new BuildingEvacuation(true, seed);
    }
}
