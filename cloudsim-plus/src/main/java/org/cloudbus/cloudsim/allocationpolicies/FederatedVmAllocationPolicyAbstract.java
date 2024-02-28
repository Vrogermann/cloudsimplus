/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */
package org.cloudbus.cloudsim.allocationpolicies;

import org.cloudbus.cloudsim.allocationpolicies.migration.VmAllocationPolicyMigration;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSuitability;
import org.cloudbus.cloudsim.provisioners.ResourceProvisioner;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.Processor;
import org.cloudbus.cloudsim.resources.ResourceManageable;
import org.cloudbus.cloudsim.schedulers.MipsShare;
import org.cloudbus.cloudsim.util.Conversion;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmGroup;
import org.cloudsimplus.autoscaling.VerticalVmScaling;

import java.util.*;
import java.util.function.BiFunction;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * An abstract class that represents the policy
 * used by a {@link Datacenter} to choose a {@link Host} to place or migrate
 * a given {@link Vm}. It supports two-stage commit of reservation of
 * hosts: first, we reserve the Host and, once committed by the customer, the VM is
 * effectively allocated to that Host.
 *
 * <p>Each {@link Datacenter} must to have its own instance of a {@link VmAllocationPolicy}.</p>
 *
 * @author Rodrigo N. Calheiros
 * @author Anton Beloglazov
 * @author Manoel Campos da Silva Filho
 * @since CloudSim Toolkit 1.0
 */
public abstract class FederatedVmAllocationPolicyAbstract implements VmAllocationPolicy {
    /**
     * WARNING: the function should not be called directly because it may be null.
     * Use the {@link #findHostForVm(Vm)} instead.
     *
     * @see #setFindHostForVmFunction(BiFunction)
     */
    private BiFunction<VmAllocationPolicy, Vm, Optional<Host>> findHostForVmFunction;

    /**
     * @see #getDatacenter()
     */
    private Datacenter datacenter;

    /**@see #getHostCountForParallelSearch() */
    private int hostCountForParallelSearch;

    /**
     * Creates a VmAllocationPolicy.
     */
    public FederatedVmAllocationPolicyAbstract() {
        this(null);
    }

    /**
     * Creates a VmAllocationPolicy, changing the {@link BiFunction} to select a Host for a Vm.
     *
     * @param findHostForVmFunction a {@link BiFunction} to select a Host for a given Vm.
     * @see VmAllocationPolicy#setFindHostForVmFunction(BiFunction)
     */
    public FederatedVmAllocationPolicyAbstract(final BiFunction<VmAllocationPolicy, Vm, Optional<Host>> findHostForVmFunction) {
        setDatacenter(Datacenter.NULL);
        setFindHostForVmFunction(findHostForVmFunction);
        this.hostCountForParallelSearch = DEF_HOST_COUNT_PARALLEL_SEARCH;
    }

    @Override
    public final <T extends Host> List<T> getHostList() {
        return datacenter.getHostList();
    }

    @Override
    public Datacenter getDatacenter() {
        return datacenter;
    }

    /**
     * Sets the Datacenter associated to the Allocation Policy
     *
     * @param datacenter the Datacenter to set
     */
    @Override
    public final void setDatacenter(final Datacenter datacenter) {
        this.datacenter = requireNonNull(datacenter);
    }

    @Override
    public boolean scaleVmVertically(final VerticalVmScaling scaling) {
        if (scaling.isVmUnderloaded()) {
            return downScaleVmVertically(scaling);
        }

        if (scaling.isVmOverloaded()) {
            return upScaleVmVertically(scaling);
        }

        return false;
    }

    /**
     * Performs the up scaling of Vm resource associated to a given scaling object.
     *
     * @param scaling the Vm's scaling object
     * @return true if the Vm was overloaded and the up scaling was performed, false otherwise
     */
    private boolean upScaleVmVertically(final VerticalVmScaling scaling) {
        return isRequestingCpuScaling(scaling) ? scaleVmPesUpOrDown(scaling) : upScaleVmNonCpuResource(scaling);
    }

