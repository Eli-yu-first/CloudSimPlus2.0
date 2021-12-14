/*
 * CloudSim Plus: A modern, highly-extensible and easier-to-use Framework for
 * Modeling and Simulation of Cloud Computing Infrastructures and Services.
 * http://cloudsimplus.org
 *
 *     Copyright (C) 2015-2021 Universidade da Beira Interior (UBI, Portugal) and
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
package org.cloudsimplus.examples.deadlinBasedsimulations;

import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.Simulation;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.distributions.ContinuousDistribution;
import org.cloudbus.cloudsim.distributions.UniformDistr;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.ResourceProvisionerSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerSpaceShared;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudsimplus.examples.dynamic.KeepSimulationRunningExample;
import org.cloudsimplus.listeners.EventInfo;
import org.cloudsimplus.listeners.EventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Shows how to keep the simulation running, even
 * when there is no event to be processed anymore.
 * It calls the {@link Simulation#terminateAt(double)} to define
 * the time the simulation must be terminated.
 *
 * <p>The example is useful when you want to run a simulation
 * for a specific amount of time, for instance, to wait random arrival
 * or requests (such as Cloudlets and VMs).
 * Lets say you want to run a simulation for 24 hours.
 * This way, you just need to call {@code simulation.terminateAt(60*60*24)} (realize the value is in seconds).</p>
 *
 * <p>It creates Cloudlets randomly, according to a pseudo random number generator (PRNG) following the
 * {@link UniformDistr uniform distribution}. You can change the PRNG as you wish,
 * for instance, to use a {@link org.cloudbus.cloudsim.distributions.PoissonDistr} arrival process.</p>
 *
 * <p>The example uses the CloudSim Plus {@link EventListener} feature
 * to enable monitoring the simulation and dynamically creating Cloudlets and VMs at runtime.
 * It relies on
 * <a href="https://docs.oracle.com/javase/tutorial/java/javaOO/methodreferences.html">Java 8 Method References</a>
 * to set a method to be called for {@link Simulation#addOnClockTickListener(EventListener) onClockTick events}.
 * It enables getting notifications when the simulation clock advances, then creating and submitting new cloudlets.
 * </p>
 *
 * <p>Since the simulation was set to keep waiting for new events
 * until a defined time, the clock will be updated
 * even if no event arrives, to simulate time passing.
 * Check the {@link Simulation#terminateAt(double)} for details.
 * The simulation will just end at the specified time.
 * </p>
 *
 * @author Manoel Campos da Silva Filho
 * @since CloudSim Plus 4.0.4
 * @see KeepSimulationRunningExample
 */
public class RandomCloudletsArrivalExample {
    /**
     * @see Simulation#terminateAt(double)
     */
    private static final double TIME_TO_TERMINATE_SIMULATION = 100;

    /**
     * @see Datacenter#getSchedulingInterval()
     */
    private static final int SCHEDULING_INTERVAL = 1;
    Random randomR = new Random();
    private static final int HOSTS = 8;
    private static final int HOST_PES = 8;

    private static final int VMS = 10;
    private static final int VM_PES = 4;

    private static final int CLOUDLET_PES = 2;
    private static final int CLOUDLET_LENGTH = 10000;
    private static final int CLOUDLET_NUM = 10;
    private static final int VM_NUM = 5;
    private static final int DataCenter_NUM = 2;
    /**
     * Number of Cloudlets to be statically created when the simulation starts.
     */
    private static final int INITIAL_CLOUDLETS_NUMBER = 5;

    private final CloudSim simulation;
    private final DatacenterBroker broker0;
    private final List<Vm> vmList;
    private final List<Cloudlet> cloudletList;
//    private final Datacenter datacenter0;
    private final ContinuousDistribution random;

    public static void main(String[] args) {
        new RandomCloudletsArrivalExample();
    }

    private RandomCloudletsArrivalExample() {
        /*Enables just some level of log messages.
          Make sure to import org.cloudsimplus.util.Log;*/
        //Log.setLevel(ch.qos.logback.classic.Level.WARN);

        simulation = new CloudSim();
        random = new UniformDistr();
        simulation.terminateAt(TIME_TO_TERMINATE_SIMULATION);
        createDatacenter(DataCenter_NUM);

        broker0 = new DatacenterBrokerSimple(simulation);

        vmList = createVms();
        cloudletList = createCloudlets(INITIAL_CLOUDLETS_NUMBER);
        broker0.submitVmList(vmList);
        broker0.submitCloudletList(cloudletList);

        simulation.addOnClockTickListener(this::createRandomCloudlets);

        simulation.start();

        final List<Cloudlet> finishedCloudlets = broker0.getCloudletFinishedList();
        new CloudletsTableBuilder(finishedCloudlets).build();

        final int randomCloudlets = cloudletList.size()-INITIAL_CLOUDLETS_NUMBER;
        System.out.println(
            "Number of Arrived Cloudlets: " +
            cloudletList.size() + " ("+INITIAL_CLOUDLETS_NUMBER+" statically created and "+
            randomCloudlets+" randomly created during simulation runtime)");

        List<Cloudlet> sub = broker0.getCloudletFinishedList();
        int i = 0;
        for (Cloudlet cloudlet1 : sub) {
            System.out.println("第"+i+"个cloudlet的id是_"+""+cloudlet1.getId());
            i++;
        }
    }

