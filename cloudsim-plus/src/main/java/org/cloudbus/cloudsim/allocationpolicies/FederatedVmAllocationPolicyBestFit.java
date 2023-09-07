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

/**
 * A Best Fit VmAllocationPolicy implementation that chooses, as
 * the host for a VM, the one with the most number of PEs in use,
 * which has enough free PEs for a VM.
 *
 * <p>This is a really computationally complex policy since the worst-case complexity
 * to allocate a Host for a VM is O(N), where N is the number of Hosts.
 * Such an implementation is not appropriate for large scale scenarios.</p>
 *
 * <p><b>NOTE: This policy doesn't perform optimization of VM allocation by means of VM migration.</b></p>
 *
 * @author Manoel Campos da Silva Filho
 * @since CloudSim Plus 3.0.1
 *
 * @see VmAllocationPolicyFirstFit
 * @see VmAllocationPolicySimple
 */
public class FederatedVmAllocationPolicyBestFit extends VmAllocationPolicyAbstract {
    /**
     * Gets the first suitable host from the {@link #getHostList()}
     * that has the most number of PEs in use (i.e. the least number of free PEs).
     * @return an {@link Optional} containing a suitable Host to place the VM or an empty {@link Optional} if not found
     */
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


    public FederatedVmAllocationPolicyBestFit(FederationMember owner,
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
        }
        // looks for the best datacenter of the user to place the VM
        List<FederatedDatacenter> datacentersFromUserThatCanSupportTheVm = owner.getDatacenters().stream().filter(datacenter -> datacenterEligibleForVMFunction.apply(datacenter,vm)).
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
            owner.getDatacentersFromOtherMembers().stream().
                filter(datacenter -> datacenterEligibleForVMFunction.apply(datacenter, vm)).collect(Collectors.toList());
        if(datacenterForVmComparator != null){
            datacentersFromOtherMembersThatCanSupportTheVm.sort(datacenterForVmComparator);
        }
        return getBestHostFromDatacenter(vm, datacentersFromOtherMembersThatCanSupportTheVm);
    }

    private Optional<Host> getBestHostFromDatacenter(Vm vm, List<FederatedDatacenter> datacentersFromOtherMembersThatCanSupportTheVm) {
        if(!datacentersFromOtherMembersThatCanSupportTheVm.isEmpty()){
            FederatedDatacenter chosenDatacenter = datacentersFromOtherMembersThatCanSupportTheVm.get(0);
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
