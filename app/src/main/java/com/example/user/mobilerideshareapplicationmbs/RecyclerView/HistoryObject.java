package com.example.user.mobilerideshareapplicationmbs.RecyclerView;

public class HistoryObject {
    private String rideId;
    private String time;
    private String userType;

    public HistoryObject(String rideId, String time, String userType ){
        this.rideId = rideId;
        this.time = time;
        this.userType = userType;
    }

    public String getRideId(){return rideId;}
    public void setRideId(String rideId) {
        this.rideId = rideId;
    }

    public String getTime(){return time;}
    public void setTime(String time) {
        this.time = time;
    }

    public String getUserType(){return userType;}
    public void setUserType(String userType) {
        this.userType = userType;
    }
}
