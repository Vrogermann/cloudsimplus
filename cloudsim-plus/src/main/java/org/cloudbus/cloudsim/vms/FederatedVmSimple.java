package org.cloudbus.cloudsim.vms;

import org.cloudbus.cloudsim.cloudlets.FederatedCloudletSimple;
import org.cloudbus.cloudsim.federation.FederationMemberUser;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletScheduler;
import org.cloudsimplus.traces.ufpel.ConvertedBoT;

import java.util.Objects;

public class FederatedVmSimple extends VmSimple implements Vm{

    private final FederationMemberUser vmOwner;

    public ConvertedBoT getBoT() {
        return boT;
    }

    public FederatedCloudletSimple getTaskCloudlet(){
        return boT.getTasks().stream().filter(federatedCloudletSimple -> Objects.equals(federatedCloudletSimple.getBotTaskNumber(), getBotTaskNumber())).findFirst().orElse(null);
    }

    private final ConvertedBoT boT;
    public FederatedVmSimple(Vm sourceVm, FederationMemberUser vmOwner, ConvertedBoT boT) {
        super(sourceVm);
        this.vmOwner = vmOwner;
        this.boT = boT;
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


    public FederatedVmSimple(double mipsCapacity, long numberOfPes, FederationMemberUser vmOwner, ConvertedBoT boT) {
        super(mipsCapacity, numberOfPes);
        this.vmOwner = vmOwner;
        this.boT = boT;
    }

    public FederatedVmSimple(double mipsCapacity, long numberOfPes, CloudletScheduler cloudletScheduler, FederationMemberUser vmOwner, ConvertedBoT boT) {
        super(mipsCapacity, numberOfPes, cloudletScheduler);
        this.vmOwner = vmOwner;
        this.boT = boT;
    }

    public FederatedVmSimple(long id, double mipsCapacity, long numberOfPes, FederationMemberUser vmOwner, ConvertedBoT boT) {
        super(id, mipsCapacity, numberOfPes);
        this.vmOwner = vmOwner;
        this.boT = boT;
    }

    public FederatedVmSimple(long id, long mipsCapacity, long numberOfPes, FederationMemberUser vmOwner, ConvertedBoT boT) {
        super(id, mipsCapacity, numberOfPes);
        this.vmOwner = vmOwner;
        this.boT = boT;
    }
    public FederationMemberUser getVmOwner() {
        return vmOwner;
    }

    @Override
    public String toString(){
        return "VM "+this.vmOwner.getFederationMember().getAbbreviation() + "/" + "do usuário " +this.vmOwner.getId() + "/bot id "+ boT.getOriginalBoT().getJobId() + "/task index " + botTaskNumber;
    }

}
