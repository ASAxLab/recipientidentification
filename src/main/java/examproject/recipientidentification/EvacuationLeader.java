package examproject.recipientidentification;

public class EvacuationLeader {
    String id;
    double latitude;
    double longitude;
    double workload; // Value between 0 (no workload) and 1 (maximum workload)

    public EvacuationLeader(String id, double latitude,double longitude){
        this.id=id;
        this.latitude=latitude;
        this.longitude=longitude;

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
}
