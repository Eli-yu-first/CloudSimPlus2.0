package org.cloudsimplus.examples.deadlinBasedsimulations;

/**
 * @author : ZY @Type : DynamicCloudletsArrival2.java
 * @date : 11/16/2021 23:12 @Description : null
 */

import org.apache.commons.io.FileUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
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
import org.cloudsimplus.listeners.CloudletVmEventInfo;
import org.cloudsimplus.listeners.EventInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

/**
 * An example showing how to delay the submission of cloudlets. Although there is enough resources
 * to run all cloudlets simultaneously, the example delays the creation of each cloudlet inside a
 * VM, simulating the dynamic arrival of cloudlets. For each instantiated cloudlet will be defined a
 * different submission delay. Even there is enough resources to run all cloudlets simultaneously,
 * it is used a CloudletSchedulerTimeShared, analyzing the output you can see that each cloudlet
 * starts in a different time, simulating different arrivals.
 *
 * @author Manoel Campos da Silva Filho
 * @since CloudSim Plus 1.0
 */
public class DynamicCloudletsArrival2 {
  /** Number of Processor Elements (CPU Cores) of each Host. */
  private static final int HOST_PES_NUMBER = 5;

  /** Number of Processor Elements (CPU Cores) of each VM and cloudlet. */
  private static final int VM_PES_NUMBER = 1;

  /** Number of Cloudlets to create simultaneously. Other cloudlets will be enqueued. */
  private static final int CLOUDLETS_NUMBER = 100;

  private static final int DATACENTER_NUMBER = 2;

  /** Number of Vms to create simultaneously. */
  private static final int VMS_NUMBER = 5;

  private static final int CloudletToVM_RoundRobin = 0; // ????????????
  private static final int CloudletToVM_CTVOS = 1; // ??????????????????
  private static final int CloudletToVM_GREEDY = 2; // ????????????
  private static File file = new File("D:\\testData\\retult.txt");
  private static double sumRate = 0.0;
  private static int testTimes = 10;
  private final List<Host> hostList;
  private final List<Vm> vmList;
  private final List<Cloudlet> cloudletList;
  private final DatacenterBroker broker;
  private final CloudSim simulation;
  private final String fileName;
  Random random = new Random();
  private String SheetName;
  private String ValueName;
  private Cloudlet cloudlet = null;

  /** Default constructor that builds and starts the simulation. */
  private DynamicCloudletsArrival2() throws IOException {
    /*Enables just some level of log messages.
    Make sure to import org.cloudsimplus.util.Log;*/
    this.ValueName = "DisContract";
    this.fileName = "result_ContractRate";
    this.SheetName = "first_sheetName";
    System.out.println("Starting " + getClass().getSimpleName());
    simulation = new CloudSim();

    this.hostList = new ArrayList<>();
    this.vmList = new ArrayList<>();
    this.cloudletList = new ArrayList<>();
    createDatacenter(DATACENTER_NUMBER);
    this.broker = new DatacenterBrokerSimple(simulation);

    List<Vm> vmList = createVmList(VMS_NUMBER );
    this.vmList.addAll(vmList);

    createAndSubmitCloudletsOnVmList();

    runSimulationAndPrintResults();

    printContractRate(cloudletList);

    System.out.println("  " + getSheetName() + " algorithm Simulation finished!");
  }

  /**
   * Starts the example execution, calling the class constructor\ to build and run the simulation.
   *
   * @param args command line parameters
   */
  public static void main(String[] args) throws IOException {
    //    for (int i = 0; i < testTimes; ++i) {
    new DynamicCloudletsArrival2();
    //    }
    System.out.println("??????????????????: " + (1.0 * sumRate / testTimes * 100) + "%");
  }

  private static void printCloudletList(List<Cloudlet> list) {
    // ?????????????????????cloudlet
    LinkedHashSet<Cloudlet> hashSet = new LinkedHashSet<>(list);
    list = new ArrayList<Cloudlet>(hashSet);

    int size = list.size();
    Cloudlet cloudlet;

    DecimalFormat dft = new DecimalFormat("###.##");
    int ContractNum = 0;
    for (int i = 0; i < size; i++) {
      cloudlet = list.get(i);
      System.out.println(
          "???" + i + "???cloudlet_???id???" + cloudlet.getId() + ",deadline = " + cloudlet.getDeadline());
    }
  }

