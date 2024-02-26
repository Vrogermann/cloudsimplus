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

import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.FederatedDatacenter;
import org.cloudbus.cloudsim.federation.CloudFederation;
import org.cloudbus.cloudsim.federation.FederationMember;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSuitability;
import org.cloudbus.cloudsim.vms.FederatedVmSimple;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;


public class FederatedVmAllocationPolicyWorstFit extends VmAllocationPolicyAbstract {

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

    public FederatedVmAllocationPolicyWorstFit(FederationMember owner,
                                               CloudFederation federation) {
        this.owner = owner;
        this.federation = federation;
    }

    public FederatedVmAllocationPolicyWorstFit(FederationMember owner,
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
    public HostSuitability allocateHostForVm(final Vm vm) {
        if (federation.getAllDatacenters().stream().allMatch(federatedDatacenter -> federatedDatacenter.getHostList().isEmpty())) {
            LOGGER.error(
                "{}: {}: {} could not be allocated because there isn't any Host for Datacenter {}",
                vm.getSimulation().clockStr(), getClass().getSimpleName(), vm, getDatacenter().getId());
            return new HostSuitability("Datacenter has no host.");
        }

        if (vm.isCreated()) {
            return new HostSuitability("VM is already created");
        }

        final Optional<Host> optional = findHostForVm(vm);
        if (optional.filter(Host::isActive).isPresent()) {
            return allocateHostForVm(vm, optional.get());
        }

        LOGGER.warn("{}: {}: No suitable host found for {} in {}", vm.getSimulation().clockStr(), getClass().getSimpleName(), vm, this.getDatacenter());
        return new HostSuitability("No suitable host found");
    }

    @Override
    protected Optional<Host> defaultFindHostForVm(final Vm vm) {
        if(!(vm instanceof FederatedVmSimple)){
            LOGGER.error("VM is not a FederatedVmSimple instance");
            throw new RuntimeException("FederatedDatacenter received non FederatedVmSimple instance");
        }
        FederationMember vmOwner = ((FederatedVmSimple) vm).getVmOwner().getFederationMember();

        // finds the host with the least amount of free PEs owned by the federation member
        Optional<Host> hostFromUserThatCanSupportTheVm = vmOwner.getDatacenters().stream().
            flatMap(dc -> dc.getHostList().stream()).filter(host -> host.isSuitableForVm(vm)).
            min(Comparator.comparingLong(Host::getFreePesNumber));

        if (hostFromUserThatCanSupportTheVm.isPresent()){
            return hostFromUserThatCanSupportTheVm;
        }

        // if no datacenters can host the user VM, look on datacenters from other members of the federation
        return vmOwner.getDatacentersFromOtherMembers().stream().flatMap(dc -> dc.getHostList().stream()).
            filter(host -> host.isSuitableForVm(vm)).
            min(Comparator.comparingLong(Host::getFreePesNumber));

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



    public boolean defaultDatacenterEligibleForVMFunction(Datacenter datacenter, Vm vm){
        return datacenter.getHostList().stream().anyMatch(host-> host.isSuitableForVm(vm));
    }


}
