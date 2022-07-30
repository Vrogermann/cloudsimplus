package org.cloudbus.cloudsim.federation;

import org.cloudbus.cloudsim.brokers.FederatedDatacenterBrokerSimple;
import org.cloudbus.cloudsim.datacenters.FederatedDatacenter;

import java.util.HashSet;
import java.util.Set;

public class FederationMember {

    private String name;
    private Integer id;
    private Set<FederatedDatacenter> datacenters;
    private CloudFederation federation;
    private FederatedDatacenterBrokerSimple broker;

    /**
     * Creates a member of a Cloud Federation
     * @param name name of the member
     * @param id of the member
     * @param datacenters set of all the datacenters of the member
     * @param federation federation the member is part of
     * if federation is non-null, also calls the method {@link CloudFederation#addMember(FederationMember)}}
     */
    public FederationMember(String name, Integer id, Set<FederatedDatacenter> datacenters, CloudFederation federation) {
        this.name = name;
        this.id = id;
        this.datacenters = datacenters;
        this.federation = federation;
        if(federation != null){
            federation.addMember(this);
        }
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

    public FederationMember(String name, Integer id, CloudFederation federation) {
        this.name = name;
        this.id = id;
        this.federation = federation;
        this.datacenters= new HashSet<>();
    }

    public Set<FederatedDatacenter> getDatacenters() {
        return datacenters;
    }

    public void setDatacenters(Set<FederatedDatacenter> datacenters) {
        this.datacenters = datacenters;
    }
}
