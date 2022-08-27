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
import java.util.stream.Collectors;


public class FederatedDatacenterBrokerSimple extends DatacenterBrokerSimple {


    private FederationMember owner;
    private CloudFederation federation;
    private Comparator<FederatedDatacenter> datacenterCloudletComparator;
    private Comparator<FederatedDatacenterBrokerSimple> datacenterBrokerComparator;
    private BiFunction<FederatedDatacenter, Cloudlet, Boolean> datacenterEligibleForCloudletFunction;
    private BiFunction<FederatedDatacenter, Vm, Boolean> datacenterEligibleForVMFunction;
    private Comparator<FederatedDatacenter> datacenterForVmComparator;

    public BiFunction<FederatedDatacenter, Vm, Boolean> getDatacenterEligibleForVMFunction() {
        return datacenterEligibleForVMFunction;
    }

    public void setDatacenterEligibleForVMFunction(BiFunction<FederatedDatacenter, Vm, Boolean> datacenterEligibleForVMFunction) {
        this.datacenterEligibleForVMFunction = datacenterEligibleForVMFunction;
    }


    public Comparator<FederatedDatacenter> getDatacenterCloudletComparator() {
        return datacenterCloudletComparator;
    }

    public void setDatacenterCloudletComparator(Comparator<FederatedDatacenter> datacenterCloudletComparator) {
        this.datacenterCloudletComparator = datacenterCloudletComparator;
    }

    public BiFunction<FederatedDatacenter, Cloudlet, Boolean> getDatacenterEligibleForCloudletFunction() {
        return datacenterEligibleForCloudletFunction;
    }

    public void setDatacenterEligibleForCloudletFunction(BiFunction<FederatedDatacenter, Cloudlet, Boolean> datacenterEligibleForCloudletFunction) {
        this.datacenterEligibleForCloudletFunction = datacenterEligibleForCloudletFunction;
    }


    public FederatedDatacenterBrokerSimple(final CloudSim simulation,
                                           Comparator<FederatedDatacenter> datacenterCloudletComparator,
                                           Comparator<FederatedDatacenterBrokerSimple> datacenterBrokerComparator,
                                           FederationMember owner) {
        super(simulation);
        this.owner = owner;
    }

    /**
     * Creates a DatacenterBroker giving a specific name.
     * @param simulation the CloudSim instance that represents the simulation the Entity is related to
     * @param name the DatacenterBroker name
     * @param datacenterCloudletComparator the comparator for chosing
     * @param datacenterBrokerComparator
     */
    public FederatedDatacenterBrokerSimple(final CloudSim simulation,
                                           final String name,
                                           Comparator<FederatedDatacenter> datacenterCloudletComparator,
                                           Comparator<FederatedDatacenterBrokerSimple> datacenterBrokerComparator,
                                           FederationMember owner) {
        super(simulation, name);
        this.owner = owner;
        this.datacenterCloudletComparator = datacenterCloudletComparator;
        this.datacenterBrokerComparator = datacenterBrokerComparator;
    }

    public FederatedDatacenterBrokerSimple(CloudSim simulation, FederationMember owner, CloudFederation federation, Comparator<FederatedDatacenter> datacenterCloudletComparator, Comparator<FederatedDatacenterBrokerSimple> datacenterBrokerComparator, BiFunction<FederatedDatacenter, Cloudlet, Boolean> datacenterEligibleForCloudletFunction) {
        super(simulation);
        this.owner = owner;
        this.federation = federation;
        this.datacenterCloudletComparator = datacenterCloudletComparator;
        this.datacenterBrokerComparator = datacenterBrokerComparator;
        this.datacenterEligibleForCloudletFunction = datacenterEligibleForCloudletFunction;
    }


    @Override
    protected Datacenter defaultDatacenterMapper(final Datacenter lastDatacenter, final Vm vm) {
        String datacenterId = vm.getDescription().split(",")[0];

        // looks for the best datacenter of the user to place the VM
        List<FederatedDatacenter> datacentersFromUserThatCanSupportTheVm = owner.getDatacenters().stream().filter(datacenter -> datacenterEligibleForVMFunction.apply(datacenter,vm)).
            collect(Collectors.toList());
        if(datacenterForVmComparator != null){
            datacentersFromUserThatCanSupportTheVm.sort(datacenterForVmComparator);
        }
        if(!datacentersFromUserThatCanSupportTheVm.isEmpty()){
            return datacentersFromUserThatCanSupportTheVm.get(0);
        }
        // if no datacenters can host the user VM, look on datacenters from other members of the federation
        Set<FederationMember> members = new HashSet<>(federation.getMembers());
        members.remove(owner);
        ArrayList<FederatedDatacenter> datacentersFromOtherMembers =
            members.stream().map(FederationMember::getDatacenters).reduce(new ArrayList<FederatedDatacenter>()
            , (ArrayList<FederatedDatacenter> datacenterList, Set<FederatedDatacenter> datacenterSet) -> {
                datacenterList.addAll(datacenterSet);
                return datacenterList;
            },
            (ArrayList<FederatedDatacenter> accumulatedList1, ArrayList<FederatedDatacenter> accumulatedList2) ->
            {
                accumulatedList1.addAll(accumulatedList2);
                return accumulatedList1;
            });
        List<FederatedDatacenter> datacentersFromOtherMembersThatCanSupportTheVm = datacentersFromOtherMembers.stream().filter(datacenter -> datacenterEligibleForVMFunction.apply(datacenter, vm)).toList();
        if(datacenterForVmComparator != null){
            datacentersFromOtherMembersThatCanSupportTheVm.sort(datacenterForVmComparator);
        }
        if(!datacentersFromOtherMembersThatCanSupportTheVm.isEmpty()){
            return datacentersFromOtherMembersThatCanSupportTheVm.get(0);
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

        if(first.isPresent()) {
            return first.get();
        }

        // if no VM is able to host the cloudlet, search for VMs from other members of the federation
        List<FederatedDatacenter> datacenters = federation.getMembers().stream().
            filter(federationMember -> !this.owner.equals(federationMember)).
            map(member -> new ArrayList<>(member.getDatacenters())).toList().stream()
            .reduce(new ArrayList<>(),(member, accumulator)->{
                accumulator.addAll(member);
                return accumulator;
            });

        if(datacenterCloudletComparator != null) {
            datacenters.sort(datacenterCloudletComparator);
        }

        Optional<FederatedDatacenter> availableDatacenter = datacenters.stream().
            filter(datacenter-> datacenterEligibleForCloudletFunction.apply(datacenter,cloudlet)).findFirst();

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

    public Comparator<FederatedDatacenter> getDatacenterForVmComparator() {
        return datacenterForVmComparator;
    }

    public void setDatacenterForVmComparator(Comparator<FederatedDatacenter> datacenterForVmComparator) {
        this.datacenterForVmComparator = datacenterForVmComparator;
    }
}
