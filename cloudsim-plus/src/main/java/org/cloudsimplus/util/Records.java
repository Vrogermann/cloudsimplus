package org.cloudsimplus.util;

import org.cloudbus.cloudsim.federation.FederationMember;

public class Records {
    public record Coordinates(Double latitude, Double longitude){}
    public record University(String name, Coordinates coordinates, Integer id){}
    public record FederationMemberUser(String id, FederationMember member) {}

}
