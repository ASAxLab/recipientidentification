package examproject.recipientidentification;

public class EvacuationLeader {
    public double priorityScore;
    String id;
    double latitude;
    double longitude;
    double workload; // Value between 0 (no workload) and 1 (maximum workload)

    double elevation;
    int assignedFloor;

    public EvacuationLeader(String id, double latitude, double longitude, double elevation, double workload) {
        this.id=id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.workload = workload;
        this.elevation = elevation;
    }

    public int getAssignedFloor() {
        return assignedFloor;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getWorkload() {
        return workload;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setWorkload(double workload) {
        this.workload = workload;
    }

    public void setAssignedFloor(int assignedFloor) {
        this.assignedFloor = assignedFloor;
    }

    @Override
    public String toString() {
        return id + " - Workload: " + workload +
                ", Position: (" + latitude + ", " + longitude +", " +elevation+
                ", Priority Score: " + priorityScore;
    }

}

