package com.creativeapps.schoolbustracker.data.network.models;

public class Payload
{
    public double distance;
    public double time;
    public double lat;
    public double lng;
    public double speed;

    public Payload(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    public Payload(double lat, double lng, double speed) {
        this.lat = lat;
        this.lng = lng;
        this.speed = speed;
    }
}
