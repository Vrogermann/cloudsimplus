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

import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.FederatedCloudletSimple;
import org.cloudbus.cloudsim.core.Identifiable;
import org.cloudbus.cloudsim.hosts.FederatedHostSimple;
import org.cloudsimplus.util.Records;

import java.util.List;

/**
 * Builds a table for printing simulation results from a list of Cloudlets.
 * It defines a set of default columns but new ones can be added
 * dynamically using the {@code addColumn()} methods.
 *
 * <p>The basic usage of the class is by calling its constructor,
 * giving a list of Cloudlets to be printed, and then
 * calling the {@link #build()} method.</p>
 *
 * @author Manoel Campos da Silva Filho
 * @since CloudSim Plus 1.0
 */
public class FederationTopologyTableBuilder extends TableBuilderAbstract<Records.University> {
    private static final String TIME_FORMAT = "%.0f";
    private static final String SECONDS = "Seconds";
    private static final String CPU_CORES = "CPU cores";

    /**
     * Instantiates a builder to print the list of Federated Cloudlets using the a
     * default {@link MarkdownTable}.
     * To use a different {@link Table}, check the alternative constructors.
     *
     * @param list the list of Cloudlets to print
     */
    public FederationTopologyTableBuilder(final List<? extends Records.University> list) {
        super(list);
    }

    /**
     * Instantiates a builder to print the list of Federated Cloudlets using the
     * given {@link Table}.
     *
     * @param list the list of Cloudlets to print
     * @param table the {@link Table} used to build the table with the Cloudlets data
     */
    public FederationTopologyTableBuilder(final List<? extends Records.University> list, final Table table) {
        super(list, table);
    }

    @Override
    protected void createTableColumns() {

        addColumnDataFunction(getTable().addColumn("University Abbreviation"),
            (Records.University::abbreviation));

        addColumnDataFunction(getTable().addColumn("University Coordinates"),
            (university -> String.format("%s, %s",university.coordinates().latitude(), university.coordinates().longitude())));

        addColumnDataFunction(getTable().addColumn("Number of Datacenters"),
            (Records.University::datacenterAmount));

        addColumnDataFunction(getTable().addColumn("Number of Hosts on each Datacenter"),
            (Records.University::hostsPerDatacenter));
        addColumnDataFunction(getTable().addColumn("Number of Users that will submit BoTs"),
            (Records.University::numberOfUsers));;

        addColumnDataFunction(getTable().addColumn("Number of bots submited by each user"),
            (Records.University::BoTsPerUser));






    }

    /**
     * Rounds a given time so that decimal places are ignored.
     * Sometimes a Cloudlet start at time 0.1 and finish at time 10.1.
     * Previously, in such a situation, the finish time was rounded to 11 (Math.ceil),
     * giving the wrong idea that the Cloudlet took 11 seconds to finish.
     * This method makes some little adjustments to avoid such a precision issue.
     *
     * @param cloudlet the Cloudlet being printed
     * @param time the time to round
     * @return
     */
    private double roundTime(final Cloudlet cloudlet, final double time) {

        /*If the given time minus the start time is less than 1,
        * it means the execution time was less than 1 second.
        * This way, it can't be round.*/
        if(time - cloudlet.getExecStartTime() < 1){
            return time;
        }

        final double startFraction = cloudlet.getExecStartTime() - (int) cloudlet.getExecStartTime();
        return Math.round(time - startFraction);
    }
}
