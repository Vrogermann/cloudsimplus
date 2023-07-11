package org.cloudbus.cloudsim.vms;

import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletScheduler;
import org.cloudsimplus.util.Records;

public class FederatedVmSimple extends VmSimple{

    private final Records.FederationMemberUser vmOwner;

    public FederatedVmSimple(Vm sourceVm, Records.FederationMemberUser vmOwner) {
        super(sourceVm);
        this.vmOwner = vmOwner;
    }

    public FederatedVmSimple(double mipsCapacity, long numberOfPes, Records.FederationMemberUser vmOwner) {
        super(mipsCapacity, numberOfPes);
        this.vmOwner = vmOwner;
    }

    public FederatedVmSimple(double mipsCapacity, long numberOfPes, CloudletScheduler cloudletScheduler, Records.FederationMemberUser vmOwner) {
        super(mipsCapacity, numberOfPes, cloudletScheduler);
        this.vmOwner = vmOwner;
    }

    public FederatedVmSimple(long id, double mipsCapacity, long numberOfPes, Records.FederationMemberUser vmOwner) {
        super(id, mipsCapacity, numberOfPes);
        this.vmOwner = vmOwner;
    }

    public FederatedVmSimple(long id, long mipsCapacity, long numberOfPes, Records.FederationMemberUser vmOwner) {
        super(id, mipsCapacity, numberOfPes);
        this.vmOwner = vmOwner;
    }
    public Records.FederationMemberUser getVmOwner() {
        return vmOwner;
    }

    @Override
    public String toString(){
        return "VM "+this.vmOwner.member().getName() + "/" + "from user " +this.vmOwner.id();
    }

}
