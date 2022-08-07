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
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.*;
import java.util.function.BiFunction;


public class FederatedDatacenterBrokerSimple extends DatacenterBrokerSimple {


    private FederationMember owner;
    private CloudFederation federation;
    private Comparator<FederatedDatacenter> datacenterComparator;
    private Comparator<FederatedDatacenterBrokerSimple> datacenterBrokerComparator;

    private BiFunction<FederatedDatacenter, Cloudlet, Boolean> datacenterEligibleFunction;

    public Comparator<FederatedDatacenter> getDatacenterComparator() {
        return datacenterComparator;
    }

    public void setDatacenterComparator(Comparator<FederatedDatacenter> datacenterComparator) {
        this.datacenterComparator = datacenterComparator;
    }

    public BiFunction<FederatedDatacenter, Cloudlet, Boolean> getDatacenterEligibleFunction() {
        return datacenterEligibleFunction;
    }

    public void setDatacenterEligibleFunction(BiFunction<FederatedDatacenter, Cloudlet, Boolean> datacenterEligibleFunction) {
        this.datacenterEligibleFunction = datacenterEligibleFunction;
    }


    public FederatedDatacenterBrokerSimple(final CloudSim simulation,
                                           Comparator<FederatedDatacenter> datacenterComparator,
                                           Comparator<FederatedDatacenterBrokerSimple> datacenterBrokerComparator,
                                           FederationMember owner) {
        super(simulation);
        this.owner = owner;
    }

    /**
     * Creates a DatacenterBroker giving a specific name.
     * @param simulation the CloudSim instance that represents the simulation the Entity is related to
     * @param name the DatacenterBroker name
     * @param datacenterComparator the comparator for chosing
     * @param datacenterBrokerComparator
     */
    public FederatedDatacenterBrokerSimple(final CloudSim simulation,
                                           final String name,
                                           Comparator<FederatedDatacenter> datacenterComparator,
                                           Comparator<FederatedDatacenterBrokerSimple> datacenterBrokerComparator,
                                           FederationMember owner) {
        super(simulation, name);
        this.owner = owner;
        this.datacenterComparator = datacenterComparator;
        this.datacenterBrokerComparator = datacenterBrokerComparator;
    }

    public FederatedDatacenterBrokerSimple(CloudSim simulation, FederationMember owner, CloudFederation federation, Comparator<FederatedDatacenter> datacenterComparator, Comparator<FederatedDatacenterBrokerSimple> datacenterBrokerComparator, BiFunction<FederatedDatacenter, Cloudlet, Boolean> datacenterEligibleFunction) {
        super(simulation);
        this.owner = owner;
        this.federation = federation;
        this.datacenterComparator = datacenterComparator;
        this.datacenterBrokerComparator = datacenterBrokerComparator;
        this.datacenterEligibleFunction = datacenterEligibleFunction;
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
        Optional<FederatedDatacenter> result = owner.getDatacenters().stream().filter(datacenter -> datacenterId.equals(datacenter.getName())).
            findFirst();
        if(result.isPresent()){
            return result.get();
        }
        return Datacenter.NULL;
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

        // if no VM is able to host the cloudlet, search for VMs from other members of the federation
        List<FederatedDatacenter> datacenters = federation.getMembers().stream().
            filter(federationMember -> !this.owner.equals(federationMember)).
            map(member -> new ArrayList<>(member.getDatacenters())).toList().stream()
            .reduce(new ArrayList<>(),(member, accumulator)->{
                accumulator.addAll(member);
                return accumulator;
            });
        if(datacenterComparator != null)
            datacenters.sort(datacenterComparator);
        Optional<FederatedDatacenter> availableDatacenter = datacenters.stream().
            filter(datacenter-> datacenterEligibleFunction.apply(datacenter,cloudlet)).findFirst();
        return availableDatacenter.map(datacenter -> datacenter.getVmList().stream().
            min(datacenter.getVmComparator()).orElse(Vm.NULL))
            .orElse(Vm.NULL);
    }
    private List<Vm> sortVms(List<Vm> vms){
        return vms.stream().
            sorted((a, b) -> (int) (a.getHost().getCpuPercentUtilization() - b.getCpuPercentUtilization() * 100000)).toList();

    }
    protected boolean checkIfVmIsSuitableForCloudlet(Vm vm, Cloudlet cloudlet){
        return vm.getHost().getCpuPercentUtilization() < 1;
    }
    public double getAverageDatacenterCpuUtilization(){
        ArrayList<Host> hosts = new ArrayList<>(owner.getDatacenters().stream().map(Datacenter::getHostList).toList().stream().reduce(new ArrayList<>(),
            (acc, hostList) -> {
                acc.addAll(hostList);
                return acc;
            }));
        return hosts.stream().map(Host::getCpuPercentUtilization).
            reduce(0.0, (acc, percentage) -> acc + percentage / hosts.size());
    }
}
