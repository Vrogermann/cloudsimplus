package org.cloudbus.cloudsim.vms;

import org.cloudbus.cloudsim.federation.FederationMemberUser;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletScheduler;
import org.cloudsimplus.util.Records;

public class FederatedVmSimple extends VmSimple{

    private final FederationMemberUser vmOwner;

    public FederatedVmSimple(Vm sourceVm, FederationMemberUser vmOwner) {
        super(sourceVm);
        this.vmOwner = vmOwner;
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


    public FederatedVmSimple(double mipsCapacity, long numberOfPes, FederationMemberUser vmOwner) {
        super(mipsCapacity, numberOfPes);
        this.vmOwner = vmOwner;
    }

    public FederatedVmSimple(double mipsCapacity, long numberOfPes, CloudletScheduler cloudletScheduler, FederationMemberUser vmOwner) {
        super(mipsCapacity, numberOfPes, cloudletScheduler);
        this.vmOwner = vmOwner;
    }

    public FederatedVmSimple(long id, double mipsCapacity, long numberOfPes, FederationMemberUser vmOwner) {
        super(id, mipsCapacity, numberOfPes);
        this.vmOwner = vmOwner;
    }

    public FederatedVmSimple(long id, long mipsCapacity, long numberOfPes, FederationMemberUser vmOwner) {
        super(id, mipsCapacity, numberOfPes);
        this.vmOwner = vmOwner;
    }
    public FederationMemberUser getVmOwner() {
        return vmOwner;
    }

    @Override
    public String toString(){
        return "VM "+this.vmOwner.getFederationMember().getName() + "/" + "from user " +this.vmOwner.getId();
    }

}
