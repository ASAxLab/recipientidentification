package org.example;

public class Notification {
    String type;
    String message;
    Room room;

    Occupant assignLeader;


    public Notification(String type,String message,Room location,String occupantName,Occupant assignedLeader){
        this.type=type;
        this.message = message;
        this.room = location;
        this.assignLeader= assignedLeader;
    }

    public Occupant getAssignLeader() {
        return assignLeader;
    }

    public Room getRoom() {
        return room;
    }

    public String getMessage() {
        return message;
    }

    public String getType() {
        return type;
    }

    public void setAssignLeader(Occupant assignLeader) {
        this.assignLeader = assignLeader;
    }


    public void setRoom(Room room) {
        this.room = room;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "type='" + type + '\'' +
                ", message='" + message + '\'' +
                ", location=" + room +
                ", assignLeader=" + assignLeader +
                '}';
    }
}
