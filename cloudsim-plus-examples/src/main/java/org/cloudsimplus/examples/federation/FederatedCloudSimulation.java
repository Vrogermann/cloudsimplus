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
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import org.cloudbus.cloudsim.allocationpolicies.*;
import org.cloudbus.cloudsim.brokers.FederatedDatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.FederatedCloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.FederatedDatacenter;
import org.cloudbus.cloudsim.distributions.GammaDistr;
import org.cloudbus.cloudsim.distributions.LognormalDistr;
import org.cloudbus.cloudsim.distributions.UniformDistr;
import org.cloudbus.cloudsim.federation.CloudFederation;
import org.cloudbus.cloudsim.federation.FederationMember;
import org.cloudbus.cloudsim.federation.FederationMemberUser;
import org.cloudbus.cloudsim.hosts.FederatedHostSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostStateHistoryEntry;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.schedulers.cloudlet.FederatedCloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.util.BotFileReader;
import org.cloudbus.cloudsim.util.Conversion;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelConstant;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.vms.FederatedVmSimple;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudsimplus.builders.tables.*;
import org.cloudsimplus.traces.ufpel.BoT;
import org.cloudsimplus.traces.ufpel.ConvertedBoT;
import org.cloudsimplus.util.Log;
import org.cloudsimplus.util.*;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static org.cloudbus.cloudsim.util.BytesConversion.megaBytesToBytes;
import static org.cloudbus.cloudsim.util.MathUtil.positive;


public class FederatedCloudSimulation {
    private static final URL BOT_CSV_FILE = FederatedCloudSimulation.class.getClassLoader().getResource("workload/ufpel/outputconverted.csv");

    private static final Path RESULTS_LOCATION = Paths.get("cloudsim-plus-examples/src/main/resources/workload/ufpel/results");


    private static final List<Records.University> UNIVERSITIES_BASELINE =
        Arrays.asList(new Records.University("Universidade Federal do Rio de Janeiro",
                new Records.Coordinates(-22.862312050419078, -43.22317329523859),
                0, 1, 10, 10, 10,
                "UFRJ"),
            new Records.University("Universidade Federal de São Paulo",
                new Records.Coordinates(-23.598773, -46.643422),
                1, 1, 10, 10, 10,
                "UNIFESP"),
            new Records.University("Universidade Federal de Minas Gerais",
                new Records.Coordinates(-19.870581085957383, -43.967746630914675),
                2, 1, 10, 10, 10,
                "UFMG"),
            new Records.University("Universidade Federal do Rio Grande Do Sul",
                new Records.Coordinates(-30.033907564026826, -51.21900538654607),
                3, 1, 10, 10, 10,
                "UFRGS"),
            new Records.University("Universidade Federal de Santa Catarina",
                new Records.Coordinates(-26.23485949891767, -48.88401144670387),
                4, 1, 10, 10, 10,
                "UFSC"),
            new Records.University("Universidade Federal de São Carlos",
                new Records.Coordinates(-21.983975081254595, -47.88152180795102),
                5, 1, 10, 10, 10,
                "UFSCar"),
            new Records.University("Universidade Federal do Paraná",
                new Records.Coordinates(-25.426871793799748, -49.26175798375143),
                6, 1, 10, 10, 10,
                "UFPR"),
            new Records.University("Universidade Federal do Pernambuco",
                new Records.Coordinates(-8.01710961795856, -34.950500616736285),
                7, 1, 10, 10, 10,
                "UFPE"),
            new Records.University("Universidade Federal da Bahia",
                new Records.Coordinates(-13.00365838049915, -38.509963739614044),
                8, 1, 10, 10, 10,
                "UFBA"),
            new Records.University("Universidade Federal de Juiz de Fora",
                new Records.Coordinates(-21.776859501069005, -43.36904141993076),
                9, 1, 10, 10, 10,
                "UFJF"));

