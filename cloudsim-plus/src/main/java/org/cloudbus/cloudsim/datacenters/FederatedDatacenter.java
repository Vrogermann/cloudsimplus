/*
 * Title: CloudSim Toolkit Description: CloudSim (Cloud Simulation) Toolkit for Modeling and
 * Simulation of Clouds Licence: GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */
package org.cloudbus.cloudsim.datacenters;

import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicy;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.allocationpolicies.migration.VmAllocationPolicyMigration;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSimEntity;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.CustomerEntityAbstract;
import org.cloudbus.cloudsim.core.Simulation;
import org.cloudbus.cloudsim.core.events.PredicateType;
import org.cloudbus.cloudsim.core.events.SimEvent;
import org.cloudbus.cloudsim.federation.FederationMember;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.hosts.HostSuitability;
import org.cloudbus.cloudsim.network.IcmpPacket;
import org.cloudbus.cloudsim.power.models.PowerModelDatacenter;
import org.cloudbus.cloudsim.power.models.PowerModelDatacenterSimple;
import org.cloudbus.cloudsim.resources.DatacenterStorage;
import org.cloudbus.cloudsim.resources.SanStorage;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletScheduler;
import org.cloudbus.cloudsim.util.BytesConversion;
import org.cloudbus.cloudsim.util.MathUtil;
import org.cloudbus.cloudsim.util.TimeUtil;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.cloudsimplus.autoscaling.VerticalVmScaling;
import org.cloudsimplus.faultinjection.HostFaultInjection;
import org.cloudsimplus.listeners.DatacenterVmMigrationEventInfo;
import org.cloudsimplus.listeners.EventListener;
import org.cloudsimplus.listeners.HostEventInfo;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;


public class FederatedDatacenter extends DatacenterSimple {


    private FederationMember owner;
    private Comparator<Vm> vmComparator;

    public double getAverageCpuPercentUtilization(){
        List<Host> hosts = getHostList();
        return hosts.stream().map(Host::getCpuPercentUtilization).reduce(0.0,
            (accumulator, percentUtilization)->accumulator + percentUtilization / hosts.size());
    }

    public Comparator<Vm> getVmComparator() {
        return vmComparator;
    }

    public void setVmComparator(Comparator<Vm> vmComparator) {
        this.vmComparator = vmComparator;
    }

    public BiFunction<Vm, Cloudlet, Boolean> getVmEligibleFunction() {
        return vmEligibleFunction;
    }

    public void setVmEligibleFunction(BiFunction<Vm, Cloudlet, Boolean> vmEligibleFunction) {
        this.vmEligibleFunction = vmEligibleFunction;
    }

    private BiFunction<Vm, Cloudlet, Boolean> vmEligibleFunction;


    private double latitude;

    public FederatedDatacenter(Simulation simulation, List<? extends Host> hostList, FederationMember member) {
        super(simulation, hostList);
        owner = member;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    private double longitude;


    public FederationMember getOwner() {
        return owner;
    }

    public void setOwner(FederationMember owner) {
        this.owner = owner;
    }

    @Override
    protected boolean processCloudletSubmit(final SimEvent evt, final boolean ack) {
        final Cloudlet cloudlet = (Cloudlet) evt.getData();
        if (cloudlet.isFinished()) {
            notifyBrokerAboutAlreadyFinishedCloudlet(cloudlet, ack);
            return false;
        }
        // TODO check if current datacenter has resources to run this cloudlet, and submit to another if it doesn't
        cloudlet.assignToDatacenter(this);
        submitCloudletToVm(cloudlet, ack);
        return true;
    }
    /**
     * Submits a cloudlet to be executed inside its bind VM.
     *
     * @param cloudlet the cloudlet to the executed
     * @param ack indicates if the Broker is waiting for an ACK after the Datacenter
     * receives the cloudlet submission
     */
    private void submitCloudletToVm(final Cloudlet cloudlet, final boolean ack) {
        // time to transfer cloudlet's files
        final double fileTransferTime = getDatacenterStorage().predictFileTransferTime(cloudlet.getRequiredFiles());

        final CloudletScheduler scheduler = cloudlet.getVm().getCloudletScheduler();
        final double estimatedFinishTime = scheduler.cloudletSubmit(cloudlet, fileTransferTime);

        // if this cloudlet is in the exec queue
        if (estimatedFinishTime > 0.0 && !Double.isInfinite(estimatedFinishTime)) {
            send(this,
                getCloudletProcessingUpdateInterval(estimatedFinishTime),
                CloudSimTags.VM_UPDATE_CLOUDLET_PROCESSING);
        }

        ((CustomerEntityAbstract)cloudlet).setCreationTime();


        sendCloudletSubmitAckToBroker(cloudlet, ack);
    }

    private void notifyBrokerAboutAlreadyFinishedCloudlet(final Cloudlet cloudlet, final boolean ack) {
        LOGGER.warn(
            "{}: {} owned by {} is already completed/finished. It won't be executed again.",
            getName(), cloudlet, cloudlet.getBroker());

        /*
         NOTE: If a Cloudlet has finished, then it won't be processed.
         So, if ack is required, this method sends back a result.
         If ack is not required, this method don't send back a result.
         Hence, this might cause CloudSim to be hanged since waiting
         for this Cloudlet back.
        */
        sendCloudletSubmitAckToBroker(cloudlet, ack);

        sendNow(cloudlet.getBroker(), CloudSimTags.CLOUDLET_RETURN, cloudlet);
    }

    private void sendCloudletSubmitAckToBroker(final Cloudlet cloudlet, final boolean ack) {
        if(!ack){
            return;
        }

        sendNow(cloudlet.getBroker(), CloudSimTags.CLOUDLET_SUBMIT_ACK, cloudlet);
    }
    @Override
    public boolean isMigrationsEnabled() {
        return false;
    }

    public <T extends Vm> List<T> getVmList() {
        return (List<T>) Collections.unmodifiableList(
            getHostList().stream()
                .flatMap(h -> h.getVmList().stream())
                .collect(toList()));
    }


}
