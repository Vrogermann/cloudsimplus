package org.cloudbus.cloudsim.federation;

import org.cloudbus.cloudsim.brokers.FederatedDatacenterBrokerSimple;
import org.cloudbus.cloudsim.datacenters.FederatedDatacenter;
import org.cloudsimplus.util.Records;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FederationMember {

    private final String abbreviation;
    private String name;
    private Integer id;
    private Set<FederatedDatacenter> datacenters;
    private CloudFederation federation;
    private FederatedDatacenterBrokerSimple broker;
    private Records.Coordinates coordinates;
    private List<FederationMemberUser> userList;

    /**
     * Creates a member of a Cloud Federation
     *
     * @param abbreviation
     * @param name         name of the member
     * @param id           of the member
     * @param datacenters  set of all the datacenters of the member
     * @param federation   federation the member is part of
     *                     if federation is non-null, also calls the method {@link CloudFederation#addMember(FederationMember)}}
     * @param coordinates
     */
    public FederationMember(String abbreviation, String name, Integer id, Set<FederatedDatacenter> datacenters, CloudFederation federation, Records.Coordinates coordinates) {
        this.abbreviation = abbreviation;
        this.name = name;
        this.id = id;
        this.datacenters = datacenters;
        this.federation = federation;
        this.coordinates = coordinates;
        if(federation != null){
            federation.addMember(this);
        }
        this.userList = new ArrayList<>();
    }

    public void addDatacenter(FederatedDatacenter datacenter){
        datacenter.setOwner(this);
        datacenters.add(datacenter);
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public FederatedDatacenterBrokerSimple getBroker() {
        return broker;
    }

    public void setBroker(FederatedDatacenterBrokerSimple broker) {
        this.broker = broker;
    }



    public CloudFederation getFederation() {
        return federation;
    }

    public void setFederation(CloudFederation federation) {
        this.federation = federation;
    }

    public Records.Coordinates getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(Records.Coordinates coordinates) {
        this.coordinates = coordinates;
    }

    public FederationMember(String name, String abbreviation, Integer id, CloudFederation federation, Records.Coordinates coordinates) {
        this.name = name;
        this.abbreviation = abbreviation;
        this.id = id;
        this.federation = federation;
        this.coordinates = coordinates;
        this.datacenters= new HashSet<>();
        this.userList = new ArrayList<>();
    }

    public Set<FederatedDatacenter> getDatacenters() {
        return datacenters;
    }

    public ArrayList<FederatedDatacenter> getDatacentersFromOtherMembers() {
        Set<FederationMember> members = new HashSet<>(federation.getMembers());
        members.remove(this);
        return members.stream().map(FederationMember::getDatacenters).reduce(new ArrayList<>()
            , (ArrayList<FederatedDatacenter> datacenterList, Set<FederatedDatacenter> datacenterSet) -> {
                datacenterList.addAll(datacenterSet);
                return datacenterList;
            },
            (ArrayList<FederatedDatacenter> accumulatedList1, ArrayList<FederatedDatacenter> accumulatedList2) ->
            {
                accumulatedList1.addAll(accumulatedList2);
                return accumulatedList1;
            });

    }

    public void setDatacenters(Set<FederatedDatacenter> datacenters) {
        this.datacenters = datacenters;
    }

    public List<FederationMemberUser> getUserList() {
        return userList;
    }

    public boolean addUser(FederationMemberUser user) {
        if (userList.contains(user)){
            return false;
        }
        userList.add(user);
        return true;
    }

    public boolean removeUser(FederationMemberUser user) {
        if (userList.contains(user)){
            return false;
        }
        userList.remove(user);
        return true;
    }

    public String getAbbreviation() {
        return abbreviation;
    }
}