    /**
     * Performs the down scaling of Vm resource associated to a given scaling object.
     *
     * @param scaling the Vm's scaling object
     * @return true if the down scaling was performed, false otherwise
     */
    private boolean downScaleVmVertically(final VerticalVmScaling scaling) {
        return isRequestingCpuScaling(scaling) ? scaleVmPesUpOrDown(scaling) : downScaleVmNonCpuResource(scaling);
    }

    /**
     * Performs the up or down scaling of Vm {@link Pe}s,
     * depending if the VM is under or overloaded.
     *
     * @param scaling the Vm's scaling object
     * @return true if the scaling was performed, false otherwise
     * @see #upScaleVmVertically(VerticalVmScaling)
     */
    private boolean scaleVmPesUpOrDown(final VerticalVmScaling scaling) {
        final double numberOfPesForScaling = scaling.getResourceAmountToScale();
        if (numberOfPesForScaling == 0) {
            return false;
        }

        if (scaling.isVmOverloaded() && isNotHostPesSuitableToUpScaleVm(scaling)) {
            showResourceIsUnavailable(scaling);
            return false;
        }

        final Vm vm = scaling.getVm();
        vm.getHost().getVmScheduler().deallocatePesFromVm(vm);
        final int signal = scaling.isVmUnderloaded() ? -1 : 1;
        //Removes or adds some capacity from/to the resource, respectively if the VM is under or overloaded
        vm.getProcessor().sumCapacity((long) numberOfPesForScaling * signal);

        vm.getHost().getVmScheduler().allocatePesForVm(vm);
        return true;
    }

    private boolean isNotHostPesSuitableToUpScaleVm(final VerticalVmScaling scaling) {
        final Vm vm = scaling.getVm();
        final long numberOfPesForScaling = (long)scaling.getResourceAmountToScale();
        final MipsShare additionalVmMips = new MipsShare(numberOfPesForScaling, vm.getMips());
        return !vm.getHost().getVmScheduler().isSuitableForVm(vm, additionalVmMips);
    }

    /**
     * Checks if the scaling object is in charge of scaling CPU resource.
     *
     * @param scaling the Vm scaling object
     * @return true if the scaling is for CPU, false if it is
     * for any other kind of resource
     */
    private boolean isRequestingCpuScaling(final VerticalVmScaling scaling) {
        return Processor.class.equals(scaling.getResourceClass());
    }

    /**
     * Performs the up scaling of a Vm resource that is anything else than CPU.
     *
     * @param scaling the Vm's scaling object
     * @return true if the up scaling was performed, false otherwise
     * @see #scaleVmPesUpOrDown(VerticalVmScaling)
     * @see #upScaleVmVertically(VerticalVmScaling)
     */
    private boolean upScaleVmNonCpuResource(final VerticalVmScaling scaling) {
        final Class<? extends ResourceManageable> resourceClass = scaling.getResourceClass();
        final ResourceManageable hostResource = scaling.getVm().getHost().getResource(resourceClass);
        final double extraAmountToAllocate = scaling.getResourceAmountToScale();
        if (!hostResource.isAmountAvailable(extraAmountToAllocate)) {
            return false;
        }

        final ResourceProvisioner provisioner = scaling.getVm().getHost().getProvisioner(resourceClass);
        final ResourceManageable vmResource = scaling.getVm().getResource(resourceClass);
        final double newTotalVmResource = (double) vmResource.getCapacity() + extraAmountToAllocate;
        if (!provisioner.allocateResourceForVm(scaling.getVm(), newTotalVmResource)) {
            showResourceIsUnavailable(scaling);
            return false;
        }

        LOGGER.info(
            "{}: {}: {} mais {} foram alocados para o {}: a nova capacidade é {}. O uso percentual de cpu é {}%",
            scaling.getVm().getSimulation().clockStr(),
            scaling.getClass().getSimpleName(),
            (long) extraAmountToAllocate, resourceClass.getSimpleName(),
            scaling.getVm(), vmResource.getCapacity(),
            vmResource.getPercentUtilization() * 100);
        return true;
    }

