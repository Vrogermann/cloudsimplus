package org.cloudbus.cloudsim.cloudlets;

import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudsimplus.util.Records;

public class FederatedCloudletSimple extends CloudletSimple{
    public Records.FederationMemberUser getOwner() {
        return owner;
    }

    private final Records.FederationMemberUser owner;
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

    public FederatedCloudletSimple(long length, int pesNumber, UtilizationModel utilizationModel, Records.FederationMemberUser owner) {
        super(length, pesNumber, utilizationModel);
        this.owner = owner;
    }

    public FederatedCloudletSimple(long length, int pesNumber, Records.FederationMemberUser owner) {
        super(length, pesNumber);
        this.owner = owner;
    }

    public FederatedCloudletSimple(long length, long pesNumber, Records.FederationMemberUser owner) {
        super(length, pesNumber);
        this.owner = owner;
    }

    public FederatedCloudletSimple(long id, long length, long pesNumber, Records.FederationMemberUser owner) {
        super(id, length, pesNumber);
        this.owner = owner;
    }
}
