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
package org.cloudsimplus.examples.federation;

import ch.qos.logback.classic.Level;
import org.cloudbus.cloudsim.allocationpolicies.FederatedVmAllocationPolicy;
import org.cloudbus.cloudsim.brokers.FederatedDatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.FederatedCloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.FederatedDatacenter;
import org.cloudbus.cloudsim.federation.CloudFederation;
import org.cloudbus.cloudsim.federation.FederationMember;
import org.cloudbus.cloudsim.federation.FederationMemberUser;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.util.BotFileReader;
import org.cloudbus.cloudsim.util.Conversion;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelConstant;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.vms.FederatedVmSimple;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudsimplus.traces.ufpel.BoT;
import org.cloudsimplus.traces.ufpel.ConvertedBoT;
import org.cloudsimplus.util.Log;
import org.cloudsimplus.util.*;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static org.cloudbus.cloudsim.util.BytesConversion.megaBytesToBytes;
import static org.cloudbus.cloudsim.util.MathUtil.positive;


public class FederatedCloudExample {
    private static final URL BOT_CSV_FILE = FederatedCloudExample.class.getClassLoader().getResource("workload/ufpel/sampleBoTs.csv");
    private static final List<Records.University> UNIVERSITIES =
        Arrays.asList(new Records.University("Universidade Federal do Rio de Janeiro",
                new Records.Coordinates(-22.862312050419078, -43.22317329523859),
                0,
                1,
                1,
                8,
                1),
            new Records.University("Universidade Federal de São Paulo (UNIFESP)",
                new Records.Coordinates(-23.598773, -46.643422),
                1,
                1,
                100,
                1,
                4));
    private static final List<Records.University> UNIVERSITIES_FULL =
        Arrays.asList(new Records.University("Universidade Federal do Rio de Janeiro",
                new Records.Coordinates(-22.862312050419078, -43.22317329523859),
                0,
                1,
                10,
                5,
                1),
            new Records.University("Universidade Federal de São Paulo (UNIFESP)",
                new Records.Coordinates(-23.598773, -46.643422),
                1,
                2,
                20,
                10,
                1),
            new Records.University("Universidade Federal de Minas Gerais",
                new Records.Coordinates(-19.870581085957383, -43.967746630914675),
                2,
                3,
                30,
                15,
                1),
            new Records.University("Universidade Federal do Rio Grande Do Sul",
                new Records.Coordinates(-30.033907564026826, -51.21900538654607),
                3,
                4,
                40,
                10,
                1),
            new Records.University("Universidade Federal de Santa Catarina",
                new Records.Coordinates(-26.23485949891767, -48.88401144670387),
                4,
                4,
                40,
                5,
                1),
            new Records.University("Universidade Federal de São Carlos",
                new Records.Coordinates(-21.983975081254595, -47.88152180795202),
                5,
                3,
                30,
                10,
                1),
            new Records.University("Universidade Federal do Paraná (UFPR)",
                new Records.Coordinates(-25.426871793799748, -49.26175798375143),
                6,
                2,
                20,
                15,
                1),
            new Records.University("Universidade Federal do Pernambuco",
                new Records.Coordinates(-8.01710961795856, -34.950500616736285),
                7,
                1,
                10,
                10,
                1),
            new Records.University("Universidade Federal da Bahia",
                new Records.Coordinates(-13.00365838049915, -38.509963739614044),
                8,
                3,
                30,
                5,
                1),
            new Records.University("Universidade Federal de Juiz de Fora",
                new Records.Coordinates(-21.776859501069005, -43.36904141993076),
                9,
                1,
                20,
                2,
                50));

    private static final int HOST_PES = 4;
    private static final int HOST_MIPS = 3450; // from 7zip sandy bridge benchmark on https://www.7-cpu.com/
    private static final int HOST_RAM = 8192; //in Megabytes
    private static final long HOST_BW = 10_000; //in Megabits/s
    private static final long HOST_STORAGE = 10_000; //in Megabytes
    private static final int VM_PES = 1;

    private static final int CLOUDLETS = 73;
    private static final int CLOUDLET_PES = 1;
    private static final int CLOUDLET_LENGTH = 10_000;

    private final CloudSim simulation;

    private final List<BoT> bagOfTasksList;
    private int currentBotIndex = 0; // Initialize the bot index counter

