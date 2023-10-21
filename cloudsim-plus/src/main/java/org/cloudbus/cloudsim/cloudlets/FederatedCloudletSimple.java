package org.cloudbus.cloudsim.cloudlets;

import org.cloudbus.cloudsim.federation.FederationMemberUser;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudsimplus.traces.ufpel.ConvertedBoT;
import org.cloudsimplus.util.Records;

public class FederatedCloudletSimple extends CloudletSimple{
    public FederationMemberUser getOwner() {
        return owner;
    }

    private final FederationMemberUser owner;

    private ConvertedBoT BoT;

    public ConvertedBoT getBoT() {
        return BoT;
    }

    public void setBoT(ConvertedBoT boT) {
        BoT = boT;
    }

    private String botJobId;

    public String getBotJobId() {
        return botJobId;
    }

    public void setBotJobId(String botJobId) {
        this.botJobId = botJobId;
    }

    public Long getBotTaskNumber() {
        return botTaskNumber;
    }

    public void setBotTaskNumber(Long botTaskNumber) {
        this.botTaskNumber = botTaskNumber;
    }

    private Long botTaskNumber;

    public FederatedCloudletSimple(long length, int pesNumber, UtilizationModel utilizationModel, FederationMemberUser owner) {
        super(length, pesNumber, utilizationModel);
        this.owner = owner;
    }

    public FederatedCloudletSimple(long length, int pesNumber, FederationMemberUser owner) {
        super(length, pesNumber);
        this.owner = owner;
    }

    public FederatedCloudletSimple(long length, long pesNumber, FederationMemberUser owner) {
        super(length, pesNumber);
        this.owner = owner;
    }

    public FederatedCloudletSimple(long id, long length, long pesNumber, FederationMemberUser owner) {
        super(id, length, pesNumber);
        this.owner = owner;
    }
}
