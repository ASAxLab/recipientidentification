package examproject.recipientidentification;

public class Notification {

    double latitude;
    double longitude;
    String type;
    int id;
  double elevation;

    String dangerLevel;
    public Notification(double latitude, double longitude, double elevation,String type, String dangerlevel,int id) {
        this.id=id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.elevation = elevation;
        this.type=type;
        this.dangerLevel=dangerlevel;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                ", type='" + type + '\'' +
                ", elevation=" + elevation +
                ", dangerLevel='" + dangerLevel + '\'' +
                '}';
    }

    public String getDangerLevel() {
        return dangerLevel;
    }

    public void setDangerLevel(String dangerLevel) {
        this.dangerLevel = dangerLevel;
    }
}
