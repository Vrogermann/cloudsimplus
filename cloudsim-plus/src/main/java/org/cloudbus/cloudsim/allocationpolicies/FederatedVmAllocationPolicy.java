/*
 * CloudSim Plus: A modern, highly-extensible and easier-to-use Framework for
 * Modeling and Simulation of Cloud Computing Infrastructures and Services.
 * http://cloudsimplus.org
 *
 *     Copyright (C) 2015-2018 Universidade da Beira Interior (UBI, Portugal) and
 *     the Instituto Federal de Educação Ciência e Tecnologia do Tocantins (IFTO, Brazil).
 *
 *     This file is part of CloudSim Plus.
 *
 *     CloudSim Plus is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     CloudSim Plus is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with CloudSim Plus. If not, see <http://www.gnu.org/licenses/>.
 */
package org.cloudbus.cloudsim.allocationpolicies;

import org.cloudbus.cloudsim.datacenters.FederatedDatacenter;
import org.cloudbus.cloudsim.federation.CloudFederation;
import org.cloudbus.cloudsim.federation.FederationMember;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.FederatedVmSimple;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;


public class FederatedVmAllocationPolicy extends VmAllocationPolicyAbstract {

    private final FederationMember owner;

    private final CloudFederation federation;

    public BiFunction<FederatedDatacenter, Vm, Boolean> getDatacenterEligibleForVMFunction() {
        return datacenterEligibleForVMFunction;
    }

    public void setDatacenterEligibleForVMFunction(BiFunction<FederatedDatacenter, Vm, Boolean> datacenterEligibleForVMFunction) {
        this.datacenterEligibleForVMFunction = datacenterEligibleForVMFunction;
    }

    public Comparator<FederatedDatacenter> getDatacenterForVmComparator() {
        return datacenterForVmComparator;
    }

    public void setDatacenterForVmComparator(Comparator<FederatedDatacenter> datacenterForVmComparator) {
        this.datacenterForVmComparator = datacenterForVmComparator;
    }

    private BiFunction<FederatedDatacenter, Vm, Boolean> datacenterEligibleForVMFunction;

    private Comparator<FederatedDatacenter> datacenterForVmComparator;

    private BiFunction<Host, Vm, Boolean> hostEligibleForVMFunction;

    public BiFunction<Host, Vm, Boolean> getHostEligibleForVMFunction() {
        return hostEligibleForVMFunction;
    }

    public void setHostEligibleForVMFunction(BiFunction<Host, Vm, Boolean> hostEligibleForVMFunction) {
        this.hostEligibleForVMFunction = hostEligibleForVMFunction;
    }

    public Comparator<Host> getHostForVmComparator() {
        return hostForVmComparator;
    }

    public void setHostForVmComparator(Comparator<Host> hostForVmComparator) {
        this.hostForVmComparator = hostForVmComparator;
    }

    private Comparator<Host> hostForVmComparator;


    public FederatedVmAllocationPolicy(FederationMember owner,
                                       CloudFederation federation,
                                       BiFunction<FederatedDatacenter, Vm, Boolean> datacenterEligibleForVMFunction,
                                       Comparator<FederatedDatacenter> datacenterForVmComparator, BiFunction<Host, Vm, Boolean> hostEligibleForVMFunction, Comparator<Host> hostForVmComparator) {
        this.owner = owner;
        this.federation = federation;
        this.datacenterEligibleForVMFunction = datacenterEligibleForVMFunction;
        this.datacenterForVmComparator = datacenterForVmComparator;
        this.hostEligibleForVMFunction = hostEligibleForVMFunction;
        this.hostForVmComparator= hostForVmComparator;
    }



    @Override
    protected Optional<Host> defaultFindHostForVm(final Vm vm) {


        if(!(vm instanceof FederatedVmSimple)){
            LOGGER.error("VM is not a FederatedVmSimple instance");
            throw new RuntimeException("FederatedDatacenter received non FederatedVmSimple instance");
        }
        FederationMember vmOwner = ((FederatedVmSimple) vm).getVmOwner().getFederationMember();
        // looks for the best datacenter of the user to place the VM
        List<FederatedDatacenter> datacentersFromUserThatCanSupportTheVm = vmOwner.getDatacenters().stream().filter(datacenter -> datacenterEligibleForVMFunction.apply(datacenter,vm)).
            collect(Collectors.toList());
        if(datacenterForVmComparator != null){
            datacentersFromUserThatCanSupportTheVm.sort(datacenterForVmComparator);
        }
        Optional<Host> eligibleHosts = getBestHostFromDatacenter(vm, datacentersFromUserThatCanSupportTheVm);
        if (eligibleHosts.isPresent()){
            return eligibleHosts;
        }
        // if no datacenters can host the user VM, look on datacenters from other members of the federation

        List<FederatedDatacenter> datacentersFromOtherMembersThatCanSupportTheVm =
            vmOwner.getDatacentersFromOtherMembers().stream().
                filter(datacenter -> datacenterEligibleForVMFunction.apply(datacenter, vm)).collect(Collectors.toList());
        if(datacenterForVmComparator != null){
            datacentersFromOtherMembersThatCanSupportTheVm.sort(datacenterForVmComparator);
        }
        return getBestHostFromDatacenter(vm, datacentersFromOtherMembersThatCanSupportTheVm);
    }

    private Optional<Host> getBestHostFromDatacenter(Vm vm, List<FederatedDatacenter> datacenters) {
        if(!datacenters.isEmpty()){
            FederatedDatacenter chosenDatacenter = datacenters.get(0);
            List<Host> eligibleHosts = chosenDatacenter.getHostList().stream().filter(host -> hostEligibleForVMFunction.apply(host, vm)).
                collect(Collectors.toList());
            if(hostForVmComparator != null){
                eligibleHosts.sort(hostForVmComparator);
            }
            if(!eligibleHosts.isEmpty()){
                return Optional.ofNullable(eligibleHosts.get(0));
            }
        }
        return Optional.empty();
    }

}
