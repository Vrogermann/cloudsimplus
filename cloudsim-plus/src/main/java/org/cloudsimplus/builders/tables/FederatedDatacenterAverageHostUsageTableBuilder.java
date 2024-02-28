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


import org.cloudbus.cloudsim.hosts.FederatedHostSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudsimplus.util.Records;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


public class FederatedDatacenterAverageHostUsageTableBuilder extends TableBuilderAbstract<Records.HostAverageCpuUsage>{

    public FederatedDatacenterAverageHostUsageTableBuilder(final List<Records.HostAverageCpuUsage> hostAverageCpuUsageList) {
        super(hostAverageCpuUsageList);
    }


    public FederatedDatacenterAverageHostUsageTableBuilder(List<Records.HostAverageCpuUsage> list, final Table table) {
        super(list, table);
    }

    @Override
    protected void createTableColumns() {

        addColumnDataFunction(getTable().addColumn("Datacenter"), (usage)-> usage.host().getDatacenter().getName());
        addColumnDataFunction(getTable().addColumn("Host Name"), (usage)-> ((FederatedHostSimple) usage.host()).getName());
        addColumnDataFunction(getTable().addColumn("Average cpu usage"), (usage)-> (String.format("%.2f", usage.averageCpuUsage())).replace(',','.'));

    }
}
