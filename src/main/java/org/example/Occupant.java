package org.example;

public class Occupant {
    int id;
    String type;



    public Occupant(int id, String type) {
        this.id = id;
        this.type = type;

    }

    public int getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return " (" +type +" " +id+ ")";
    }
}
