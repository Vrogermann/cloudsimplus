package org.cloudsimplus.util;

import org.cloudbus.cloudsim.allocationpolicies.FederatedVmAllocationPolicyAbstract;
import org.cloudbus.cloudsim.federation.FederationMember;
import org.cloudbus.cloudsim.hosts.Host;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Records {
    public record Coordinates(Double latitude, Double longitude){}
    public record University(String name, Coordinates coordinates, Integer id, Integer datacenterAmount, Integer hostsPerDatacenter, Integer numberOfUsers, Integer BoTsPerUser, String abbreviation){}
    public record HostAverageCpuUsage(Host host, Double averageCpuUsage){}
    public record FederationMemberUser(Long id, FederationMember member) {}

    public record ExecutionPlan(List<University> universityList, String name, Class<? extends FederatedVmAllocationPolicyAbstract> allocationPolicy){}

}
