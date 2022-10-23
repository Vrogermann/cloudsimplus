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
import org.cloudbus.cloudsim.brokers.FederatedDatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.cloudlets.FederatedCloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.FederatedDatacenter;
import org.cloudbus.cloudsim.federation.CloudFederation;
import org.cloudbus.cloudsim.federation.FederationMember;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.util.Conversion;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelConstant;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.vms.FederatedVmSimple;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudsimplus.traces.ufpel.BoT;
import org.cloudsimplus.util.Log;
import org.cloudsimplus.util.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.cloudbus.cloudsim.util.BytesConversion.megaBytesToBytes;
import static org.cloudbus.cloudsim.util.MathUtil.positive;


public class FederatedCloudExample {
    private static final String BOT_CSV_FILE = "workload/ufpel/bot.csv";
    private static final List<Records.University> UNIVERSITIES =
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

    public static void main(String[] args) {
        new FederatedCloudExample();
    }

    private FederatedCloudExample() {
        /*Enables just some level of log messages.
          Make sure to import org.cloudsimplus.util.Log;*/
        Log.setLevel(Level.ALL);

        simulation = new CloudSim();
        CloudFederation federation = new CloudFederation("Federal Universities of Brazil", 0L);

        UNIVERSITIES.forEach(university -> {
            FederationMember federationMember = new FederationMember(university.name(), university.id(), federation, university.coordinates());

            federation.addMember(federationMember);
            federationMember.setBroker(new FederatedDatacenterBrokerSimple(simulation, getFederatedDatacenterComparator(), getFederatedDatacenterBrokerComparator(), federationMember, federation));
            federationMember.getBroker().setDatacenterEligibleForVMFunction((datacenter, vm)-> datacenter.getHostList().stream().anyMatch(host-> host.getFreePesNumber() >= vm.getExpectedFreePesNumber()));
            federationMember.getBroker().setVmEligibleForCloudletFunction((vm, cloudlet)-> cloudlet.getOwner().equals(vm.getVmOwner()));
            federationMember.getBroker().setDatacenterForVmComparator(Comparator.comparingDouble(FederatedDatacenter::getAverageCpuPercentUtilization));
            federationMember.getBroker().setName(university.name().replace(" ", "_"));
            federationMember.setDatacenters(Set.copyOf(createDatacenters(federationMember, university)));
            List<FederatedCloudletSimple> cloudlets = createCloudlets(university, federationMember);
            List<Vm> Vms = createVmList(cloudlets);
            federationMember.getBroker().submitVmList(Vms);
            federationMember.getBroker().submitCloudletList(cloudlets);
        });


        //Creates a broker that is a software acting on behalf a cloud customer to manage his/her VMs and Cloudlets
        //BrokerSimple irá alocar o cloudlet nas vms com politica round-robin
//        brokers = createBrokers(datacenters);
//        brokers.forEach(broker->broker.submitVmList(datacenterVmList.get(Long.valueOf(broker.getName()))));
        //brokers.forEach(broker->broker.submitCloudletList(createCloudlets()));


        //  broker0.setVmMapper((cloudlet -> vmList.sort((vml))))

        simulation.start();

        final List<Cloudlet> finishedCloudlets = federation.getMembers().stream().map(member -> member.getBroker().getCloudletFinishedList()).reduce((accumulator, list) -> {
            accumulator.addAll(list);
            return accumulator;
        }).orElse(Collections.emptyList());
        new CloudletsTableBuilder(finishedCloudlets).build();
    }

    private List<Vm> createVmList(List<FederatedCloudletSimple> cloudlets) {
        return cloudlets.stream().map(cloudlet-> {
            final Vm vm = new FederatedVmSimple(HOST_MIPS, HOST_PES, cloudlet.getOwner());
            vm.setRam(HOST_RAM/ HOST_PES).setBw(HOST_BW/HOST_PES).setSize(HOST_STORAGE).setCloudletScheduler(new CloudletSchedulerTimeShared());
            return vm;
        }).collect(Collectors.toList());
    }

