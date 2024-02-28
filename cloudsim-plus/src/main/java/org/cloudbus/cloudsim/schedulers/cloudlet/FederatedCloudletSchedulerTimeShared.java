package org.cloudbus.cloudsim.schedulers.cloudlet;

import org.cloudbus.cloudsim.cloudlets.CloudletExecution;

public class FederatedCloudletSchedulerTimeShared extends CloudletSchedulerTimeShared {
    public double getCloudletEstimatedFinishTime() {
        if(getCloudletExecList().isEmpty()){
            return Double.MAX_VALUE;
        }
        CloudletExecution cle = getCloudletExecList().get(0);
        final double cloudletAllocatedMips = getAllocatedMipsForCloudlet(cle, getVm().getSimulation().clock());
        cle.setLastAllocatedMips(cloudletAllocatedMips);

        final double estimatedFinishTime = cle.getRemainingCloudletLength() / cle.getLastAllocatedMips();

        return Math.max(estimatedFinishTime, getVm().getSimulation().getMinTimeBetweenEvents());
    }
}