    /**
     * Creates a Datacenter and its Hosts.
     */
    private void createDatacenter(int num) {
        for (int j = 0; j < num; ++j) {
            final List<Host> hostList = new ArrayList<>(HOSTS);
            for(int i = 0; i < HOSTS; i++) {
                Host host = createHost();
                hostList.add(host);
            }
            final Datacenter dc = new DatacenterSimple(simulation, hostList, new VmAllocationPolicySimple());
            dc.setSchedulingInterval(SCHEDULING_INTERVAL);
        }
    }

    private Host createHost() {
        List<Pe> peList = new ArrayList<>(HOST_PES);
        //List of Host's CPUs (Processing Elements, PEs)
        for (int i = 0; i < HOST_PES; i++) {
            peList.add(new PeSimple(1000, new PeProvisionerSimple()));
        }

        final long ram = 2048; //in Megabytes
        final long bw = 10000; //in Megabits/s
        final long storage = 1000000; //in Megabytes
        Host host = new HostSimple(ram, bw, storage, peList);
        host
            .setRamProvisioner(new ResourceProvisionerSimple())
            .setBwProvisioner(new ResourceProvisionerSimple())
            .setVmScheduler(new VmSchedulerSpaceShared());
        return host;
    }

    /**
     * Creates a list of VMs.
     */
    private List<Vm> createVms() {
        final List<Vm> list = new ArrayList<>(VM_NUM);
        for (int i = 0; i < VM_NUM; i++) {
            list.add(createVm(VM_PES));
        }
        return list;
    }

    private Vm createVm(final int pes) {
        int mips = randomR.nextInt(170) + 150;
        long size = 10000; // image size (Megabyte)
        int ram = 512; // vm memory (Megabyte)
        long bw = 1000;

        return new VmSimple(mips, pes)
            .setRam(ram).setBw(bw).setSize(size)
            .setCloudletScheduler(new CloudletSchedulerSpaceShared());
    }

    /**
     * Creates a list of Cloudlets.
     * @param count number of Cloudlets to create statically
     */
    private List<Cloudlet> createCloudlets(final int count) {
        final List<Cloudlet> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(createCloudlet());
        }

        return list;
    }

    private Cloudlet createCloudlet() {
        long fileSize = randomR.nextInt(100) + 1024;
        long outputSize = randomR.nextInt(100) + 1024;
        long length = randomR.nextInt(5000) + 8000; // in number of Million Instructions (MI)
        int pesNumber = 2;

        UtilizationModel um = new UtilizationModelDynamic(0.2);
        Cloudlet cloudlet  =  new CloudletSimple(length, pesNumber)
            .setFileSize(fileSize)
            .setOutputSize(outputSize)
            .setUtilizationModelCpu(new UtilizationModelFull())
            .setUtilizationModelRam(um)
            .setUtilizationModelBw(um)
            .setDeadline(randomR.nextDouble(length / 170) + randomR.nextDouble(1.0 * length / 210) * 3);
        cloudlet.setSubmissionDelay(10.0);
        return cloudlet;
    }
//    private Cloudlet createCloudlet() {
//        UtilizationModel um = new UtilizationModelDynamic(0.2);
//        Cloudlet cloudlet  =  new CloudletSimple(CLOUDLET_LENGTH, CLOUDLET_PES)
//            .setFileSize(1024)
//            .setOutputSize(1024)
//            .setUtilizationModelCpu(new UtilizationModelFull())
//            .setUtilizationModelRam(um)
//            .setUtilizationModelBw(um);
//        cloudlet.setSubmissionDelay(10.0);
//        System.out.println("创建的cloudlet是cloudlet_"+cloudlet.getId());
//        return cloudlet;
//    }
    /**
     * Simulates the dynamic arrival of Cloudlets, randomly during simulation runtime.
     * At any time the simulation clock updates, a new Cloudlet will be
     * created with a probability of 30%.
     *
     * @param evt
     */
    private void createRandomCloudlets(final EventInfo evt) {
        if(random.sample() <= 0.3){
            Cloudlet cloudlet = createCloudlet();
            cloudletList.add(cloudlet);
            broker0.submitCloudlet(cloudlet);
            System.out.printf("%n# Randomly creating 1 Cloudlet_"+cloudlet.getId()+" at time %.2f%n", evt.getTime());
        }
    }
}
