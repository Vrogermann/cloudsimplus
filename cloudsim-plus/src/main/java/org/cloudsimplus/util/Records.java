package org.cloudsimplus.util;

public class Records {
    public record Coordinates(Double latitude, Double longitude){}
    public record University(String name, Coordinates coordinates, Integer id){}
}