  private void createAndSubmitCloudletsOnVmList() {
    int preid = cloudletList.size();
    double submissionDelay = random.nextInt(5) + 5;
    List<Cloudlet> list = new ArrayList<>(CLOUDLETS_NUMBER);

    Cloudlet cloudlet = createCloudlet(preid, broker);
    setCloudlet(cloudlet);

//    Vm vm = bindCloudletToVm(this.vmList, cloudlet, broker, CloudletToVM_CTVOS); // ???????????????
//          Vm vm = bindCloudletToVm(this.vmList,cloudlet,broker,CloudletToVM_GREEDY);//????????????
          Vm vm = bindCloudletToVm(this.vmList,cloudlet,broker,CloudletToVM_RoundRobin);//????????????
    cloudlet.setVm(vm);
    if (vm == null) {
      System.out.println("vm == null");
    }
    vm.getCloudletsOnVm().add(cloudlet);

    submissionDelay += 15;
//    cloudlet.setSubmissionDelay(submissionDelay);
    cloudletList.add(cloudlet);

    if (cloudletList.size() < CLOUDLETS_NUMBER) {
        cloudlet.addOnStartListener(this::cloudletFinishListener);
//        simulation.addOnClockTickListener(this::createRandomCloudlets);
    }
    broker.submitCloudlet(cloudlet);
  }

  /**
   * Creates a cloudlet with pre-defined configuration.
   *
   * @param id Cloudlet id
   * @param broker the broker that will submit the cloudlets
   * @return the created cloudlet
   */
  private Cloudlet createCloudlet(int id, DatacenterBroker broker) {
    long fileSize = random.nextInt(100) + 250;
    long outputSize = random.nextInt(100) + 250;
    long length = random.nextInt(5000) + 18000; // in number of Million Instructions (MI)
    int pesNumber = 1;
    UtilizationModel utilizationModel = new UtilizationModelFull();

    return new CloudletSimple(id, 10000, pesNumber)
        .setFileSize(fileSize)
        .setOutputSize(outputSize)
        .setUtilizationModel(utilizationModel)
        .setDeadline(random.nextDouble(length / 170) + random.nextDouble(1.0 * length / 210) * 3);
  }

  /**
   * Creates a VM with pre-defined configuration.
   *
   * @param id the VM id
   * @param broker the broker that will be submit the VM
   * @return the created VM
   */
  private List<Vm> createVms(int id, int nums, DatacenterBroker broker) {
    List<Vm> vmList = new ArrayList<Vm>(nums);
    for (int i = id; i < nums + id; i++) {
      int mips = random.nextInt(170) + 150;
      long size = 1000; // image size (Megabyte)
      int ram = 512; // vm memory (Megabyte)
      long bw = 1000;

      Vm vm =
          new VmSimple(i, 300, VM_PES_NUMBER)
              .setRam(ram)
              .setBw(bw)
              .setSize(size)
              .setCloudletScheduler(new CloudletSchedulerSpaceShared());

      vmList.add(vm);
    }
    return vmList;
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
    Collections.sort(vmList, new VmComparator()); // vm???MIPS????????????

    System.out.println("==============");
    if (deadline >= getPretime(cloudlet, vmList.get(vmList.size() - 1))) { // ???????????????
      if (cloudlet.getSensivityType() == 0) { // ???????????????????????????????????????
        System.out.println(
            "deadline>=pretime?????? && sentype=1??????"
                + "cloudlet_"
                + cloudlet.getId()
                + "???????????????"
                + "vm_"
                + (vmList.size() - 1)
                + "???");
        return vmList.get(vmList.size() - 1);
      }
      if (cloudlet.getSensivityType() == 1) { // ??????????????????
        System.out.println(
            "deadline>=pretime?????? && sentype=1??????"
                + "cloudlet_"
                + cloudlet.getId()
                + "???????????????"
                + "vm_"
                + (vmList.size() / 2)
                + "???");
        return vmList.get(vmList.size() / 2);
      }
      if (cloudlet.getSensivityType() == 2) { // ?????????????????????
        System.out.println(
            "deadline>=pretime?????? && sentype=2??????"
                + "cloudlet_"
                + cloudlet.getId()
                + "???????????????"
                + "vm_"
                + (0)
                + "???");
        return vmList.get(0);
      }
    } else if (deadline <= getPretime(cloudlet, vmList.get(0))) { // ????????????????????????????????????????????????????????????????????????
      System.out.println(
          "deadline<=pretime ?????????"
              + "cloudlet_"
              + cloudlet.getId()
              + "???????????????"
              + "vm_"
              + (vmList.size() - 1)
              + "???");
      return vmList.get(vmList.size() - 1);
    } else { // deadline?????????
      if (cloudlet.getSensivityType() == 0) { // ???????????????????????????????????????deadline?????????
        for (int i = 0; i < vmList.size(); i++) {
          if (getPretime(cloudlet, vmList.get(i)) >= cloudlet.getDeadline()) {
            System.out.println(
                "deadline????????? && sentype=0?????????,"
                    + "cloudlet_"
                    + cloudlet.getId()
                    + "???????????????"
                    + "vm_"
                    + (i - 1)
                    + "???");
            return vmList.get(i - 1);
          }
        }
      }
      if (cloudlet.getSensivityType() == 1) {
        for (int i = 0; i < vmList.size(); i++) {
          if (getPretime(cloudlet, vmList.get(i)) >= cloudlet.getDeadline()) {
            System.out.println(
                "deadline????????? && sentype=1??????"
                    + "cloudlet_"
                    + cloudlet.getId()
                    + "???????????????"
                    + "vm_"
                    + (vmList.get((i - 1) / 2))
                    + "???");
            return vmList.get((i - 1) / 2);
          }
        }
      }
      if (cloudlet.getSensivityType() == 2) {
        System.out.println(
            "deadline????????? && sentype=2??????"
                + "cloudlet_"
                + cloudlet.getId()
                + "???????????????"
                + "vm_"
                + (0)
                + "???");
        return vmList.get(0);
      }
    }
    System.out.println("cloudlet_" + cloudlet.getId() + "???????????????" + "vm_" + vmid + "???");
    return vmList.get(0);
  }

