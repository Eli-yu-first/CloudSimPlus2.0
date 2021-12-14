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
package org.cloudsimplus.examples.dynamic;

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
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerSpaceShared;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudsimplus.examples.deadlinBasedsimulations.DynamicCloudletsArrival2;
import org.cloudsimplus.listeners.EventInfo;
import org.cloudsimplus.listeners.EventListener;

import java.io.IOException;
import java.util.*;

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
    private static final double TIME_TO_TERMINATE_SIMULATION = 500;

    /**
     * @see Datacenter#getSchedulingInterval()
     */
    private static final int VM_PES_NUMBER = 1;
    private static final int SCHEDULING_INTERVAL = 1;
    private static final int DATACENTER_NUMBER = 2;
    private static final int HOSTS = 8;
    private static final int HOST_PES = 8;
    private static final int CLOUDLETS_NUMBER = 10;
    private static final int VMS_NUMBER = 5;
    private static final int VMS = 10;
    private static final int VM_PES = 4;
    private static final int HOST_PES_NUMBER = 5;
    private static final int CloudletToVM_RoundRobin = 0; // 轮询算法
    private static final int CloudletToVM_CTVOS = 1; // 我们的询算法
    private static final int CloudletToVM_GREEDY = 2; // 贪心算法
    private static final int CLOUDLET_PES = 1;
    private static final int CLOUDLET_LENGTH = 10000;
    private static double sumRate = 0.0;
    private static int testTimes = 10;
    Random random1 = new Random();
    private String SheetName;
    private String ValueName;
    private Cloudlet cloudlet = null;
    private final List<Host> hostList;

    /**
     * Number of Cloudlets to be statically created when the simulation starts.
     */
    private static final int INITIAL_CLOUDLETS_NUMBER = 5;

    private final CloudSim simulation;
    private final DatacenterBroker broker;
    private final List<Vm> vmList;
    private List<Cloudlet> cloudletList = new ArrayList<Cloudlet>();
