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

    public int addTasks(List<FederatedCloudletSimple> tasksToAdd) {
        int totalAdded = 0;
        for(int currentTask = 0; currentTask< tasksToAdd.size();currentTask++){
            if(addTask(tasksToAdd.get(currentTask))){
                totalAdded++;
            }
        }
        return totalAdded;
    }

    public boolean addTask(FederatedCloudletSimple task) {
        if (this.tasks.contains(task)){
            return false;
        }
        this.tasks.add(task);
        return true;
    }

}
