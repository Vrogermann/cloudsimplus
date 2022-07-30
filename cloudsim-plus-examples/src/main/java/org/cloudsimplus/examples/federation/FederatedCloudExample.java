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
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.brokers.FederatedDatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
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
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudsimplus.traces.ufpel.BoT;
import org.cloudsimplus.util.Log;

import java.util.*;

import static org.cloudbus.cloudsim.util.BytesConversion.megaBytesToBytes;
import static org.cloudbus.cloudsim.util.MathUtil.positive;


public class FederatedCloudExample {
    private static final String BOT_CSV_FILE = "workload/ufpel/bot.csv";
    private record Coordinates(Double latitude, Double longitude){}
    private record University(String name, Coordinates coordinates, Integer id){}
    private static final int UNIVERSITIES = 10;
    private static final List<String> UNIVERSITY_NAMES = Arrays.asList(
        "Universidade Federal do Rio de Janeiro",
        "Universidade Federal de São Paulo (UNIFESP)",
        "Universidade Federal de Minas Gerais",
        "Universidade Federal do Rio Grande Do Sul",
        "Universidade Federal de Santa Catarina",
        "Universidade Federal de São Carlos",
        "Universidade Federal do Paraná (UFPR)",
        "Universidade Federal do Pernambuco",
        "Universidade Federal da Bahia",
        "Universidade Federal de Juiz de Fora");
    private static final List<Integer> UNIVERSITY_DATACENTER_AMOUNT = Arrays.asList(
        1,
        2,
        3,
        4,
        4,
        3,
        2,
        1,
        3,
        2);
    private static final List<Coordinates> UNIVERSITY_COORDINATES = Arrays.asList(
        new Coordinates(-22.862312050419078, -43.22317329523859),
        new Coordinates(-23.598773, -46.643422),
        new Coordinates(-19.870581085957383, -43.967746630914675),
        new Coordinates(-30.033907564026826, -51.21900538654607),
        new Coordinates(-26.23485949891767, -48.88401144670387),
        new Coordinates(-21.983975081254595, -47.88152180795202),
        new Coordinates(-25.426871793799748, -49.26175798375143),
        new Coordinates(-8.01710961795856, -34.950500616736285),
        new Coordinates(-13.00365838049915, -38.509963739614044),
        new Coordinates(-21.776859501069005, -43.36904141993076));

    private static final List<Integer>  HOSTS_PER_DATACENTER = Arrays.asList(
        10,
        20,
        30,
        40,
        40,
        30,
        20,
        10,
        30,
        20);
    private static final int  HOST_PES = 4;
    private static final int  HOST_MIPS = 3450; // from 7zip sandy bridge benchmark on https://www.7-cpu.com/
    private static final int  HOST_RAM = 8192; //in Megabytes
    private static final long HOST_BW = 10_000; //in Megabits/s
    private static final long HOST_STORAGE = 10_000; //in Megabytes
    private static final int VM_PES = 1;

    private static final int CLOUDLETS = 2;
    private static final int CLOUDLET_PES = 1;
    private static final int CLOUDLET_LENGTH = 10_000;

    private final CloudSim simulation;
    private List<DatacenterBroker> brokers;
    private Map<Long,List<Vm>> datacenterVmList = new HashMap<>();
    private List<Cloudlet> cloudletList;
    private List<DatacenterSimple> datacenters;

    public static void main(String[] args) {
        new FederatedCloudExample();
    }

    private FederatedCloudExample() {
        /*Enables just some level of log messages.
          Make sure to import org.cloudsimplus.util.Log;*/
        Log.setLevel(Level.ALL);

        simulation = new CloudSim();
        Set<University> universities = new HashSet<>();
        CloudFederation federation = new CloudFederation("Federal Universities of Brazil",0L);
        for (int currentUniversity = 0; currentUniversity < UNIVERSITIES; currentUniversity++) {
            universities.add(new University(UNIVERSITY_NAMES.get(currentUniversity),UNIVERSITY_COORDINATES.get(currentUniversity),  currentUniversity));
        }
        universities.forEach(university->{
            FederationMember federationMember = new FederationMember(university.name, university.id, federation);
            federation.addMember(federationMember);
            federationMember.setDatacenters(Set.copyOf(createDatacentersAndBroker(federationMember)));

        });


        //Creates a broker that is a software acting on behalf a cloud customer to manage his/her VMs and Cloudlets
        //BrokerSimple irá alocar o cloudlet nas vms com politica round-robin
//        brokers = createBrokers(datacenters);
//        brokers.forEach(broker->broker.submitVmList(datacenterVmList.get(Long.valueOf(broker.getName()))));
        brokers.forEach(broker->broker.submitCloudletList(createCloudlets()));


      //  broker0.setVmMapper((cloudlet -> vmList.sort((vml))))

        simulation.start();

        final List<Cloudlet> finishedCloudlets = brokers.stream().
            map(DatacenterBroker::getCloudletFinishedList).
            reduce((accumulator, list)-> {
                accumulator.addAll(list);
                return accumulator;
        }).orElse(Collections.emptyList());
        new CloudletsTableBuilder(finishedCloudlets).build();
    }

