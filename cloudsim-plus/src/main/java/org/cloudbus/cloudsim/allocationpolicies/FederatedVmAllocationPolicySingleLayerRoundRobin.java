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


public class FederatedVmAllocationPolicySingleLayerRoundRobin extends FederatedVmAllocationPolicyAbstract {

    private final FederationMember owner;

    private List<Host> hostList;

    private int lastHostIndex;

    private final CloudFederation federation;

    public FederatedVmAllocationPolicySingleLayerRoundRobin(FederationMember owner,
                                                            CloudFederation federation) {
        this.owner = owner;
        this.federation = federation;
    }
    @Override
    public HostSuitability allocateHostForVm(final Vm vm) {
        if (federation.getAllDatacenters().stream().allMatch(federatedDatacenter -> federatedDatacenter.getHostList().isEmpty())) {
            LOGGER.error(
                "{}: {}: {} não pode ser alocada pois não há nenhum host no datacenter {}",
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

        LOGGER.warn("{}: {}: Nenhum host disponível para alocar {} no {}", vm.getSimulation().clockStr(), getClass().getSimpleName(), vm, this.getDatacenter().getName());
        return new HostSuitability("No suitable host found");
    }

    @Override
    protected Optional<Host> defaultFindHostForVm(final Vm vm) {
        if(!(vm instanceof FederatedVmSimple)){
            LOGGER.error("VM is not a FederatedVmSimple instance");
            throw new RuntimeException("FederatedDatacenter received non FederatedVmSimple instance");
        }

        if(hostList == null){
            hostList = owner.getFederation().getAllDatacenters().stream().
                flatMap(dc->dc.getHostList().stream()).collect(Collectors.toList());
        }

        double vmAllocationLatency = 0;
        final int maxTries = hostList.size();
        for (int i = 0; i < maxTries; i++) {
            final Host host = hostList.get(lastHostIndex);
            lastHostIndex = ++lastHostIndex % hostList.size();
            FederationMember hostOwner  = ((FederatedDatacenter) host.getDatacenter()).getOwner();
            Double latencyBetweenDCs = owner.getFederation().
                getMemberToMemberLatencyMap(hostOwner).get(this.owner);
            vmAllocationLatency += latencyBetweenDCs;
            if (host.isSuitableForVm(vm)) {
                ((FederatedDatacenter)getDatacenter()).updateTimeSpentFindingHostForVm(vm, vmAllocationLatency);
                return Optional.of(host);
            }
        }
        ((FederatedDatacenter)getDatacenter()).updateTimeSpentFindingHostForVm(vm, vmAllocationLatency);
        return Optional.empty();

    }



}
