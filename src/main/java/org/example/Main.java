package org.example;

import java.util.Random;

public class Main {

    public static void main(String[] args) {

        long seed = System.currentTimeMillis() ^ System.nanoTime() ^ Thread.currentThread().getId() ^ new Random().nextLong();


        BuildingEvacuation buildingEvacuation1 = new BuildingEvacuation(false, seed);
        BuildingEvacuation buildingEvacuation2 = new BuildingEvacuation(true, seed);
    }
}