    private List<DatacenterBroker> createBrokers(List<DatacenterSimple> datacenters){
        List<DatacenterBroker> brokers = new ArrayList<>();
        for(int currentDatacenter = 0; currentDatacenter < datacenters.size(); currentDatacenter++) {
            FederatedDatacenterBrokerSimple broker = new FederatedDatacenterBrokerSimple(simulation);
            brokers.add(broker);
            broker.setName(String.valueOf(currentDatacenter));
        }
        return brokers;
    }

    /**
     * creates an id for a federatedHost
     * @param federationMemberId id of the federation member
     * @param datacenterId id of the datacenter
     * @param hostId id of the host
     * @return a long mapping the first 16 bits to the host number, the next 16 to the datacenter number
     * and the next 16 to the federation member number
     */
    private long createHostId(long federationMemberId, long datacenterId, long hostId){
        return (long) (hostId + datacenterId * Math.pow(2,16) + federationMemberId * Math.pow(2,32));
    }
    /**
     * Creates a Datacenter and its Hosts, and one VM for each Host.
     */
    private List<FederatedDatacenter> createDatacentersAndBroker(FederationMember federationMember) {
        List<FederatedDatacenter> datacenters = new ArrayList<>();
        FederatedDatacenterBrokerSimple broker = new FederatedDatacenterBrokerSimple(simulation);
        federationMember.setBroker(broker);
        for(int currentDatacenter = 0; currentDatacenter < UNIVERSITY_DATACENTER_AMOUNT.get(federationMember.getId()); currentDatacenter++) {
            final List<Host> hostList = new ArrayList<>();

            for(int currentHost = 0; currentHost < HOSTS_PER_DATACENTER.get(federationMember.getId()); currentHost++) {
                Host host = createHost();
                host.setId(createHostId(federationMember.getId(), currentDatacenter, currentHost));
                hostList.add(host);
                host.createVm(createVm(host, currentDatacenter,federationMember.getId()));
            }

            //Uses a VmAllocationPolicySimple by default to allocate VMs
            //VmAllocationPolicySimple irá alocar o host com mais PEs livres para a VM
            FederatedDatacenter federatedDatacenter = new FederatedDatacenter(simulation, hostList, federationMember);
            federatedDatacenter.setName(String.valueOf(currentDatacenter));
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


    private Vm createVm(Host host, long datacenterId, long federationMemberId) {

        final Vm vm = new VmSimple(host.getMips(), host.getNumberOfPes());
        vm.setRam(host.getRam().getCapacity())
            .setBw(host.getBw().getCapacity())
            .setSize(host.getStorage().getCapacity()).
            setCloudletScheduler(new CloudletSchedulerTimeShared())
            .setDescription(String.format("%d,%d,%d",datacenterId, host.getId(), federationMemberId));
        return vm;
    }

    /**
     * Creates a list of Cloudlets.
     */
    private List<Cloudlet> createCloudlets() {
        final List<Cloudlet> list = new ArrayList<>(CLOUDLETS);

        //UtilizationModel defining the Cloudlets use only 50% of any resource all the time
        //a utilização de processamento de cada job será de 50%
        //como a função setUtilizationUpdateFunction não foi chamada, esse será o uso até o cloudlet terminar
        final UtilizationModelDynamic utilizationModel = new UtilizationModelDynamic(0.5);
        //utilizationModel.setUtilizationUpdateFunction((UtilizationModelDynamic modelDynamic)-> Math.abs(Math.sin(modelDynamic.getTimeSpan())));
        for (int i = 0; i < CLOUDLETS; i++) {
            final Cloudlet cloudlet = new CloudletSimple(CLOUDLET_LENGTH, CLOUDLET_PES, utilizationModel);
            cloudlet.setSizes(1024);
            list.add(cloudlet);

        }

        return list;
    }
    //0.0001554,0.06433,0.0625
    private Cloudlet createCloudlet(BoT bot){
        final long pesNumber = positive(bot.actualCpuCores(VM_PES), VM_PES);

        final double maxRamUsagePercent = positive(bot.getTaskRamUsage(), Conversion.HUNDRED_PERCENT);
        final UtilizationModelConstant utilizationRam = new UtilizationModelConstant(maxRamUsagePercent);

        final double sizeInMB    = bot.getTaskDiskUsage() * HOST_STORAGE + 1;
        final long   sizeInBytes = (long) Math.ceil(megaBytesToBytes(sizeInMB));
        return new CloudletSimple(CLOUDLET_LENGTH, pesNumber)
            .setFileSize(sizeInBytes)
            .setOutputSize(sizeInBytes)
            .setUtilizationModelBw(new UtilizationModelFull())
            .setUtilizationModelCpu(new UtilizationModelFull())
            .setUtilizationModelRam(utilizationRam);
    }
}
