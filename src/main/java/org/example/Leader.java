package org.example;

import org.graphstream.graph.Path;

import java.util.ArrayList;

public class Leader extends Occupant {

    private Room roomLocation;
    private double WorkLoad;
    private double WorkloadNotification;
    private ArrayList<Room> assignedRooms;
    boolean isAvailable;
    private ArrayList<Room> FullPath; // Use ArrayList instead of Path for easier manipulation

    public Leader(int id, String type, Room roomLocation) {
        super(id, type);
        this.roomLocation = roomLocation;
        this.assignedRooms = new ArrayList<>();
        this.FullPath = new ArrayList<>();
    }

    public Room getRoomLocation() {
        return roomLocation;
    }

    public void setRoomLocation(Room roomLocation) {
        this.roomLocation = roomLocation;
    }

    public void setWorkLoad(double workLoad) {
        WorkLoad = workLoad;
    }

    public double getWorkLoad() {
        return WorkLoad;
    }

    public void setAssignedRooms(ArrayList<Room> assignedRooms) {
        this.assignedRooms = assignedRooms;
    }

    public ArrayList<Room> getAssignedRooms() {
        return assignedRooms;
    }

    public void setWorkloadNotification(double workloadNotification) {
        WorkloadNotification = workloadNotification;
    }

    public double getWorkloadNotification() {
        return WorkloadNotification;
    }

    public void setFullPath(ArrayList<Room> fullPath) {
        FullPath = fullPath;
    }

    public ArrayList<Room> getFullPath() {
        return FullPath;
    }

    @Override
    public int getId() {
        return super.getId();
    }

    @Override
    public String toString() {
        return super.toString() + " in " + roomLocation.getName();
    }
}
