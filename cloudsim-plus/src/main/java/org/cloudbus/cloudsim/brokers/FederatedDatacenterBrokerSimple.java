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


    private final CloudFederation federation;


    /**
     * Comparator used to sort the list of VMs when selecting one to execute a cloudlet
     */
    private Comparator<FederatedVmSimple> vmComparator;
    private BiFunction<FederatedVmSimple, FederatedCloudletSimple, Boolean> vmEligibleForCloudletFunction;



    private Comparator<FederatedDatacenter> datacenterForVmComparator;





    public FederatedDatacenterBrokerSimple(final CloudSim simulation,
                                           FederationMember owner, CloudFederation federation) {
        super(simulation);
        this.owner = owner;
        this.federation = federation;
    }



    @Override
    protected Datacenter defaultDatacenterMapper(final Datacenter lastDatacenter, final Vm vm) {
        if(!(vm instanceof FederatedVmSimple)){
            LOGGER.error("VM is not a FederatedVmSimple instance");
        }
        if(!owner.getDatacenters().isEmpty()){
            return (Datacenter) owner.getDatacenters().toArray()[0];
        }

        // if no datacenters can host the user VM, look on datacenters from other members of the federation
        ArrayList<FederatedDatacenter> datacentersFromOtherMembers = owner.getDatacentersFromOtherMembers();
        if(!datacentersFromOtherMembers.isEmpty()){
            return datacentersFromOtherMembers.get(0);
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

        // TODO: talvez buscar em todos os datacenters faça mais sentido, pois buscando fora do datacenter local apenas
        //  se não houver nenhum disponível irá sobrecarregar as vms rodando em datacenters do usuário antes de
        //  espalhar para vms rodando em datacenters de outros membros da federação
        owner.getDatacenters().forEach(datacenter-> localVms.addAll(datacenter.getVmList().stream().
            filter(vm -> vm instanceof FederatedVmSimple &&
                vmEligibleForCloudletFunction.apply((FederatedVmSimple) vm, (FederatedCloudletSimple) cloudlet)).
            map(vm-> (FederatedVmSimple) vm).toList()));
        if(!localVms.isEmpty()){
            localVms.sort(vmComparator);
            return localVms.get(0);
        }

        // if no datacenter from the user organization is currently hosting a vm from this user,
        // expand search to datacenters owned by other members of the federation
        List<FederatedVmSimple> vmsOnOtherMembersDatacenter = owner.getDatacentersFromOtherMembers().stream().
            reduce(new ArrayList<FederatedVmSimple>(), (accumulator, datacenter) -> {
                accumulator.addAll(datacenter.getVmList());
                return accumulator;
            }, (list1, list2) -> {
                list1.addAll(list2);
                return list1;
            }).stream().filter((FederatedVmSimple vm) ->
                    vmEligibleForCloudletFunction.apply(vm, (FederatedCloudletSimple) cloudlet)).
            collect(Collectors.toList());

        if(!vmsOnOtherMembersDatacenter.isEmpty()){
            vmsOnOtherMembersDatacenter.sort(vmComparator);
            return vmsOnOtherMembersDatacenter.get(0);
        }

        // if no VM can host the user cloudlet
        return Vm.NULL;


    }


    public CloudFederation getFederation() {
        return federation;
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



    public void setVmEligibleForCloudletFunction(BiFunction<FederatedVmSimple, FederatedCloudletSimple, Boolean> vmEligibleForCloudletFunction) {
        this.vmEligibleForCloudletFunction = vmEligibleForCloudletFunction;
    }
}
