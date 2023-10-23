package org.cloudsimplus.traces.ufpel;

import org.cloudbus.cloudsim.cloudlets.FederatedCloudletSimple;
import org.cloudbus.cloudsim.federation.FederationMemberUser;
import org.cloudsimplus.util.Records;

import java.util.ArrayList;
import java.util.List;

public class ConvertedBoT {
    private BoT originalBoT;
    private List<FederatedCloudletSimple> tasks;

    private FederationMemberUser owner;

    public BoT getOriginalBoT() {
        return originalBoT;
    }

    public List<FederatedCloudletSimple> getTasks() {
        return tasks;
    }

    public FederationMemberUser getOwner() {
        return owner;
    }



    public ConvertedBoT(BoT originalBoT, List<FederatedCloudletSimple> tasks, FederationMemberUser owner) {
        this.originalBoT = originalBoT;
        this.tasks = tasks;
        this.owner = owner;
    }

    public ConvertedBoT(BoT originalBoT, FederationMemberUser owner) {
        this.originalBoT = originalBoT;
        this.tasks = new ArrayList<>();
        this.owner = owner;
    }

    public void addTasks(List<FederatedCloudletSimple> tasksToAdd) {
        tasks.addAll(tasksToAdd);
    }

    public void addTask(FederatedCloudletSimple task) {
        this.tasks.add(task);
    }

}