    private void showResourceIsUnavailable(final VerticalVmScaling scaling) {
        final Class<? extends ResourceManageable> resourceClass = scaling.getResourceClass();
        final ResourceManageable hostResource = scaling.getVm().getHost().getResource(resourceClass);
        final double extraAmountToAllocate = scaling.getResourceAmountToScale();
        LOGGER.warn(
            "{}: {}: {} solicitou mais {} de {} capacidade mas o {} possui somento {} disponível {}",
            scaling.getVm().getSimulation().clockStr(),
            scaling.getClass().getSimpleName(),
            scaling.getVm(), (long) extraAmountToAllocate,
            resourceClass.getSimpleName(), scaling.getVm().getHost(),
            hostResource.getAvailableResource(), resourceClass.getSimpleName());
    }

    /**
     * Performs the down scaling of a Vm resource that is anything else than CPU.
     *
     * @param scaling the Vm's scaling object
     * @return true if the down scaling was performed, false otherwise
     * @see #downScaleVmVertically(VerticalVmScaling)
     */
    private boolean downScaleVmNonCpuResource(final VerticalVmScaling scaling) {
        final Class<? extends ResourceManageable> resourceClass = scaling.getResourceClass();
        final ResourceManageable vmResource = scaling.getVm().getResource(resourceClass);
        final double amountToDeallocate = scaling.getResourceAmountToScale();
        final ResourceProvisioner provisioner = scaling.getVm().getHost().getProvisioner(resourceClass);
        final double newTotalVmResource = vmResource.getCapacity() - amountToDeallocate;
        if (!provisioner.allocateResourceForVm(scaling.getVm(), newTotalVmResource)) {
            LOGGER.error(
                "{}: {}: {} solicitou reduzir a capacidade de {} em {} mas um erro inesperado impediu a alteração.",
                scaling.getVm().getSimulation().clockStr(),
                scaling.getClass().getSimpleName(),
                scaling.getVm(),
                resourceClass.getSimpleName(), (long) amountToDeallocate);
            return false;
        }

        LOGGER.info(
            "{}: {}: {} {} deallocated from {}: new capacity is {}. Current resource usage is {}%",
            scaling.getVm().getSimulation().clockStr(),
            scaling.getClass().getSimpleName(),
            (long) amountToDeallocate, resourceClass.getSimpleName(),
            scaling.getVm(), vmResource.getCapacity(),
            vmResource.getPercentUtilization() * 100);
        return true;
    }

