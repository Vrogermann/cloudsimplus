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
package org.cloudsimplus.builders.tables;

import org.cloudbus.cloudsim.core.CloudSimEntity;
import org.cloudbus.cloudsim.datacenters.FederatedDatacenter;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostStateHistoryEntry;
import org.cloudsimplus.traces.ufpel.ConvertedBoT;

import java.util.List;

/**
 * Builds a table for printing {@link HostStateHistoryEntry} entries from the
 * {@link Host#getStateHistory()}.
 * It defines a set of default columns but new ones can be added
 * dynamically using the {@code addColumn()} methods.
 *
 * <p>The basic usage of the class is by calling its constructor,
 * giving a Host to print its history, and then
 * calling the {@link #build()} method.</p>
 *
 * @author Manoel Campos da Silva Filho
 * @since CloudSim Plus 2.3.2
 */
public class FederatedDatacenterHistoryTableBuilder extends TableBuilderAbstract<FederatedDatacenter>{
    private final List<FederatedDatacenter> federatedDatacenterList;


    public FederatedDatacenterHistoryTableBuilder(final List<FederatedDatacenter> datacenter) {
        super(datacenter);
        this.federatedDatacenterList = datacenter;
    }


    public FederatedDatacenterHistoryTableBuilder(final List<FederatedDatacenter> datacenter, final Table table) {
        super(datacenter, table);
        this.federatedDatacenterList = datacenter;
    }

    @Override
    protected void createTableColumns() {
        addColumnDataFunction(getTable().addColumn("University Abbreviation"),
            (datacenter-> datacenter.getOwner().getAbbreviation()));

        addColumnDataFunction(getTable().addColumn(getTable() instanceof MarkdownTable ? "      Datacenter     " : "Datacenter"),
            (CloudSimEntity::getName));

        addColumnDataFunction(getTable().addColumn("Number of hosts"),
            (datacenter -> datacenter.getHostList().size()));

        addColumnDataFunction(getTable().addColumn("Number of PEs per host"),
            (datacenter -> datacenter.getHostList().stream().
                mapToLong(host->host.getPeList().size()).sum() / datacenter.getHostList().size()));

        addColumnDataFunction(getTable().addColumn("Amount of university users"),
            (datacenter -> datacenter.getOwner().getUserList().size()));

        addColumnDataFunction(getTable().addColumn("BoTs per university user"),
            (datacenter -> datacenter.getOwner().getBotsPerUser()));

        addColumnDataFunction(
            getTable().addColumn("Average CPU Usage"), (FederatedDatacenter::getAverageCpuUsage));


    }
}