    public static void main(String[] args) throws IOException {
        new FederatedCloudExample();
    }

    private FederatedCloudExample() throws IOException {
        /*Enables just some level of log messages.
          Make sure to import org.cloudsimplus.util.Log;*/
        Log.setLevel(Level.ALL);

        simulation = new CloudSim();
        CloudFederation federation = new CloudFederation("Federal Universities of Brazil", 0L);

        bagOfTasksList = BotFileReader.readBoTFile(BOT_CSV_FILE.getFile(), 500L);
        UNIVERSITIES.forEach(university -> {
            FederationMember federationMember = new FederationMember(university.name(), university.id(), federation, university.coordinates());

            federation.addMember(federationMember);
            federationMember.setBroker(new FederatedDatacenterBrokerSimple(simulation, federationMember, federation));

            // aqui é definida a política de mapeamento de cloudlet para VM. nesse caso estão sendo filtradas
            // VMs que pertençam ao usuário, e que tenham os campos de id de job e numero de task iguais.
            federationMember.getBroker().setVmEligibleForCloudletFunction((vm, cloudlet) ->
                cloudlet.getOwner().equals(vm.getVmOwner()) && vm.getBotJobId() != null
                    && vm.getBotJobId().equals(cloudlet.getBotJobId()) &&
                    Objects.equals(vm.getBotTaskNumber(), cloudlet.getBotTaskNumber()));
            federationMember.getBroker().setName("broker_" + university.name().replace(" ", "_"));

            federationMember.setDatacenters(Set.copyOf(createDatacenters(federationMember, university)));
            List<FederatedCloudletSimple> cloudlets = createCloudlets(university, federationMember);
            List<Vm> Vms = createVmList(cloudlets);
            federationMember.getBroker().submitVmList(Vms);
            federationMember.getBroker().submitCloudletList(cloudlets);
        });


        simulation.start();

        final List<Cloudlet> finishedCloudlets = federation.getMembers().stream().map(member -> member.getBroker().getCloudletFinishedList()).reduce((accumulator, list) -> {
            accumulator.addAll(list);
            return accumulator;
        }).orElse(Collections.emptyList());
        new CloudletsTableBuilder(finishedCloudlets).build();
    }

    private List<Vm> createVmList(List<FederatedCloudletSimple> cloudlets) {
        return cloudlets.stream().map(cloudlet -> {
            final FederatedVmSimple vm = new FederatedVmSimple(HOST_MIPS,
                1,
                cloudlet.getOwner(),
                cloudlet.getBoT() );
            vm.setRam(HOST_RAM / HOST_PES).
                setBw(HOST_BW / HOST_PES).
                setSize(HOST_STORAGE / 4).
                setCloudletScheduler(new CloudletSchedulerTimeShared())
                .setSubmissionDelay(cloudlet.getSubmissionDelay());
            vm.setBotJobId(cloudlet.getBotJobId());
            vm.setBotTaskNumber(cloudlet.getBotTaskNumber());
            return vm;
        }).collect(Collectors.toList());
    }


    private List<FederatedDatacenter> createDatacenters(FederationMember federationMember, Records.University university) {
        List<FederatedDatacenter> datacenters = new ArrayList<>();
        for (int currentDatacenter = 0; currentDatacenter < university.datacenterAmount(); currentDatacenter++) {
            final List<Host> hostList = new ArrayList<>();

            for (int currentHost = 0; currentHost < university.hostsPerDatacenter(); currentHost++) {
                Host host = createHost();
                hostList.add(host);
            }


            FederatedDatacenter federatedDatacenter = new FederatedDatacenter(simulation, hostList, federationMember);

            federatedDatacenter.
                setVmAllocationPolicy(vmAllocationFirstFit(federationMember));
            federatedDatacenter.setName(String.format("datacenter_%s:_number_%d", university.name().replace(" ", "_"), currentDatacenter));
            datacenters.add(federatedDatacenter);
        }
        return datacenters;
    }

