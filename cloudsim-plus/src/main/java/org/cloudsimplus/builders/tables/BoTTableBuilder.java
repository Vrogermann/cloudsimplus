
package org.cloudsimplus.builders.tables;

import org.cloudbus.cloudsim.cloudlets.Cloudlet;

import org.cloudbus.cloudsim.core.Identifiable;
import org.cloudbus.cloudsim.hosts.FederatedHostSimple;
import org.cloudbus.cloudsim.vms.FederatedVmSimple;
import org.cloudsimplus.traces.ufpel.BoT;
import org.cloudsimplus.traces.ufpel.ConvertedBoT;

import java.util.List;

/*
 */
import java.util.List;

public class BoTTableBuilder extends TableBuilderAbstract<ConvertedBoT> {

    public BoTTableBuilder(final List<? extends ConvertedBoT> list) {
        super(list);
    }

    public BoTTableBuilder(final List<? extends ConvertedBoT> list, final Table table) {
        super(list, table);
    }

    @Override
    protected void createTableColumns() {
        addColumnDataFunction(getTable().addColumn("University Abbreviation"),
            (bot -> bot.getOwner().getFederationMember().getAbbreviation()));

        addColumnDataFunction(getTable().addColumn("Original User ID"),
            (bot -> bot.getOriginalBoT().getUserId()));

        addColumnDataFunction(getTable().addColumn("Original Job ID"),
            (bot -> bot.getOriginalBoT().getJobId()));

        addColumnDataFunction(getTable().addColumn("Original Number of Tasks"),
            (bot -> bot.getOriginalBoT().getNumberOfTasks()));

        addColumnDataFunction(getTable().addColumn("Original Task Length"),
            (bot -> bot.getOriginalBoT().getTaskLength()));

        addColumnDataFunction(getTable().addColumn("Original Task Time"),
            (bot -> bot.getOriginalBoT().getTaskTime()));

        addColumnDataFunction(getTable().addColumn("Original Task Disk Usage"),
            (bot -> bot.getOriginalBoT().getTaskDiskUsage()));

        addColumnDataFunction(getTable().addColumn("Original Task RAM Usage"),
            (bot -> bot.getOriginalBoT().getTaskRamUsage()));

        addColumnDataFunction(getTable().addColumn("Optimal Execution Time"),
            (bot -> {
                BoT originalBoT = bot.getOriginalBoT();
                double taskLength = originalBoT.getTaskLength();
                long numberOfTasks = originalBoT.getNumberOfTasks();
                double capacity = bot.getVms().get(0).getHost().getPeList().get(0).getCapacity();
                return (taskLength * numberOfTasks) / capacity;
            }));

        addColumnDataFunction(getTable().addColumn("Job Start Time"),
            (bot -> bot.getVms().get(0).getSubmissionDelay()));

        addColumnDataFunction(getTable().addColumn("Job Finish Time"),
            (bot -> bot.getVms().stream().mapToDouble(FederatedVmSimple::getStopTime).max().orElse(0)));

        addColumnDataFunction(getTable().addColumn("Total Job Time"),
            (bot -> bot.getVms().stream().mapToDouble(FederatedVmSimple::getStopTime).max().
                orElse(0) - bot.getVms().get(0).getSubmissionDelay()));

        addColumnDataFunction(getTable().addColumn("Average Task Execution Time"),
            (bot -> {
                double sumExecutionTime = bot.getVms().stream()
                    .mapToDouble(vm -> vm.getStopTime() - vm.getCreationTime())
                    .sum();
                return sumExecutionTime / bot.getVms().size();
            }));

        addColumnDataFunction(getTable().addColumn("Average Task Slowdown"),
            (bot -> {
                double sumSlowdown = bot.getTasks().stream()
                    .mapToDouble(task -> (task.getVm().getStopTime() - task.getVm().getCreationTime()) / task.getActualCpuTime())
                    .sum();
                return sumSlowdown / bot.getVms().size();
            }));

        addColumnDataFunction(getTable().addColumn("Job Slowdown"),
            (bot -> {
                double totalJobTime = bot.getVms().stream().mapToDouble(FederatedVmSimple::getStopTime).max().
                    orElse(0) - bot.getVms().get(0).getSubmissionDelay();

                BoT originalBoT = bot.getOriginalBoT();
                double taskLength = originalBoT.getTaskLength();
                long numberOfTasks = originalBoT.getNumberOfTasks();
                double capacity = bot.getVms().get(0).getHost().getPeList().get(0).getCapacity();
                double optimalExecutionTime = (taskLength * numberOfTasks) / capacity;


                return totalJobTime / optimalExecutionTime;
            }));
    }
}
