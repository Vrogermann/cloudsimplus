package org.cloudbus.cloudsim.federation;

import java.util.HashSet;
import java.util.Set;
/**
Represents a Cloud federation, where different organizations can share datacenters
 */
public class CloudFederation {
    public Set<FederationMember> getMembers() {
        return members;
    }

    private final Set<FederationMember> members;
    private String name;
    private Long id;

    public CloudFederation( String name, Long id) {
        this.name = name;
        this.id = id;
        this.members = new HashSet<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * adds a member to the Cloud Federation
     * @param member member to add
     * @return true if the member was added, false if the member was already part of the federation
     */
    public boolean addMember(FederationMember member){
        if(members.contains(member)){
            return false;
        }
        members.add(member);
        member.setFederation(this);
        return true;
    }
    /**
     * removes a member from the Cloud Federation
     * @param member member to remove
     * @return true if the member was part of the federation, false otherwise
     */
    public boolean removeMember(FederationMember member){
        if(!members.contains(member)){
            return false;
        }
        members.remove(member);
        member.setFederation(null);
        return true;
    }


}