    private FederatedVmAllocationPolicy vmAllocationFirstFit(FederationMember federationMember) {
        return new FederatedVmAllocationPolicy(federationMember,
            federationMember.getFederation(),
            (datacenter, vm) -> datacenter.getHostList().stream().anyMatch(host -> host.getFreePesNumber() >= vm.getExpectedFreePesNumber())
            , null, (host, vm) -> host.getFreePesNumber() >= vm.getExpectedFreePesNumber()
            && vm.getCurrentRequestedRam() <= host.getRam().getAvailableResource()
            && host.getAvailableStorage() >= vm.getStorage().getCapacity(), null);
    }


    private Host createHost() {
        final List<Pe> peList = new ArrayList<>(HOST_PES);
        //List of Host's CPUs (Processing Elements, PEs)
        for (int i = 0; i < HOST_PES; i++) {
            //Uses a PeProvisionerSimple by default to provision PEs for VMs
            peList.add(new PeSimple(HOST_MIPS));
        }

        /*
        Uses ResourceProvisionerSimple by default for RAM and BW provisioning
        and VmSchedulerSpaceShared for VM scheduling.
        */
        return new HostSimple(HOST_RAM, HOST_BW, HOST_STORAGE, peList);
    }


    private List<FederatedCloudletSimple> createCloudlets(Records.University university,
                                                          FederationMember federationMember) {
        int requiredCloudlets = currentBotIndex + (university.BoTsPerUser() * university.numberOfUsers());
        if (requiredCloudlets >= bagOfTasksList.size()) {
            throw new RuntimeException(String.format("Não há BoTs suficientes para executar a simulação, " +
                    "foram requisitados {0} mas a lista de BoTs só contem {1} itens.",
                requiredCloudlets,
                bagOfTasksList.size()));
        }


        final List<FederatedCloudletSimple> list = new ArrayList<>(university.BoTsPerUser());

        for (
            int currentUser = 0; currentUser < university.numberOfUsers(); currentUser++) {
            FederationMemberUser user = new FederationMemberUser(federationMember, (long) currentUser);


            for (int i = 0; i < university.BoTsPerUser(); i++) {
                BoT currentBoT = bagOfTasksList.get(currentBotIndex % bagOfTasksList.size());
                List<FederatedCloudletSimple> cloudlets = createAllCloudletsFromBoT(currentBoT, user);
                federationMember.addUser(user);
                list.addAll(cloudlets);

                currentBotIndex++;
            }
        }

        return list;
    }

    private List<FederatedCloudletSimple> createAllCloudletsFromBoT(BoT currentBoT, FederationMemberUser user) {
        List<FederatedCloudletSimple> cloudlets = new ArrayList<>(currentBoT.getNumberOfTasks().intValue());
        ConvertedBoT convertedBoT = new ConvertedBoT(currentBoT, user);
        for (int currentCloudlet = 0; currentCloudlet < currentBoT.getNumberOfTasks(); currentCloudlet++) {
            FederatedCloudletSimple cloudlet = createCloudletFromBoT(currentBoT, user);
            cloudlet.setBotJobId(currentBoT.getJobId());
            cloudlet.setBoT(convertedBoT);
            cloudlet.setBotTaskNumber((long) currentCloudlet);
            cloudlets.add(cloudlet);
            convertedBoT.addTask(cloudlet);
        }

        user.addBoT(convertedBoT);
        return cloudlets;
    }

    private FederatedCloudletSimple createCloudletFromBoT(BoT bot, FederationMemberUser user) {

        final double maxRamUsagePercent = positive(bot.getTaskRamUsage(), Conversion.HUNDRED_PERCENT);
        final UtilizationModelConstant utilizationRam = new UtilizationModelConstant(maxRamUsagePercent);
        final double sizeInMB = bot.getTaskDiskUsage() * HOST_STORAGE + 1;
        final UtilizationModelConstant utilizationModel = new UtilizationModelConstant(1);
        final long sizeInBytes = (long) Math.ceil(megaBytesToBytes(sizeInMB));
        final FederatedCloudletSimple cloudlet = new FederatedCloudletSimple(bot.getTaskLength().longValue(),
            CLOUDLET_PES,
            utilizationModel,
            user);
        cloudlet.setSubmissionDelay(bot.getJobStartTime());
        cloudlet.setSizes(sizeInBytes);
        cloudlet.setUtilizationModelRam(utilizationRam);
        cloudlet.setUtilizationModelBw(new UtilizationModelFull());
        return cloudlet;
    }
}