    private List<FederatedCloudletSimple> createCloudlets(Records.University university, FederationMember federationMember) {
        final List<FederatedCloudletSimple> list = new ArrayList<>(university.cloudletsPerUser());
        for(int currentUser = 0; currentUser < university.numberOfUsers(); currentUser++){
            Records.FederationMemberUser user = new Records.FederationMemberUser((long) currentUser, federationMember);
            final UtilizationModelConstant utilizationModel = new UtilizationModelConstant(1);
            for (int i = 0; i < university.cloudletsPerUser(); i++) {

                final FederatedCloudletSimple cloudlet = new FederatedCloudletSimple(CLOUDLET_LENGTH,
                    CLOUDLET_PES,
                    utilizationModel,
                    user);
                cloudlet.setSubmissionDelay(currentUser * university.cloudletsPerUser() + i);
                cloudlet.setSizes(1024);
                cloudlet.setUtilizationModelRam(new UtilizationModelConstant(1.0/ university.cloudletsPerUser()));
                cloudlet.setUtilizationModelBw(new UtilizationModelConstant(1.0/ university.cloudletsPerUser()));
                list.add(cloudlet);

            }


        }
        return list;
    }

    private List<FederatedVmSimple> createVMs(Records.University university, FederationMember federationMember) {
       return null;
    }


    /**
     * creates an id for a federatedHost
     *
     * @param federationMemberId id of the federation member
     * @param datacenterId       id of the datacenter
     * @param hostId             id of the host
     * @return a long mapping the first 16 bits to the host number, the next 16 to the datacenter number
     * and the next 16 to the federation member number
     */
    private long createHostId(long federationMemberId, long datacenterId, long hostId) {
        return (long) (hostId + datacenterId * Math.pow(2, 16) + federationMemberId * Math.pow(2, 32));
    }

    /**
     * Creates a Datacenter and its Hosts, and one VM for each Host.
     */
    public static Comparator<FederatedDatacenterBrokerSimple> getFederatedDatacenterBrokerComparator() {
        return Comparator.comparingDouble(FederatedDatacenterBrokerSimple::getAverageDatacenterCpuUtilization).reversed();
    }

    public static Comparator<FederatedDatacenter> getFederatedDatacenterComparator() {
        return Comparator.comparingDouble(FederatedDatacenter::getAverageCpuPercentUtilization).reversed();
    }

    private List<FederatedDatacenter> createDatacenters(FederationMember federationMember, Records.University university) {
        List<FederatedDatacenter> datacenters = new ArrayList<>();
        federationMember.setBroker(federationMember.getBroker());
        for (int currentDatacenter = 0; currentDatacenter < university.datacenterAmount(); currentDatacenter++) {
            final List<Host> hostList = new ArrayList<>();

            for (int currentHost = 0; currentHost < university.hostsPerDatacenter(); currentHost++) {
                Host host = createHost();
                hostList.add(host);
            }


            FederatedDatacenter federatedDatacenter = new FederatedDatacenter(simulation, hostList, federationMember);
            federatedDatacenter.setName(String.format("%s:%d",university.name().replace(" ","_"), currentDatacenter));
            datacenters.add(federatedDatacenter);
        }
        return datacenters;
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




    /**
     * Creates a list of Cloudlets.
     */
    private List<Cloudlet> createCloudlets() {
        final List<Cloudlet> list = new ArrayList<>(CLOUDLETS);


        final UtilizationModelDynamic utilizationModel = new UtilizationModelDynamic(0.5);
        //utilizationModel.setUtilizationUpdateFunction((UtilizationModelDynamic modelDynamic)-> Math.abs(Math.sin(modelDynamic.getTimeSpan())));
        for (int i = 0; i < CLOUDLETS; i++) {
            final Cloudlet cloudlet = new CloudletSimple(CLOUDLET_LENGTH, CLOUDLET_PES, utilizationModel);
            cloudlet.setSizes(1024);
            cloudlet.setUtilizationModelRam(new UtilizationModelConstant(0.1));
            cloudlet.setUtilizationModelBw(new UtilizationModelConstant(0.1));
            list.add(cloudlet);

        }

        return list;
    }

    //0.0001554,0.06433,0.0625
    private Cloudlet createCloudlet(BoT bot) {
        final long pesNumber = positive(bot.actualCpuCores(VM_PES), VM_PES);

        final double maxRamUsagePercent = positive(bot.getTaskRamUsage(), Conversion.HUNDRED_PERCENT);
        final UtilizationModelConstant utilizationRam = new UtilizationModelConstant(maxRamUsagePercent);

        final double sizeInMB = bot.getTaskDiskUsage() * HOST_STORAGE + 1;
        final long sizeInBytes = (long) Math.ceil(megaBytesToBytes(sizeInMB));
        return new CloudletSimple(CLOUDLET_LENGTH, pesNumber).setFileSize(sizeInBytes).setOutputSize(sizeInBytes).setUtilizationModelBw(new UtilizationModelFull()).setUtilizationModelCpu(new UtilizationModelFull()).setUtilizationModelRam(utilizationRam);
    }
}
