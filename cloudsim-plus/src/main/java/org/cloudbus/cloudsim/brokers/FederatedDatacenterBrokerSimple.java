/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */
package org.cloudbus.cloudsim.brokers;

import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.FederatedDatacenter;
import org.cloudbus.cloudsim.federation.CloudFederation;
import org.cloudbus.cloudsim.federation.FederationMember;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


public class FederatedDatacenterBrokerSimple extends DatacenterBrokerAbstract {


    private FederationMember owner;
    private CloudFederation federation;
    /**
     * Index of the last VM selected from the {@link #getVmExecList()}
     * to run some Cloudlet.
     */

    private int lastSelectedVmIndex;

    /**
     * Index of the last Datacenter selected to place some VM.
     */
    private int lastSelectedDcIndex;

    /**
     * Creates a new DatacenterBroker.
     *
     * @param simulation the CloudSim instance that represents the simulation the Entity is related to
     */
    public FederatedDatacenterBrokerSimple(final CloudSim simulation) {
        this(simulation, "");

    }

    /**
     * Creates a DatacenterBroker giving a specific name.
     *
     * @param simulation the CloudSim instance that represents the simulation the Entity is related to
     * @param name the DatacenterBroker name
     */
    public FederatedDatacenterBrokerSimple(final CloudSim simulation, final String name) {
        super(simulation, name);
        this.lastSelectedVmIndex = -1;
        this.lastSelectedDcIndex = -1;
    }

    /**
     * {@inheritDoc}
     *
        Expects to receive one VM for each Host in the Datacenter, for simplifying the logic.
        Each VM will use all available resources of the Host.
     * @param lastDatacenter {@inheritDoc}
     * @param vm {@inheritDoc}
     * @return {@inheritDoc}
     * @see DatacenterBroker#setDatacenterMapper(java.util.function.BiFunction)
     * @see #setSelectClosestDatacenter(boolean)
     */
    @Override
    protected Datacenter defaultDatacenterMapper(final Datacenter lastDatacenter, final Vm vm) {
        String datacenterId = vm.getDescription().split(",")[0];
        return getDatacenterList().stream().filter(datacenter -> datacenterId.equals(datacenter.getName())).findFirst().orElse(Datacenter.NULL);

    }




    /**
     * {@inheritDoc}
     *
    checks if a cloudlet can be executed on a datacenter owned by this organization, and sends to a datacenter of another member of the federation if necessary
     *
     * @param cloudlet {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    protected Vm defaultVmMapper(final Cloudlet cloudlet) {
        if (cloudlet.isBoundToVm()) {
            return cloudlet.getVm();
        }

        if (getVmExecList().isEmpty()) {
            return Vm.NULL;
        }
        List<Vm> availableVms = getVmExecList();
        Optional<Vm> first = availableVms.stream().filter(vm -> vm.isSuitableForCloudlet(cloudlet)).findFirst();
        if(first.isPresent())
            return first.get();

        /*If the cloudlet isn't bound to a specific VM or the bound VM was not created,
        cyclically selects the next VM on the list of created VMs.*/
        lastSelectedVmIndex = ++lastSelectedVmIndex % getVmExecList().size();
        return getVmFromCreatedList(lastSelectedVmIndex);
    }
    private List<Vm> sortVms(List<Vm> vms){
        return vms.stream().
            sorted((a, b) -> (int) (a.getHost().getCpuPercentUtilization() - b.getCpuPercentUtilization() * 100000)).toList();

    }
    protected boolean checkIfVmIsSuitableForCloudlet(Vm vm, Cloudlet cloudlet){
        return vm.getHost().getCpuPercentUtilization() < 1;
    }
}
