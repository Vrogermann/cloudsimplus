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
import org.cloudbus.cloudsim.distributions.ContinuousDistribution;
import org.cloudbus.cloudsim.federation.CloudFederation;
import org.cloudbus.cloudsim.federation.FederationMember;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSuitability;
import org.cloudbus.cloudsim.vms.FederatedVmSimple;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


public class FederatedVmAllocationPolicyRandom extends FederatedVmAllocationPolicyAbstract {

    private final FederationMember owner;

    private final CloudFederation federation;

    private final ContinuousDistribution random;

    private List<Host> hostList;

    public FederatedVmAllocationPolicyRandom(FederationMember owner,
                                             CloudFederation federation, ContinuousDistribution random) {
        this.owner = owner;
        this.federation = federation;
        this.random = random;
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

        double vmAllocationLatency = 0;
        FederationMember vmOwner = ((FederatedVmSimple) vm).getVmOwner().getFederationMember();
        if(hostList == null){
            hostList = vmOwner.getFederation().getAllDatacenters().stream().
                flatMap(dc->dc.getHostList().stream()).collect(Collectors.toList());
        }
        final int maxTriesLocal = hostList.size();
        for (int i = 0; i < maxTriesLocal; i++) {
            final int hostIndex = (int)(random.sample() * hostList.size());
            final Host host = hostList.get(hostIndex);
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