  /* Place cloudlet to a vm based on Round-Robin algorithm. */
  private Vm bindCloudletToVm_RoundRobin(Cloudlet cloudlet, List<Vm> vmList) {
    return vmList.get((int) ((cloudlet.getId()) % (vmList.size())));
  }

  /* Place cloudlet to a vm based on greeay algorithm. */
  private Vm bindCloudletToVm_GREEDY(Cloudlet cloudlet, List<Vm> vmList) {

    List<Cloudlet> cloudletList = new ArrayList<Cloudlet>(this.cloudletList.size());
    cloudletList.addAll(this.broker.getCloudletSubmittedList());
    cloudletList.add(cloudlet);

    int cloudletNum = cloudletList.size();
    int vmNum = vmList.size();
    // time[i][j] ????????????i????????????j??????????????????
    double[][] time = new double[cloudletNum][vmNum];
    // cloudletList???MI????????????, vm???MIPS????????????
    Collections.sort(cloudletList, new CloudletComparator());
    Collections.sort(vmList, new VmComparatorAwared());

    for (int i = 0; i < cloudletNum; i++) {
      for (int j = 0; j < vmNum; j++) {
        time[i][j] = (double) cloudletList.get(i).getLength() / vmList.get(j).getMips();
      }
    }

    double[] vmLoad = new double[vmNum]; // ?????????????????????????????????????????????
    int[] vmTasks = new int[vmNum]; // ?????????Vm????????????????????????
    double minLoad = 0; // ??????????????????????????????????????????
    int idx = 0; // ????????????????????????????????????????????????????????????
    // ?????????cloudlet??????????????????vm
    vmLoad[vmNum - 1] = time[0][vmNum - 1];
    vmTasks[vmNum - 1] = 1;
    //	CloudletList.getById(getCloudletList(), cloudletId).setVmId(vmId);
    if (cloudlet.getId() == 0) {
      return vmList.get(vmNum - 1);
    }
    //  cloudletList.get(0).setVm(vmList.get(vmNum-1));
    for (int i = 1; i < cloudletNum; i++) {
      minLoad = vmLoad[vmNum - 1] + time[i][vmNum - 1];
      idx = vmNum - 1;
      for (int j = vmNum - 2; j >= 0; j--) {
        // ????????????????????????????????????,?????????????????????????????????????????????????????????
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
        // ?????????????????????
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

  private List<Vm> createVmList(int nums) {
    List<Vm> list = new ArrayList<>(VMS_NUMBER );
    list = createVms(0, nums, broker);
    broker.submitVmList(list);
    return list;
  }

  /**
   * Creates a Datacenter with pre-defined configuration.
   *
   * @return the created Datacenter
   */
  private void createDatacenter(int num) {
    for (int i = 0; i < num; ++i) {
      Host host = createHost(i);
      hostList.add(host);
      new DatacenterSimple(simulation, hostList, new VmAllocationPolicySimple());
    }
  }

  /**
   * Creates a host with pre-defined configuration.
   *
   * @param id The Host id
   * @return the created host
   */
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

  private void runSimulationAndPrintResults() {
    //      simulation.addOnClockTickListener(this::createRandomCloudlets);
    simulation.start();
    List<Cloudlet> cloudlets = broker.getCloudletFinishedList();
    new CloudletsTableBuilder(cloudlets).build();
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

  private double getPretime(Cloudlet cloudlet, Vm vm) {
    double preWaitTime = 0.0;
    Queue<Cloudlet> queue = vm.getCloudletsOnVm();
    if (queue == null) {
      System.out.println("queue == null");
    }
    while (queue.peek() != null) {
      Cloudlet cloudlet1 = queue.poll();
      preWaitTime += cloudlet1.getLength() / vm.getMips();
      System.out.println(
          "cloudlet_"
              + cloudlet1.getId()
              + ",????????????Vm_"
              + vm.getId()
              + "????????????_"
              + cloudlet1.getLength()
              + "/"
              + vm.getMips()
              + "="
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

  private void cloudletFinishListener(final CloudletVmEventInfo info) {
    System.out.printf(
        "\t# %.2f: Requesting creation of new Cloudlet after %s finishes executing.%n",
        info.getTime(), info.getCloudlet());
    createAndSubmitCloudletsOnVmList();
  }

  public Cloudlet getCloudlet() {
    return cloudlet;
  }

  public void setCloudlet(Cloudlet cloudlet) {
    this.cloudlet = cloudlet;
  }

  public void dataToExcel(String ValueName, double value) throws IOException {
    File file1 = new File("D:\\testData\\" + this.fileName + ".xls");
    if (!file1.exists()) {
      file1.createNewFile();
    }
    FileInputStream inputStream = new FileInputStream(file1);
    // ??????Excel?????????
    HSSFWorkbook workbook = new HSSFWorkbook(inputStream);
    // ???????????????sheeet
    HSSFSheet sheet = workbook.getSheet(SheetName);
    if (sheet == null) {
      System.out.println("????????????" + SheetName + "?????????");
      sheet = workbook.createSheet(SheetName);
    }
    // ???????????????
    HSSFRow row = sheet.createRow(0);
    int lastRowNum = sheet.getLastRowNum();

    if (lastRowNum == 0) {
      String[] title = {"???n?????????", ValueName};
      HSSFCell cell = null;
      for (int i = 0; i < title.length; i++) {
        cell = row.createCell(i);
        cell.setCellValue(title[i]);
      }
    }

    // ????????????
    HSSFRow nextrow = sheet.createRow(lastRowNum + 1);
    System.out.println("?????? " + (lastRowNum + 1) + "???????????????");
    HSSFCell cell2 = nextrow.createCell(0);
    cell2.setCellValue("???" + lastRowNum + "?????????");
    cell2 = nextrow.createCell(1);
    cell2.setCellValue(value);

    FileOutputStream stream = FileUtils.openOutputStream(file1);
    workbook.write(stream);
    stream.close();
  }

  public String getSheetName() {
    return this.SheetName;
  }

  public void setSheetName(String sheetName) {
    this.SheetName = sheetName;
  }

  public void setValueName(String ValueName) {
    this.ValueName = ValueName;
  }

  public double getpreArriveTime() {
    List<Cloudlet> submittedCloudlets = broker.getCloudletSubmittedList();
    List<Vm> vmList = broker.getVmCreatedList();
    double Alllength = 0;
    double AllVmMips = 0;
    for (Cloudlet cloudlet : submittedCloudlets) {
      Alllength += cloudlet.getLength();
    }
    for (Vm vm : vmList) {
      AllVmMips += vm.getMips();
    }
    return 1.0 * Alllength / AllVmMips;
  }

  // Cloudlet??????MI????????????
  private class CloudletComparator implements Comparator<Cloudlet> {
    @Override
    public int compare(Cloudlet cl1, Cloudlet cl2) {
      return (int)
          (cl2.getLength()
              + cl2.getDeadline() * 50000
              - cl1.getLength()
              + cl1.getDeadline() * 5000);
    }
  }
  // Vm??????PredictTime????????????
  private class VmComparator implements Comparator<Vm> {
    @Override
    public int compare(Vm vm1, Vm vm2) {
      return (int) (vm1.getPredictTime(getCloudlet()) - vm2.getPredictTime(getCloudlet()));
    }
  }

  // Vm??????MIPS????????????
  private class VmComparatorAwared implements Comparator<Vm> {
    @Override
    public int compare(Vm vm1, Vm vm2) {
      return (int) (vm1.getPredictTime(getCloudlet()) - vm2.getPredictTime(getCloudlet()));
    }
  }

    private void createRandomCloudlets(final EventInfo evt) {
        if(new UniformDistr().sample() <= 0.3){
            createAndSubmitCloudletsOnVmList();
            System.out.printf("%n# Randomly creating 1 Cloudlet_"+cloudlet.getId()+" at time %.2f%n", evt.getTime());
        }
    }
}