    private static final List<Records.ExecutionPlan> simulationExecutionPlanList =
        List.of(new Records.ExecutionPlan(UNIVERSITIES_BASELINE, "UNIVERSITIES_BASELINE",
                FederatedVmAllocationPolicyFirstFit.class),
            new Records.ExecutionPlan(UNIVERSITIES_BASELINE, "UNIVERSITIES_BASELINE",
                FederatedVmAllocationPolicyBestFit.class),
            new Records.ExecutionPlan(UNIVERSITIES_BASELINE, "UNIVERSITIES_BASELINE",
                FederatedVmAllocationPolicyWorstFit.class),
            new Records.ExecutionPlan(UNIVERSITIES_BASELINE, "UNIVERSITIES_BASELINE",
                FederatedVmAllocationPolicyDualLayerRoundRobin.class),
            new Records.ExecutionPlan(UNIVERSITIES_BASELINE, "UNIVERSITIES_BASELINE",
                FederatedVmAllocationPolicySingleLayerRoundRobin.class),
            new Records.ExecutionPlan(UNIVERSITIES_BASELINE, "UNIVERSITIES_BASELINE",
                FederatedVmAllocationPolicyLocalFirstRandom.class),
            new Records.ExecutionPlan(UNIVERSITIES_BASELINE, "UNIVERSITIES_BASELINE",
                FederatedVmAllocationPolicyRandom.class));
    private static final List<Records.ExecutionPlan> singleSimulation = Collections.singletonList(new Records.ExecutionPlan(UNIVERSITIES_BASELINE, "UNIVERSITIES_BASELINE",
        FederatedVmAllocationPolicyRandom.class));

    private static final int HOST_PES = 4;
    private static final int HOST_MIPS = 3450; // from 7zip sandy bridge benchmark on https://www.7-cpu.com/
    private static final int HOST_RAM = 8192; //in Megabytes
    private static final long HOST_BW = 10_000; //in Megabits/s
    private static final long HOST_STORAGE = 10_000; //in Megabytes
    private static final int CLOUDLET_PES = 1;
    private static final double MIN_TIME_BETWEEN_EVENTS = 0.00001;
    private static final boolean DATA_COLLECTION_ENABLED = true;

    private final CloudSim simulation;

    private final List<BoT> bagOfTasksList;

    public static void main(String[] args) {
        final Path baseResultPath = RESULTS_LOCATION.resolve(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).
            format(DateTimeFormatter.ISO_LOCAL_DATE_TIME).replace(':', '-') + "/");
        baseResultPath.toFile().mkdirs();

