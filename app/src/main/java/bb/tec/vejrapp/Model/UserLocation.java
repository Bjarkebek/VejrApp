package bb.tec.vejrapp.Model;

import android.location.Location;
import bb.tec.vejrapp.MainActivity;

public class UserLocation {

    private double latitude;
    private double longitude;

    public void registerLocation(android.location.Location location){
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        MainActivity.locations.add(this);
    }

    public UserLocation() {
    }

    public UserLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

}
