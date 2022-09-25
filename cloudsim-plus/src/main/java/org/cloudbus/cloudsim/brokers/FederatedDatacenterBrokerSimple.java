/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */
package org.cloudbus.cloudsim.brokers;

import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.FederatedCloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.FederatedDatacenter;
import org.cloudbus.cloudsim.federation.CloudFederation;
import org.cloudbus.cloudsim.federation.FederationMember;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.FederatedVmSimple;
import org.cloudbus.cloudsim.vms.Vm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;


public class FederatedDatacenterBrokerSimple extends DatacenterBrokerSimple {

    Logger LOGGER = LoggerFactory.getLogger(FederatedDatacenterBrokerSimple.class.getSimpleName());
    private final FederationMember owner;
    private CloudFederation federation;


    /**
     * Comparator used to sort the list of VMs when selecting one to execute a cloudlet
     */
    private Comparator<FederatedVmSimple> vmComparator;
    private BiFunction<FederatedVmSimple, FederatedCloudletSimple, Boolean> vmEligibleForCloudletFunction;



    private BiFunction<FederatedDatacenter, Vm, Boolean> datacenterEligibleForVMFunction;
    private Comparator<FederatedDatacenter> datacenterForVmComparator;

    public BiFunction<FederatedDatacenter, Vm, Boolean> getDatacenterEligibleForVMFunction() {
        return datacenterEligibleForVMFunction;
    }

    public void setDatacenterEligibleForVMFunction(BiFunction<FederatedDatacenter, Vm, Boolean> datacenterEligibleForVMFunction) {
        this.datacenterEligibleForVMFunction = datacenterEligibleForVMFunction;
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
     */
    public FederatedDatacenterBrokerSimple(final CloudSim simulation,
                                           final String name,
                                           FederationMember owner) {
        super(simulation, name);
        this.owner = owner;
    }

    public FederatedDatacenterBrokerSimple(CloudSim simulation, FederationMember owner, CloudFederation federation, Comparator<FederatedDatacenter> datacenterCloudletComparator, BiFunction<FederatedDatacenter, FederatedCloudletSimple, Boolean> datacenterEligibleForCloudletFunction) {
        super(simulation);
        this.owner = owner;
        this.federation = federation;
    }


    @Override
    protected Datacenter defaultDatacenterMapper(final Datacenter lastDatacenter, final Vm vm) {
        if(!(vm instanceof FederatedVmSimple)){
            LOGGER.error("VM is not a FederatedVmSimple instance");
        }
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
        Looks for a VM owned by the user to host the cloudlet, it can be on a datacenter owned by the user's
        organization or from another member of the federation
     *
     * @param cloudlet {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    protected Vm defaultVmMapper(final Cloudlet cloudlet) {
        if (cloudlet.isBoundToVm()) {
            return cloudlet.getVm();
        }
        List<FederatedVmSimple> localVms = new ArrayList<>();
        owner.getDatacenters().forEach(datacenter-> localVms.addAll(datacenter.getVmList().stream().
            filter(vm -> vm instanceof FederatedVmSimple && ((FederatedVmSimple) vm).getVmOwner().
                equals(((FederatedCloudletSimple) cloudlet).getOwner()) &&
                vmEligibleForCloudletFunction.apply((FederatedVmSimple) vm, (FederatedCloudletSimple) cloudlet)).
            map(vm-> (FederatedVmSimple) vm).toList()));
        if(!localVms.isEmpty()){
            localVms.sort(vmComparator);
            return localVms.get(0);
        }

        // if no datacenter from the user organization is currently hosting a vm from this user,
        // expand search to datacenters owned by other members of the federation
        List<FederatedVmSimple> vmsOnOtherMembersDatacenter = federation.getMembers().stream().
            filter(federationMember -> !this.owner.equals(federationMember)).
            map(member -> new ArrayList<>(member.getDatacenters())).toList().stream()
            .reduce(new ArrayList<>(), (member, accumulator) -> {
                accumulator.addAll(member);
                return accumulator;
            }).stream().reduce(new ArrayList<FederatedVmSimple>(), (accumulator, datacenter) -> {
                accumulator.addAll(datacenter.getVmList());
                return accumulator;
            }, (list1, list2) -> {
                list1.addAll(list2);
                return list1;
            }).stream().filter((FederatedVmSimple vm) ->
                vm.getVmOwner().equals(((FederatedCloudletSimple) cloudlet).getOwner()) &&
                    vmEligibleForCloudletFunction.apply(((FederatedVmSimple) vm), (FederatedCloudletSimple) cloudlet)).
            toList();

        if(!vmsOnOtherMembersDatacenter.isEmpty()){
            vmsOnOtherMembersDatacenter.sort(vmComparator);
            return vmsOnOtherMembersDatacenter.get(0);
        }

        // if no VM can host the user cloudlet
        return Vm.NULL;


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
