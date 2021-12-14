
package org.cloudsimplus.examples.deadlinBasedsimulations;

import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
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
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudsimplus.examples.dynamic.DynamicCloudletsArrival2;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class DynamicCloudletsArrival2test_2 {
    /**
     * Number of Processor Elements (CPU Cores) of each Host.
     */
    private static final int HOST_PES_NUMBER = 5;
    /** Number of Processor Elements (CPU Cores) of each VM and cloudlet. */
    private static final int VM_PES_NUMBER = 1;

    private static final double TIME_TO_TERMINATE_SIMULATION = 500;
    /** Number of Cloudlets to create simultaneously. Other cloudlets will be enqueued. */
    private static final int VMS_NUMBER = 5;

    private static final int CLOUDLETS_NUMBER = 100;
    private static final int INITIAL_CLOUDLETS_NUMBER = 5;
    private static final int DATACENTER_NUMBER = 2;
    private final ContinuousDistribution random1;

    /** Number of Vms to create simultaneously. */
    private static final int CloudletToVM_RoundRobin = 0; // 轮询算法

    private static final int CloudletToVM_CTVOS = 1; // 我们的询算法
    private static final int CloudletToVM_GREEDY = 2; // 贪心算法
    private static File file = new File("D:\\testData\\retult.txt");
    private static double sumRate = 0.0;
    private static int testTimes = 50;
    private final List<Host> hostList;
    private final List<Vm> vmList;
    private final List<Cloudlet> cloudletList;
    private final DatacenterBroker broker;
    private final CloudSim simulation;
//    private final String fileName;
    private  double submissionDelay = 0;
    Random random = new Random();
    private String SheetName;
    private String ValueName;
    private Cloudlet cloudlet = null;

    /**
     * Number of Cloudlets to create simultaneously.
     * Other cloudlets will be enqueued.
     */
    private static final int NUMBER_OF_CLOUDLETS = 90;
    private final List<Datacenter> datacenterList;


    /**
     * Starts the example execution, calling the class constructor\
     * to build and run the simulation.
     *
     * @param args command line parameters
     */
    public static void main(String[] args) {
        new DynamicCloudletsArrival2test_2();
    }

    /**
     * Default constructor that builds and starts the simulation.
     */
    private DynamicCloudletsArrival2test_2() {
        /*Enables just some level of log messages.
          Make sure to import org.cloudsimplus.util.Log;*/
        //Log.setLevel(ch.qos.logback.classic.Level.WARN);

        System.out.println("Starting " + getClass().getSimpleName());
        simulation = new CloudSim();
        random1 = new UniformDistr();
        this.hostList = new ArrayList<>();
        this.vmList = new ArrayList<>();
        this.cloudletList = new ArrayList<>();
        this.datacenterList = createDatacenter(DATACENTER_NUMBER);
        this.broker = new DatacenterBrokerSimple(simulation);

        List<Vm> vmList = createVmList(VMS_NUMBER);

        this.vmList.addAll(vmList);
        createAndSubmitVmAndCloudlets();

        runSimulationAndPrintResults();
        System.out.println(getClass().getSimpleName() + " finished!");
    }

    private List<Vm> createVmList(int vmsNumber) {
        List<Vm> list = new ArrayList<>(VMS_NUMBER);
        list = createVm(0, vmsNumber, broker);
        broker.submitVmList(list);
        return list;
    }

    private void runSimulationAndPrintResults() {
        simulation.start();
        List<Cloudlet> cloudlets = broker.getCloudletFinishedList();
        new CloudletsTableBuilder(cloudlets).build();
    }

    /**
     * Creates cloudlets and submit them to the broker, applying
     * a different submission delay for each one (simulating the dynamic cloudlet arrival).
     *
     * @param vm Vm to run the cloudlets to be created
     *
     * @see #createCloudlet(int, Vm, DatacenterBroker)
     */
    private void createAndSubmitCloudlets(Vm vm) {
        int cloudletId = cloudletList.size();
        double submissionDelay = 0;
        List<Cloudlet> list = new ArrayList<>(NUMBER_OF_CLOUDLETS);
        for(int i = 0; i < NUMBER_OF_CLOUDLETS; i++){
            Cloudlet cloudlet = createCloudlet(cloudletId++, vm, broker);
            cloudlet.setSubmissionDelay(submissionDelay);
            submissionDelay += 10;
            list.add(cloudlet);
        }

        broker.submitCloudletList(list);
        cloudletList.addAll(list);
    }
    private void createAndSubmitCloudletsOnVms(List<Vm> vmlist) {
        int cloudletId = cloudletList.size();
        double submissionDelay = 0;
        List<Cloudlet> list = new ArrayList<>(NUMBER_OF_CLOUDLETS);
        for(int i = 0; i < NUMBER_OF_CLOUDLETS; i++){
            Cloudlet cloudlet = createCloudlet(cloudletId++, vmlist.get(i%VM_PES_NUMBER), broker);
            cloudlet.setSubmissionDelay(submissionDelay);
            submissionDelay += 5;
            list.add(cloudlet);
        }

        broker.submitCloudletList(list);
        cloudletList.addAll(list);
    }

    /**
     * Creates one Vm and a group of cloudlets to run inside it,
     * and submit the Vm and its cloudlets to the broker.
     *
     * @see #createVm(int, DatacenterBroker)
     */
    private void createAndSubmitVmAndCloudlets() {
        List<Vm> list = new ArrayList<>();
//        Vm vm = createVm(this.vmList.size(), broker);
//        list.add(vm);
        list = createVms(this.vmList.size(), VM_PES_NUMBER,broker);

        broker.submitVmList(list);
        this.vmList.addAll(list);

//        createAndSubmitCloudlets(vm);
        createAndSubmitCloudletsOnVms(vmList);
    }

    /**
     * Creates a VM with pre-defined configuration.
     *
     * @param id the VM id
     * @param broker the broker that will be submit the VM
     * @return the created VM
     *
     */
    private List<Vm> createVm(int id, int nums, DatacenterBroker broker) {
        List<Vm> vmList = new ArrayList<Vm>(nums);
        for (int i = 0; i < nums; ++i) {
            int mips =  random.nextInt(150) + 170;
            long size = 10000; // image size (Megabyte)
            int ram = 512; // vm memory (Megabyte)
            long bw = 1000;

            Vm vm = new VmSimple(id++, mips, 1)
                .setRam(ram)
                .setBw(bw)
                .setSize(size)
                .setCloudletScheduler(new CloudletSchedulerSpaceShared());

            vmList.add(vm);
        }
        return vmList;
    }

    private List<Vm> createVms(int id,int nums, DatacenterBroker broker) {
        List<Vm> vmList = new ArrayList<Vm>(nums);
        for (int i = 0; i < 4; ++i) {
            int mips = 1000;
            long size = 10000; // image size (Megabyte)
            int ram = 512; // vm memory (Megabyte)
            long bw = 1000;

            Vm vm = new VmSimple(id, mips, 1)
                .setRam(ram).setBw(bw).setSize(size)
                .setCloudletScheduler(new CloudletSchedulerSpaceShared());

            vmList.add(vm);
        }
        return vmList;
    }

    /**
     * Creates a cloudlet with pre-defined configuration.
     *
     * @param id Cloudlet id
     * @param vm vm to run the cloudlet
     * @param broker the broker that will submit the cloudlets
     * @return the created cloudlet
     */
    private Cloudlet createCloudlet(int id, Vm vm, DatacenterBroker broker) {
        long fileSize = 300;
        long outputSize = 300;
        long length = 10000; //in number of Million Instructions (MI)
        int pesNumber = 1;
        UtilizationModel utilizationModel = new UtilizationModelFull();

        return new CloudletSimple(id, length, pesNumber)
            .setFileSize(fileSize)
            .setOutputSize(outputSize)
            .setUtilizationModel(utilizationModel)
            .setVm(vm);
    }

    /**
     * Creates a Datacenter with pre-defined configuration.
     *
     * @return the created Datacenter
     */
    private List<Datacenter> createDatacenter(int nums) {
        List<Datacenter> list = new ArrayList<Datacenter>(nums);
        for (int i = 0; i < nums; ++i) {
            Host host = createHost(i);
            hostList.add(host);
            Datacenter datacenter = new DatacenterSimple(simulation, hostList, new VmAllocationPolicySimple());
            list.add(datacenter);
        }
        return list;
    }

    /**
     * Creates a host with pre-defined configuration.
     *
     * @param id The Host id
     * @return the created host
     */
    private Host createHost(int id) {
        List<Pe> peList = new ArrayList<>();
        long mips = 10000;
        for(int i = 0; i < 4; i++){
            peList.add(new PeSimple(mips, new PeProvisionerSimple()));
        }
        long ram = 20480; // in Megabytes
        long storage = 1000000; // in Megabytes
        long bw = 10000; //in Megabits/s

        return new HostSimple(ram, bw, storage, peList)
            .setRamProvisioner(new ResourceProvisionerSimple())
            .setBwProvisioner(new ResourceProvisionerSimple())
            .setVmScheduler(new VmSchedulerSpaceShared());
    }
}
