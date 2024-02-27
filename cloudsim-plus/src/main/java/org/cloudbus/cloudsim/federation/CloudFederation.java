package org.cloudbus.cloudsim.federation;

import org.apache.lucene.util.SloppyMath;
import org.cloudbus.cloudsim.datacenters.FederatedDatacenter;
import org.cloudsimplus.util.Records;

import java.util.*;

/**
Represents a Cloud federation, where different organizations can share datacenters
 */
public class CloudFederation {
    public Set<FederationMember> getMembers() {
        return members;
    }

    public  Map<FederationMember, Double> getMemberToMemberLatencyMap(FederationMember member) {
        return memberToMemberLatencyMap.get(member);
    }

    private final Map<FederationMember, Map<FederationMember, Double>> memberToMemberLatencyMap;

    private final double SPEED_OF_LIGHT = 200_000;

    private final Set<FederationMember> members;
    private String name;
    private Long id;

    public CloudFederation( String name, Long id) {
        this.name = name;
        this.id = id;
        this.members = new HashSet<>();
        memberToMemberLatencyMap = new HashMap<>();
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
        Map<FederationMember, Double> newMemberMap = new HashMap<>();
        memberToMemberLatencyMap.put(member, newMemberMap);

        // calcular a latência de comunicação do novo membro com os membros existentes
        Records.Coordinates newMemberCoordinates = member.getCoordinates();
        members.forEach((currentMember)->{
            Records.Coordinates currentMemberCoordinates = currentMember.getCoordinates();
            newMemberMap.put(currentMember, SloppyMath.haversin(newMemberCoordinates.latitude(),
                newMemberCoordinates.longitude(),
                currentMemberCoordinates.latitude(),
                currentMemberCoordinates.longitude())/ SPEED_OF_LIGHT * 2);
        });


        // salvar a latência no mapa dos outros membros
        memberToMemberLatencyMap.forEach((currentMember, currentMemberMap)->{
            currentMemberMap.computeIfAbsent(member, (m)-> newMemberMap.get(currentMember));
        });
        return true;
    }

    public List<FederatedDatacenter> getAllDatacenters(){
        return this.members.stream().map(FederationMember::getDatacenters).reduce(new ArrayList<>()
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