        singleSimulation.forEach(executionPlan -> {
            try {
                new FederatedCloudSimulation(executionPlan.universityList(), executionPlan.allocationPolicy(),
                    baseResultPath.resolve(executionPlan.name() + "/" + executionPlan.allocationPolicy().getSimpleName() + "/")
                );
            } catch (IOException | JoranException e) {
                throw new RuntimeException(e);
            }
        });

    }

    private FederatedCloudSimulation(List<Records.University> universities,
                                     Class<? extends FederatedVmAllocationPolicyAbstract> allocationPolicy, Path simulationInstanceResultPath) throws IOException, JoranException {
        /*Enables just some level of log messages.
          Make sure to import org.cloudsimplus.util.Log;*/
        final String STRATEGY_NAME = simulationInstanceResultPath.toFile().getName();
        final String SIMULATION_NAME = simulationInstanceResultPath.getParent().toFile().getName();
        final String SIMULATION_START_TIME = simulationInstanceResultPath.getParent().getParent().toFile().getName();
        System.setProperty("simulation_folder", String.format("%s/%s/%s", SIMULATION_START_TIME, SIMULATION_NAME, STRATEGY_NAME));
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        context.reset();

        configurator.doConfigure(LoggerContext.class.getClassLoader().getResource("logback.xml"));



        Log.setLevel(Level.INFO);


        simulation = new CloudSim(MIN_TIME_BETWEEN_EVENTS);
        CloudFederation federation = new CloudFederation(simulation,"Federal Universities of Brazil", 0L);
        OptionalLong lineLimit = universities.stream().mapToLong(university -> universities.size() * ((long) university.numberOfUsers() * university.BoTsPerUser()) + university.id()).max();
            bagOfTasksList = BotFileReader.readBoTFile(BOT_CSV_FILE.getFile(), lineLimit.isPresent() ? lineLimit.getAsLong() : null);
        universities.forEach(university -> {
            FederationMember federationMember = new FederationMember(university.name(), university.abbreviation(),
                university.id(), federation, university.coordinates());
            federationMember.setBotsPerUser(Long.valueOf(university.BoTsPerUser()));

            federation.addMember(federationMember);
            federationMember.setBroker(new FederatedDatacenterBrokerSimple(simulation, federationMember, federation));

            // Aqui é definida a política de mapeamento de cloudlet para VM. nesse caso estão sendo filtradas
            // VMs que pertençam ao usuário, e que tenham os campos de id de job e numero de task iguais.
            federationMember.getBroker().setVmEligibleForCloudletFunction((vm, cloudlet) ->
                cloudlet.getOwner().equals(vm.getVmOwner()) && vm.getBotJobId() != null
                    && vm.getBotJobId().equals(cloudlet.getBotJobId()) &&
                    Objects.equals(vm.getBotTaskNumber(), cloudlet.getBotTaskNumber()));
            federationMember.getBroker().setName("broker_" + university.abbreviation().replace(" ", "_"));


            try {
                federationMember.setDatacenters(Set.copyOf(createDatacenters(federationMember, university, allocationPolicy)));
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                     InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            List<FederatedCloudletSimple> cloudlets = createCloudlets(university, federationMember, universities);
            List<Vm> Vms = createVmList(cloudlets);
            federationMember.getBroker().submitVmList(Vms);
            federationMember.getBroker().submitCloudletList(cloudlets);
        });


        simulation.start();

        final List<FederatedCloudletSimple> finishedFederatedCloudlets =
            federation.getMembers().stream().flatMap(member -> member.getBroker().getCloudletFinishedList().stream()).
                map(cloudlet -> (FederatedCloudletSimple) cloudlet).toList();

        simulationInstanceResultPath.toFile().mkdirs();
        writeUniversityDataCsv(universities, simulationInstanceResultPath, false);


        writeTaskResultCsv(simulationInstanceResultPath, finishedFederatedCloudlets, false);


        writeJobResultCsv(simulationInstanceResultPath, finishedFederatedCloudlets, false);

        writeHostUsageDetailCsv(simulationInstanceResultPath, federation, false);


        writeDatacenterDataCsv(simulationInstanceResultPath, federation, false);


    }

    private static void writeDatacenterDataCsv(Path simulationInstanceResultPath, CloudFederation federation, boolean showOnStdOut) {
        if(!DATA_COLLECTION_ENABLED){
            return;
        }
        Path datacenterData = simulationInstanceResultPath.resolve("datacenterData.csv");
        try {

            // Cria um PrintStream redirecionado para o arquivo
            PrintStream printStream = new PrintStream(new FileOutputStream(datacenterData.toFile()));
            CsvTable csvTable = new CsvTable();
            csvTable.setColumnSeparator(",");
            csvTable.setPrintStream(printStream);
            new FederatedDatacenterHistoryTableBuilder(federation.getAllDatacenters(), csvTable).build();


            // Fechar o arquivo
            printStream.close();
        } catch (IOException e) {
            System.out.println("Erro ao escrever o arquivo " + datacenterData + " : " + e);
        }

        if(showOnStdOut){
            new FederatedDatacenterHistoryTableBuilder(federation.getAllDatacenters()).build();
        }
    }

    private static void writeHostUsageDetailCsv(Path simulationInstanceResultPath, CloudFederation federation, boolean showOnStdout) throws FileNotFoundException {
        if(!DATA_COLLECTION_ENABLED){
            return;
        }

        // dados de execução de cada host de cada datacenter
        List<Records.HostAverageCpuUsage> hostAverageCpuUsageFullList = new ArrayList<>();
        federation.getAllDatacenters().stream().parallel().forEach(datacenter ->
        {

            List<Records.HostAverageCpuUsage> currentDatacenterHostAverageCpuUsage = datacenter.getHostList().stream().parallel().map(host -> {


                double totalUsage = 0.0;
                double totalTime = 0;


                List<HostStateHistoryEntry> history = host.getStateHistory();
                double previousTime = 0;
                for (int i = 0; i < history.size(); i++) {
                    HostStateHistoryEntry currentEntry = history.get(i);



                    double timeDifference = currentEntry.getTime() - previousTime;
                    if(timeDifference < 0.1){
                        continue;
                    }

                    // Calculo da média ponderada
                    double cpuUsage = currentEntry.getAllocatedMips() / host.getTotalMipsCapacity();
                    totalUsage += cpuUsage * timeDifference;
                    totalTime += timeDifference;
                    previousTime = currentEntry.getTime();
                }

                 return  new Records.HostAverageCpuUsage(host,
                    totalTime > 0 ? totalUsage / totalTime : 0);

//                writeHostTimelineCsv(simulationInstanceResultPath, showOnStdout, host);


            }).toList();
            datacenter.setAverageCpuUsage(currentDatacenterHostAverageCpuUsage.stream().mapToDouble(Records.HostAverageCpuUsage::averageCpuUsage).average().orElse(0));
            hostAverageCpuUsageFullList.addAll(currentDatacenterHostAverageCpuUsage);
//            try {
//                writeHostAverageUsageCsv(getDatacenterHostAverageCpuUsagePrintStream(datacenter,
//                    simulationInstanceResultPath), false, currentDatacenterHostAverageCpuUsage);
//            } catch (FileNotFoundException e) {
//                throw new RuntimeException(e);
//            }

        });
        writeHostAverageUsageCsv(getFullHostAverageCpuUsagePrintStream(simulationInstanceResultPath),
            false,
            hostAverageCpuUsageFullList);


    }

    private static void writeHostAverageUsageCsv(PrintStream resultPrintStream, boolean showOnStdout, List<Records.HostAverageCpuUsage> usage) {
        CsvTable csvTable = new CsvTable();
        csvTable.setPrintStream(resultPrintStream);
        csvTable.setColumnSeparator(",");
        new FederatedDatacenterAverageHostUsageTableBuilder(usage, csvTable).
            setTitle(usage.get(0).host().getDatacenter().getName()).build();


        // Fechar o arquivo
        resultPrintStream.close();

        if (showOnStdout) {
            new FederatedDatacenterAverageHostUsageTableBuilder(usage).
                setTitle(usage.get(0).host().getDatacenter().getName()).build();

        }
    }
    private static void writeHostTimelineCsv(Path simulationInstanceResultPath, boolean showOnStdout, Host host) {
        try {
            PrintStream printStream = getHostUsageTimelinePrintStream(host, simulationInstanceResultPath);
            CsvTable csvTable = new CsvTable();
            csvTable.setPrintStream(printStream);
            csvTable.setColumnSeparator(",");
            new FederatedHostHistoryTableBuilder(host, csvTable).
                setTitle(((FederatedHostSimple) host).getName()).build();


            // Fechar o arquivo
            printStream.close();

        } catch (IOException e) {
            System.out.println("Erro ao escrever o arquivo do host  " + ((FederatedHostSimple) host).getName()
                + " : " + e);
        }
        if (showOnStdout) {
            new FederatedHostHistoryTableBuilder(host).setTitle(((FederatedHostSimple) host).getName()).build();
        }
    }

    private static void writeJobResultCsv(Path simulationInstanceResultPath,
                                          List<FederatedCloudletSimple> finishedFederatedCloudlets,
                                          boolean showOnStdOut) {
        if(!DATA_COLLECTION_ENABLED){
            return;
        }
        Path jobData = simulationInstanceResultPath.resolve("jobData.csv");
        ArrayList<ConvertedBoT> botList = finishedFederatedCloudlets.stream().
            map(FederatedCloudletSimple::getBoT).distinct().collect(Collectors.toCollection(ArrayList::new));
        try {

            // Cria um PrintStream redirecionado para o arquivo
            PrintStream printStream = new PrintStream(new FileOutputStream(jobData.toFile()));
            CsvTable csvTable = new CsvTable();
            csvTable.setPrintStream(printStream);
            csvTable.setColumnSeparator(",");

            new BoTTableBuilder(botList, csvTable).build();


            // Fechar o arquivo
            printStream.close();
        } catch (IOException e) {
            System.out.println("Erro ao escrever o arquivo " + jobData + " : " + e);
        }

        if (showOnStdOut) {
            new BoTTableBuilder(botList).build();
        }
    }

    private static void writeTaskResultCsv(Path simulationInstanceResultPath, List<FederatedCloudletSimple> finishedFederatedCloudlets, boolean showOnStdOut) {
        if(!DATA_COLLECTION_ENABLED){
            return;
        }
        Path taskData = simulationInstanceResultPath.resolve("taskData.csv");
        try {

            // Cria um PrintStream redirecionado para o arquivo
            PrintStream printStream = new PrintStream(new FileOutputStream(taskData.toFile()));
            CsvTable csvTable = new CsvTable();
            csvTable.setPrintStream(printStream);
            csvTable.setColumnSeparator(",");
            new FederatedCloudletsTableBuilder(finishedFederatedCloudlets, csvTable).build();


            // Fechar o arquivo
            printStream.close();
        } catch (IOException e) {
            System.out.println("Erro ao escrever o arquivo " + taskData + " : " + e);
        }

        if (showOnStdOut) {
            new FederatedCloudletsTableBuilder(finishedFederatedCloudlets).build();
        }
    }

    private static void writeUniversityDataCsv(List<Records.University> universities, Path simulationInstanceResultPath, boolean showOnStdOut) {
        if(!DATA_COLLECTION_ENABLED){
            return;
        }
        Path universitiesData = simulationInstanceResultPath.resolve("universitiesData.csv");

        try {

            // Cria um PrintStream redirecionado para o arquivo
            PrintStream printStream = new PrintStream(new FileOutputStream(universitiesData.toFile()));
            CsvTable csvTable = new CsvTable();
            csvTable.setPrintStream(printStream);
            csvTable.setColumnSeparator(",");
            new FederationTopologyTableBuilder(universities, csvTable).build();


            // Fechar o arquivo
            printStream.close();
        } catch (IOException e) {
            System.out.println("Erro ao escrever o arquivo " + universitiesData + " : " + e);
        }
        if (showOnStdOut) {
            new FederationTopologyTableBuilder(universities).build();
        }
    }

    private static PrintStream getDatacenterHostAverageCpuUsagePrintStream(Datacenter datacenter, Path baseResultPath) throws FileNotFoundException {
        Path datacenterFolder = baseResultPath.resolve("datacenters/" + datacenter.getName() + "/");
        datacenterFolder.toFile().mkdirs();

        Path hostHistoryDataCsv = datacenterFolder.resolve("hostAverageCpuUsage.csv");

        // Cria um PrintStream redirecionado para o arquivo
        return new PrintStream(new FileOutputStream(hostHistoryDataCsv.toFile()));
    }

    private static PrintStream getFullHostAverageCpuUsagePrintStream(Path baseResultPath) throws FileNotFoundException {

        Path hostFullCpuUsageCsv = baseResultPath.resolve("hostData.csv");

        // Cria um PrintStream redirecionado para o arquivo
        return new PrintStream(new FileOutputStream(hostFullCpuUsageCsv.toFile()));
    }


    private static PrintStream getHostUsageTimelinePrintStream(Host host, Path baseResultPath) throws FileNotFoundException {
        Path datacenterFolder = baseResultPath.resolve("datacenters/" + host.getDatacenter().getName() + "/hostUsageDetails/");
        datacenterFolder.toFile().mkdirs();

        Path hostHistoryDataCsv = datacenterFolder.resolve((((FederatedHostSimple) host).getName()
            + ".csv"));

        // Cria um PrintStream redirecionado para o arquivo
        return new PrintStream(new FileOutputStream(hostHistoryDataCsv.toFile()));
    }


    private List<Vm> createVmList(List<FederatedCloudletSimple> cloudlets) {
        return cloudlets.stream().map(cloudlet -> {
            final FederatedVmSimple vm = new FederatedVmSimple(HOST_MIPS,
                1,
                cloudlet.getOwner(),
                cloudlet.getBoT());
                vm.setRam(Math.min(HOST_RAM/4, (long) (positive(cloudlet.getBoT().getOriginalBoT().getTaskRamUsage(), Conversion.HUNDRED_PERCENT) * HOST_RAM)))
                .setBw(HOST_BW / HOST_PES).
                setSize(HOST_STORAGE / HOST_PES).setCloudletScheduler(new FederatedCloudletSchedulerTimeShared())
                .setSubmissionDelay(cloudlet.getSubmissionDelay());

            vm.setBotJobId(cloudlet.getBotJobId());
            vm.setBotTaskNumber(cloudlet.getBotTaskNumber());
            cloudlet.getBoT().addVm(vm);
            return vm;
        }).collect(Collectors.toList());
    }


    private List<FederatedDatacenter> createDatacenters(FederationMember federationMember,
                                                        Records.University university,
                                                        Class<? extends FederatedVmAllocationPolicyAbstract> allocationPolicy) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        List<FederatedDatacenter> datacenters = new ArrayList<>();
        for (int currentDatacenter = 0; currentDatacenter < university.datacenterAmount(); currentDatacenter++) {
            final List<FederatedHostSimple> hostList = new ArrayList<>();
            final String datacenterName = federationMember.getAbbreviation() + "_dc_" + currentDatacenter;
            for (int currentHost = 0; currentHost < university.hostsPerDatacenter(); currentHost++) {
                FederatedHostSimple host = createHost();
                host.setName(datacenterName + "_host_" + currentHost);
                hostList.add(host);
            }
             FederatedVmAllocationPolicyAbstract allocationPolicyInstance;
            if((allocationPolicy == FederatedVmAllocationPolicyRandom.class) ||
                (allocationPolicy == FederatedVmAllocationPolicyLocalFirstRandom.class)){
                allocationPolicyInstance = new FederatedVmAllocationPolicyRandom(federationMember,
                    federationMember.getFederation(),
                    new UniformDistr());
            }
            else{
                allocationPolicyInstance = allocationPolicy.getConstructor(FederationMember.class,
                    CloudFederation.class).newInstance(federationMember, federationMember.getFederation());
            }
            FederatedDatacenter federatedDatacenter = new FederatedDatacenter(simulation,
                hostList,
                allocationPolicyInstance,
                federationMember);
            federatedDatacenter.setName(datacenterName);
            datacenters.add(federatedDatacenter);
        }
        return datacenters;
    }


    private FederatedHostSimple createHost() {
        final List<Pe> peList = new ArrayList<>(HOST_PES);
        //List of Host's CPUs (bing Elements, PEs)
        for (int i = 0; i < HOST_PES; i++) {
            //Uses a PeProvisionerSimple by default to provision PEs for VMs
            peList.add(new PeSimple(HOST_MIPS));
        }

        /*
        Uses ResourceProvisionerSimple by default for RAM and BW provisioning
        and VmSchedulerSpaceShared for VM scheduling.
        */
        FederatedHostSimple federatedHostSimple = new FederatedHostSimple(HOST_RAM, HOST_BW, HOST_STORAGE, peList);
        federatedHostSimple.enableStateHistory();
        return federatedHostSimple;

    }


    private List<FederatedCloudletSimple> createCloudlets(Records.University university,
                                                          FederationMember federationMember, List<Records.University> universities) {

        int currentBotIndex = 0;
        final List<FederatedCloudletSimple> list = new ArrayList<>(university.BoTsPerUser());

        for (int currentUser = 0; currentUser < university.numberOfUsers(); currentUser++) {
            FederationMemberUser user = new FederationMemberUser(federationMember, (long) currentUser);


            for (int i = 0; i < university.BoTsPerUser(); i++) {
                int index = universities.size() * currentBotIndex + university.id();

                if (index >= bagOfTasksList.size()) {
                    throw new RuntimeException(String.format("Não há BoTs suficientes para executar a simulação, " +
                            "foram requisitados %d mas a lista de BoTs só contem %d itens.",
                        index,
                        bagOfTasksList.size()));
                }
                BoT currentBoT = bagOfTasksList.get(index);
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

    private String getLatencyValuesString(CloudFederation federation) {
        StringBuilder latencyValues = new StringBuilder();
        federation.getAllLatencyMaps().forEach((key, value) -> {
            latencyValues.append("distâncias entre ").append(key.getAbbreviation()).append(" e outros datacenters: ");
            value.forEach((key2, value2) -> latencyValues.append(key2.getAbbreviation()).append(": ").
                append(value2).append("; "));
            latencyValues.append('\n');
        });
        return latencyValues.toString();
    }
}