    /**
     * Allocates the host with less PEs in use for a given VM.
     *
     * @param vm {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public HostSuitability allocateHostForVm(final Vm vm) {
        if (getHostList().isEmpty()) {
            LOGGER.error(
                "{}: {}: {} não pode ser alocada pois não há nenhum host no datacenter {}",
                vm.getSimulation().clockStr(), getClass().getSimpleName(), vm, getDatacenter().getId());
            return new HostSuitability("Datacenter has no host.");
        }

        if (vm.isCreated()) {
            return new HostSuitability("VM já foi criada.");
        }

        final Optional<Host> optional = findHostForVm(vm);
        if (optional.filter(Host::isActive).isPresent()) {
            return allocateHostForVm(vm, optional.get());
        }

        LOGGER.warn("{}: {}: Nenhum host capaz de hospedar a {} foi encontrado em {}", vm.getSimulation().clockStr(), getClass().getSimpleName(), vm, datacenter.getName());
        return new HostSuitability("Nenhum host consegue hospedar a VM neste instante.");
    }

    @Override
    public <T extends Vm> List<T> allocateHostForVm(final Collection<T> vmCollection) {
        requireNonNull(vmCollection, "The list of VMs to allocate a host to cannot be null");
        return vmCollection.stream().filter(vm -> !allocateHostForVm(vm).fully()).collect(toList());
    }

    @Override
    public HostSuitability allocateHostForVm(final Vm vm, final Host host) {
        if(vm instanceof VmGroup){
            return createVmsFromGroup((VmGroup) vm, host);
        }

        return createVm(vm, host);
    }

    private HostSuitability createVmsFromGroup(final VmGroup vmGroup, final Host host) {
        int createdVms = 0;
        final HostSuitability hostSuitabilityForVmGroup = new HostSuitability();
        for (final Vm vm : vmGroup.getVmList()) {
            final HostSuitability suitability = createVm(vm, host);
            hostSuitabilityForVmGroup.setSuitability(suitability);
            createdVms += Conversion.boolToInt(suitability.fully());
        }

        vmGroup.setCreated(createdVms > 0);
        if(vmGroup.isCreated()) {
            vmGroup.setHost(host);
        }

        return hostSuitabilityForVmGroup;
    }

    private HostSuitability createVm(final Vm vm, final Host host) {
        final HostSuitability suitability = host.createVm(vm);
        if (suitability.fully()) {
            LOGGER.info(
                "{}: {}: {} foi alocado em {}",
                vm.getSimulation().clockStr(), getClass().getSimpleName(), vm, host);
        } else {
            LOGGER.error(
                "{}: {} A criação de {} em {} falhou por {}.",
                vm.getSimulation().clockStr(), getClass().getSimpleName(), vm, host, suitability);
        }

        return suitability;
    }

    @Override
    public void deallocateHostForVm(final Vm vm) {
        vm.getHost().destroyVm(vm);
    }

    /**
     * {@inheritDoc}
     * The default implementation of such a Function is provided by the method {@link #findHostForVm(Vm)}.
     *
     * @param findHostForVmFunction {@inheritDoc}.
     *                              Passing null makes the default method to find a Host for a VM to be used.
     */
    @Override
    public final void setFindHostForVmFunction(final BiFunction<VmAllocationPolicy, Vm, Optional<Host>> findHostForVmFunction) {
        this.findHostForVmFunction = findHostForVmFunction;
    }

    @Override
    public final Optional<Host> findHostForVm(final Vm vm) {
        final Optional<Host> optional = findHostForVmFunction == null ? defaultFindHostForVm(vm) : findHostForVmFunction.apply(this, vm);
        //If the selected Host is not active, activate it (if it's already active, setActive has no effect)
        return optional.map(host -> host.setActive(true));
    }

    /**
     * Provides the default implementation of the policy
     * to find a suitable Host for a given VM.
     *
     * @param vm the VM to find a suitable Host to
     * @return an {@link Optional} containing a suitable Host to place the VM or an empty {@link Optional} if no suitable Host was found
     * @see #setFindHostForVmFunction(BiFunction)
     */
    protected abstract Optional<Host> defaultFindHostForVm(Vm vm);

    /**
     * {@inheritDoc}
     *
     * <p><b>This method implementation doesn't perform any
     * VM placement optimization and, in fact, has no effect.
     * Classes implementing the {@link VmAllocationPolicyMigration}
     * provide actual implementations for this method that can be overridden
     * by subclasses.
     * </b></p>
     *
     * @param vmList {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Map<Vm, Host> getOptimizedAllocationMap(final List<? extends Vm> vmList) {
        return Collections.emptyMap();
    }

    @Override
    public int getHostCountForParallelSearch() {
        return hostCountForParallelSearch;
    }

    @Override
    public void setHostCountForParallelSearch(final int hostCountForParallelSearch) {
        this.hostCountForParallelSearch = hostCountForParallelSearch;
    }

    @Override
    public boolean isVmMigrationSupported() {
        return false;
    }
}

