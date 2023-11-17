package org.cloudsimplus.util;

import org.cloudbus.cloudsim.federation.FederationMember;

public class Records {
    public record Coordinates(Double latitude, Double longitude){}
    public record University(String name, Coordinates coordinates, Integer id, Integer datacenterAmount, Integer hostsPerDatacenter, Integer numberOfUsers, Integer BoTsPerUser, String abbreviation){}
    public record FederationMemberUser(Long id, FederationMember member) {}

}
