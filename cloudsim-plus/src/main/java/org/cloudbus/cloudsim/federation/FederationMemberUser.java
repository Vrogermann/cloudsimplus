package org.cloudbus.cloudsim.federation;

import org.cloudbus.cloudsim.cloudlets.FederatedCloudletSimple;
import org.cloudsimplus.traces.ufpel.ConvertedBoT;

import java.util.ArrayList;
import java.util.List;

public class FederationMemberUser {

    public FederationMember getFederationMember() {
        return federationMember;
    }

    public Long getId() {
        return id;
    }

    public List<ConvertedBoT> getBots() {
        return bots;
    }

    private FederationMember federationMember;
    private Long id;
    private List<ConvertedBoT> bots;

    private final String name;

    public int addBoTs(List<ConvertedBoT> botsToAdd) {
        int totalAdded = 0;
        for(int currentBoT = 0; currentBoT< botsToAdd.size();currentBoT++){
            if(addBoT(botsToAdd.get(currentBoT))){
                totalAdded++;
            }
        }
        return totalAdded;
    }

    public boolean addBoT(ConvertedBoT task) {
        if (this.bots.contains(task)){
            return false;
        }
        this.bots.add(task);
        return true;
    }

    public FederationMemberUser(FederationMember federationMember, Long id, List<ConvertedBoT> bots) {
        this.federationMember = federationMember;
        this.id = id;
        this.bots = bots;
        this.name = federationMember.getAbbreviation() + " " + id;
    }

    public FederationMemberUser(FederationMember federationMember, Long id) {
        this.federationMember = federationMember;
        this.id = id;
        this.bots = new ArrayList<>();
        this.name = federationMember.getAbbreviation() + " " + id;
    }

    public String getName() {
        return name;
    }
}