//    private final Datacenter datacenter0;
    private final ContinuousDistribution random;

    public static void main(String[] args) throws IOException {
//        for (int i = 0; i < testTimes; ++i) {
            new RandomCloudletsArrivalExample();
//        }
        System.out.println("平均违约率为: " + (1.0 * sumRate / testTimes * 100) + "%");
    }

    public RandomCloudletsArrivalExample() throws IOException {
        /*Enables just some level of log messages.
          Make sure to import org.cloudsimplus.util.Log;*/
        //Log.setLevel(ch.qos.logback.classic.Level.WARN);
        simulation = new CloudSim();
        random = new UniformDistr();
        this.hostList = new ArrayList<>();
        simulation.terminateAt(TIME_TO_TERMINATE_SIMULATION);
        createDatacenter(DATACENTER_NUMBER);

        broker = new DatacenterBrokerSimple(simulation);

        vmList = createVms(0, VMS_NUMBER);
        cloudletList = createCloudlets(INITIAL_CLOUDLETS_NUMBER);
        broker.submitVmList(vmList);
        broker.submitCloudletList(cloudletList);

        simulation.addOnClockTickListener(this::createRandomCloudlets);

        simulation.start();

        final List<Cloudlet> finishedCloudlets = broker.getCloudletFinishedList();
        new CloudletsTableBuilder(finishedCloudlets).build();

        final int randomCloudlets = cloudletList.size()-INITIAL_CLOUDLETS_NUMBER;
        System.out.println(
            "Number of Arrived Cloudlets: " +
            cloudletList.size() + " ("+INITIAL_CLOUDLETS_NUMBER+" statically created and "+
            randomCloudlets+" randomly created during simulation runtime)");
        printContractRate(cloudletList);

        System.out.println("  " + getSheetName()+" algorithm Simulation finished!");
        List<Cloudlet> sub = broker.getCloudletFinishedList();
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
        for (int i = 0; i < num; ++i) {
            Host host = createHost(i);
            hostList.add(host);
            new DatacenterSimple(simulation, hostList, new VmAllocationPolicySimple());
        }
    }

    private Datacenter createDatacenter() {
        final List<Host> hostList = new ArrayList<>(HOSTS);
        for(int i = 0; i < HOSTS; i++) {
            Host host = createHost(i);
            hostList.add(host);
        }

        final Datacenter dc = new DatacenterSimple(simulation, hostList, new VmAllocationPolicySimple());
        dc.setSchedulingInterval(SCHEDULING_INTERVAL);
        return dc;
    }

    private Host createHost(int id) {

        List<Pe> peList = new ArrayList<>();
        long mips = 1000; //    ,NUMBER_OF_CLOUDLETS=100
        for (int i = 0; i < HOST_PES_NUMBER; i++) {
            peList.add(new PeSimple(mips, new PeProvisionerSimple()));
        }
        long ram = 1024 * CLOUDLETS_NUMBER; // in Megabytes 204800
        long storage = 1000000; // in Megabytes
        long bw = 1000000; // in Megabits/s

        return new HostSimple(ram, bw, storage, peList)
            .setRamProvisioner(new ResourceProvisionerSimple())
            .setBwProvisioner(new ResourceProvisionerSimple())
            .setVmScheduler(new VmSchedulerSpaceShared());
    }


    /**
     * Creates a list of VMs.
     */
    private List<Vm> createVms(int id, int nums) {
        List<Vm> vmList = new ArrayList<Vm>(nums);
        for (int i = id; i < nums + id; i++) {
            int mips = random1.nextInt(170) + 150;
            long size = 1000; // image size (Megabyte)
            int ram = 512; // vm memory (Megabyte)
            long bw = 1000;

            Vm vm = new VmSimple(i, mips, VM_PES_NUMBER)
                    .setRam(ram)
                    .setBw(bw)
                    .setSize(size)
                    .setCloudletScheduler(new CloudletSchedulerSpaceShared());

            vmList.add(vm);
        }
        return vmList;
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
        long fileSize = random1.nextInt(100) + 1024;
        long outputSize = random1.nextInt(100) + 1024;
        long length = random1.nextInt(5000) + 8000; // in number of Million Instructions (MI)
        UtilizationModel um = new UtilizationModelDynamic(0.2);
        UtilizationModel utilizationModel = new UtilizationModelFull();

        Cloudlet cloudlet = new CloudletSimple(CLOUDLET_LENGTH, CLOUDLET_PES)
                .setLength(length)
                .setFileSize(fileSize)
                .setOutputSize(outputSize)
//                .setUtilizationModel(utilizationModel)
                .setUtilizationModelCpu(new UtilizationModelFull())
                .setUtilizationModelRam(um)
                .setUtilizationModelBw(um)
                .setDeadline(random1.nextDouble(length / 170) + random1.nextDouble(1.0 * length / 210) * 3);

        setCloudlet(cloudlet);

//      Vm vm = bindCloudletToVm(this.vmList, cloudlet, broker, CloudletToVM_CTVOS); // 我们的算法
//        Vm vm = bindCloudletToVm(this.vmList,cloudlet,broker,CloudletToVM_GREEDY);//贪心算法
        Vm vm = bindCloudletToVm(this.vmList,cloudlet,broker,CloudletToVM_RoundRobin);//轮询算法
        cloudlet.setVm(vm);

        return cloudlet;
    }

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
            broker.submitCloudlet(cloudlet);
            System.out.printf("%n# Randomly creating 1 Cloudlet_"+cloudlet.getId()+" at time %.2f%n", evt.getTime());
        }
    }
    private void printContractRate(List<Cloudlet> cloudletList) throws IOException {
        int ContractNum = 0;
        int size = cloudletList.size();
        for (int i = 0; i < size; i++) {
            if (this.cloudletList.get(i).getIfContract()) {
                ContractNum++;
            }
        }
        double DisContractRate = (1.0 * (size - ContractNum) / size);
        sumRate += DisContractRate;
    }

    // Cloudlet根据MI降序排列
    private class CloudletComparator implements Comparator<Cloudlet> {
        @Override
        public int compare(Cloudlet cl1, Cloudlet cl2) {
            return (int) (cl2.getLength()+cl2.getDeadline()*50000 - cl1.getLength()+cl1.getDeadline()*5000 );
        }
    }

    // Vm根据PredictTime升序排列
    private class VmComparator implements Comparator<Vm> {
        @Override
        public int compare(Vm vm1, Vm vm2) {
            return (int) (vm1.getPredictTime(getCloudlet()) - vm2.getPredictTime(getCloudlet()));
        }
    }

    // Vm根据MIPS升序排列
    private class VmComparatorAwared implements Comparator<Vm> {
        @Override
        public int compare(Vm vm1, Vm vm2) {
            return (int) (vm1.getPredictTime(getCloudlet()) - vm2.getPredictTime(getCloudlet()));
        }
    }
    public void setCloudlet(Cloudlet cloudlet) {
        this.cloudlet = cloudlet;
    }
    public Cloudlet getCloudlet() {
        return cloudlet;
    }
    private double getPretime(Cloudlet cloudlet, Vm vm) {
        double preWaitTime = 0.0;
        Queue<Cloudlet> queue = vm.getCloudletsOnVm();
        while (queue.peek() != null) {
            Cloudlet cloudlet1 = queue.poll();
            preWaitTime += cloudlet1.getLength() / vm.getMips();
            System.out.println(
                "cloudlet_"
                    + cloudlet1.getId()
                    + ",正在等待Vm_"
                    + vm.getId()
                    + "等待时间_"
                    + cloudlet1.getLength() / vm.getMips());
        }
        double execTime = 1.0 * cloudlet.getLength() / vm.getMips();
        System.out.println(
            "cloudlet_"
                + cloudlet.getId()
                + ",preWaitTime = "
                + preWaitTime
                + ",execTime="
                + execTime
                + ",ALLTime="
                + (preWaitTime + execTime));
        return preWaitTime + execTime;
    }
    private Vm bindCloudletToVm(List<Vm> vmList, Cloudlet cloudlet, DatacenterBroker broker, int type) {
        switch (type) {
            case CloudletToVM_CTVOS:
                setSheetName("CTVOS_" + ValueName);
                return bindCloudletToVm_CTVOS(cloudlet, vmList);
            case CloudletToVM_GREEDY:
                setSheetName("GREEDY_" + ValueName);
                return bindCloudletToVm_GREEDY(cloudlet, vmList);
            case CloudletToVM_RoundRobin:
                setSheetName("RoundRobin_" + ValueName);
                return bindCloudletToVm_RoundRobin(cloudlet, vmList);
            default:
                return null;
        }
    }

    /* Place cloudlet to a vm based on sensivity-based algorithm. */
    private Vm bindCloudletToVm_CTVOS(Cloudlet cloudlet, List<Vm> vmList) {
        // CTVOS
        setCloudlet(cloudlet);
        double deadline = cloudlet.getDeadline();
        int vmid = 0;
        Collections.sort(vmList, new VmComparator()); // vm按MIPS升序排列

        System.out.println("==============");
        if (deadline >= getPretime(cloudlet, vmList.get(vmList.size() - 1))) { // 所有的都行
            if (cloudlet.getSensivityType() == 0) { // 不敏感时，可以放在最后一个
                System.out.println(
                    "deadline>=pretime都行 && sentype=1适中"
                        + "cloudlet_"
                        + cloudlet.getId()
                        + "被分配到了"
                        + "vm_"
                        + (vmList.size() - 1)
                        + "上");
                return vmList.get(vmList.size() - 1);
            }
            if (cloudlet.getSensivityType() == 1) { // 敏感度适中时
                System.out.println(
                    "deadline>=pretime都行 && sentype=1适中"
                        + "cloudlet_"
                        + cloudlet.getId()
                        + "被分配到了"
                        + "vm_"
                        + (vmList.size() / 2)
                        + "上");
                return vmList.get(vmList.size() / 2);
            }
            if (cloudlet.getSensivityType() == 2) { // 敏感度非常高时
                System.out.println(
                    "deadline>=pretime都行 && sentype=2紧急"
                        + "cloudlet_"
                        + cloudlet.getId()
                        + "被分配到了"
                        + "vm_"
                        + (0)
                        + "上");
                return vmList.get(0);
            }
        } else if (deadline <= getPretime(cloudlet, vmList.get(0))) { // 所有的都不行，无论敏感度高低，都直接放在最后一个
            System.out.println(
                "deadline<=pretime 都不行"
                    + "cloudlet_"
                    + cloudlet.getId()
                    + "被分配到了"
                    + "vm_"
                    + (vmList.size() - 1)
                    + "上");
            return vmList.get(vmList.size() - 1);
        } else { // deadline在中间
            if (cloudlet.getSensivityType() == 0) { // 不敏感时，可以放在最后靠近deadline的地方
                for (int i = 0; i < vmList.size(); i++) {
                    if (getPretime(cloudlet, vmList.get(i)) >= cloudlet.getDeadline()) {
                        System.out.println(
                            "deadline在中间 && sentype=0不紧急,"
                                + "cloudlet_"
                                + cloudlet.getId()
                                + "被分配到了"
                                + "vm_"
                                + (i - 1)
                                + "上");
                        return vmList.get(i - 1);
                    }
                }
            }
            if (cloudlet.getSensivityType() == 1) {
                for (int i = 0; i < vmList.size(); i++) {
                    if (getPretime(cloudlet, vmList.get(i)) >= cloudlet.getDeadline()) {
                        System.out.println(
                            "deadline在中间 && sentype=1适中"
                                + "cloudlet_"
                                + cloudlet.getId()
                                + "被分配到了"
                                + "vm_"
                                + (vmList.get((i - 1) / 2))
                                + "上");
                        return vmList.get((i - 1) / 2);
                    }
                }
            }
            if (cloudlet.getSensivityType() == 2) {
                System.out.println(
                    "deadline在中间 && sentype=2紧急"
                        + "cloudlet_"
                        + cloudlet.getId()
                        + "被分配到了"
                        + "vm_"
                        + (0)
                        + "上");
                return vmList.get(0);
            }
        }
        System.out.println("cloudlet_" + cloudlet.getId() + "被分配到了" + "vm_" + vmid + "上");
        return vmList.get(0);
    }

    /* Place cloudlet to a vm based on Round-Robin algorithm. */
    private Vm bindCloudletToVm_RoundRobin(Cloudlet cloudlet, List<Vm> vmList) {
        return vmList.get((int) ((cloudlet.getId()+1) % (vmList.size())));
    }

    /* Place cloudlet to a vm based on greeay algorithm. */
    private Vm bindCloudletToVm_GREEDY(Cloudlet cloudlet, List<Vm> vmList) {

        List<Cloudlet> cloudletList = new ArrayList<Cloudlet>(this.cloudletList.size());
        cloudletList.addAll(this.broker.getCloudletSubmittedList());
        cloudletList.add(cloudlet);

        int cloudletNum = cloudletList.size();
        int vmNum = vmList.size();
        // time[i][j] 表示任务i在虚拟机j上的执行时间
        double[][] time = new double[cloudletNum][vmNum];
        // cloudletList按MI降序排列, vm按MIPS升序排列
        Collections.sort(cloudletList, new CloudletComparator());
        Collections.sort(vmList, new VmComparatorAwared());

        for (int i = 0; i < cloudletNum; i++) {
            for (int j = 0; j < vmNum; j++) {
                time[i][j] = (double) cloudletList.get(i).getLength() / vmList.get(j).getMips();
            }
        }

        double[] vmLoad = new double[vmNum]; // 在某个虚拟机上任务的总执行时间
        int[] vmTasks = new int[vmNum]; // 在某个Vm上运行的任务数量
        double minLoad = 0; // 记录当前任务分配方式的最优值
        int idx = 0; // 记录当前任务最优分配方式对应的虚拟机列号
        // 第一个cloudlet分配给最快的vm
        vmLoad[vmNum - 1] = time[0][vmNum - 1];
        vmTasks[vmNum - 1] = 1;
        //		CloudletList.getById(getCloudletList(), cloudletId).setVmId(vmId);
        if (cloudlet.getId() == 0) {
            return vmList.get(vmNum - 1);
        }
        //        cloudletList.get(0).setVm(vmList.get(vmNum-1));
        for (int i = 1; i < cloudletNum; i++) {
            minLoad = vmLoad[vmNum - 1] + time[i][vmNum - 1];
            idx = vmNum - 1;
            for (int j = vmNum - 2; j >= 0; j--) {
                // 如果当前虚拟机未分配任务,则比较完当前任务分配给该虚拟机是否最优
                if (vmLoad[j] == 0) {
                    if (minLoad >= time[i][j]) {
                        idx = j;
                    }
                    break;
                }
                if (minLoad > vmLoad[j] + time[i][j]) {
                    minLoad = vmLoad[j] + time[i][j];
                    idx = j;
                }
                // 简单的负载均衡
                else if (minLoad == vmLoad[j] + time[i][j] && vmTasks[j] < vmTasks[idx]) {
                    idx = j;
                }
            }
            vmLoad[idx] += time[i][idx];
            vmTasks[idx]++;
            if (cloudlet.getId() == i) {
                return vmList.get(idx);
            }
        }
        return vmList.get(0);
    }

    public String getSheetName() {
        return this.SheetName;
    }
    public void setSheetName(String sheetName) {
        this.SheetName = sheetName;
    }


}
