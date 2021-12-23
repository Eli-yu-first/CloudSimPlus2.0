package org.cloudsimplus.examples.deadlinBasedsimulations;

import org.apache.commons.io.FileUtils;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
import org.cloudbus.cloudsim.vms.VmCost;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudsimplus.listeners.CloudletVmEventInfo;
import org.cloudsimplus.listeners.EventInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * 变化不同的任务频率-违约率-我们的时间敏感算法-改变不同的任务频率
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
public class DynamicCloudletsArrival2test_11 {
  /** Number of Processor Elements (CPU Cores) of each Host. */
  private static final int HOST_PES_NUMBER = 5;

  /** Number of Processor Elements (CPU Cores) of each VM and cloudlet. */
  private static final int VM_PES_NUMBER = 1;

  private static final int DATACENTER_NUMBER = 2;
  /** Number of Vms to create simultaneously. */
  private static final int CloudletToVM_RoundRobin = 2; // 轮询算法
  private static final int CloudletToVM_CTVOS = 0; // 我们的询算法
  private static final int CloudletToVM_GREEDY =1; // 贪心算法
  private static final int CloudletToVM_GREEDY_Hy = 3; // 贪心算法_对比
  private static int mathordType = 0;
  private static double deadlineSpan = 0.0;
  private static File file = new File("D:\\testData\\retult.txt");
  private static int testTimes = 100;
  private static double sumRate = 0.0;
  private static double finishTime = 0.0;
  private static double waittingTime = 0.0;
  private static double submissionTime = 0.0;
  private static double[] Cost = new double[5];
  private final ContinuousDistribution random1;
  private final List<Host> hostList;
  private final List<Vm> vmList;
  private final List<Cloudlet> cloudletList;
  private final DatacenterBroker broker;
  private final CloudSim simulation;
  private final String fileName;
  Random random = new Random();
  private double submissionDelay = 0;
  private String SheetName;
  private String ValueName;
  private Cloudlet cloudlet = null;
  private static final int VMS_NUMBER = 7;
  private static  int CLOUDLETS_NUMBER = 10;

  private Cloudlet createCloudletsOnVmList() {
    int preid = cloudletList.size();
    List<Cloudlet> list = new ArrayList<>(CLOUDLETS_NUMBER);
    Cloudlet cloudlet = createCloudlet(preid, broker);
    setCloudlet(cloudlet);

  Vm vm = bindCloudletToVm(this.vmList, cloudlet, broker, mathordType); // 我们的算法
//  Vm vm = bindCloudletToVm(this.vmList, cloudlet, broker, CloudletToVM_CTVOS); // 我们的算法
//  Vm vm = bindCloudletToVm(this.vmList, cloudlet, broker, CloudletToVM_GREEDY); // 贪心算法
//  Vm vm = bindCloudletToVm(this.vmList, cloudlet, broker, CloudletToVM_RoundRobin); // 轮询算法
//  Vm vm = bindCloudletToVm(this.vmList, cloudlet, broker, CloudletToVM_GREEDY_Hy); // 轮询算法
    cloudlet.setVm(vm);

    vm.getCloudletsOnVm().add(cloudlet);
      cloudlet.setSubmissionDelay(submissionDelay);
      submissionDelay += random.nextDouble(10);//默认是10

    return cloudlet;
  }
    /** Default constructor that builds and starts the simulation. */
    private DynamicCloudletsArrival2test_11() throws IOException {
        random1 = new UniformDistr();
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

        List<Vm> vmList = createVmList(VMS_NUMBER);

        this.vmList.addAll(vmList);

        createAndSubmitCloudletsOnVmList(CLOUDLETS_NUMBER);

        runSimulationAndPrintResults();

        printContractRate(cloudletList);
        printFinishTime(cloudletList);
        printWaittingTime(cloudletList);
        printTotalVmsCost(this.vmList);

        System.out.println("  " + getSheetName() + " algorithm Simulation finished!");
    }

    /**
     * Starts the example execution, calling the class constructor\ to build and run the simulation.
     *
     * @param args command line parameters
     */
    public static void main(String[] args) throws IOException {
        for(int k =0;k<=3;k++){
            mathordType = k;
            double TF = 8.0;
            for(int j = 2;TF <= 15;j++){
                sumRate = 0;
                finishTime = 0;
                waittingTime = 0;
                Cost = new double[]{0, 0, 0, 0,0};
                CLOUDLETS_NUMBER = 500;
                deadlineSpan = TF;
                for (int i = 0; i < testTimes; ++i) {
                    new DynamicCloudletsArrival2test_11();
                }

                System.out.println("平均违约率为: " + (1.0 * sumRate / testTimes * 100) + "%");
                System.out.println("平均完成时间为: " + (1.0 * finishTime / testTimes) + " s");
                System.out.println("平均等待时间为: " + (1.0 * waittingTime / testTimes) + " s");
                System.out.printf("平均完成成本为: processingCost: %5.2f$ ,memoryCost:%5.2f$ ,storageCost:%5.2f$ ,bwCost:%5.2f$ ,totalCost:%5.2f$ %n", Cost[0] / testTimes, Cost[1] / testTimes, Cost[2] / testTimes, Cost[3] / testTimes, Cost[4] / testTimes);
//                dataToExcel(0,j/100+2,' B',(1.0 * finishTime / testTimes)  );
                dataToExcel(0,j+1,k+1,(1.0 * sumRate / testTimes * 100 )   );
                dataToExcel(1,j+1,k+1,(1.0 * finishTime / testTimes)  );
                dataToExcel(2,j+1,k+1,(1.0 * waittingTime / testTimes) );
                dataToExcel(3,j+1,k+1,Cost[4] / testTimes );
                TF+=0.5;
            }
        }

    }
  public String getSheetName() {
    return this.SheetName;
  }

  public void setSheetName(String sheetName) {
    this.SheetName = sheetName;
  }

  private void runSimulationAndPrintResults() {
    simulation.start();

    final List<Cloudlet> finishedCloudlets = broker.getCloudletFinishedList();
    new CloudletsTableBuilder(finishedCloudlets).build();
  }

  /**
   * Creates cloudlets and submit them to the broker, applying a different submission delay for each
   * one (simulating the dynamic cloudlet arrival).
   *
   * @param vm Vm to run the cloudlets to be created
   */

  /**
   * Creates a VM with pre-defined configuration.
   *
   * @param broker the broker that will be submit the VM
   * @return the created VM
   */
  private List<Vm> createVms(int nums, DatacenterBroker broker) {
    List<Vm> vmList = new ArrayList<Vm>(nums);
    for (int i = 0; i < nums; ++i) {
      //      int mips = random.nextInt(150) + 170;
      int j = i % 6;
      int mips = 40 * i + 170;
      long size = 10000; // image size (Megabyte)
      int ram = 512; // vm memory (Megabyte)
      long bw = 1000;

      Vm vm =
          new VmSimple(i, mips, 1)
              .setRam(ram)
              .setBw(bw)
              .setSize(size)
              .setCloudletScheduler(new CloudletSchedulerSpaceShared());

      vmList.add(vm);
    }
    return vmList;
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
    //    long length = random.nextInt(5000) + 8000; // in number of Million Instructions (MI)
    long length = (id % 5) * 1000 + 8000; // in number of Million Instructions (MI)
    int pesNumber = 1;
    UtilizationModel utilizationModel = new UtilizationModelFull();
    Cloudlet cloudlet = new CloudletSimple(id, length, pesNumber)
        .setFileSize(fileSize)
        .setOutputSize(outputSize)
        .setUtilizationModel(utilizationModel)
//        .setDeadline(random.nextDouble(10) + length /350+id*1)
        .setDeadline(random.nextDouble(10) + length /200+(broker.getCloudletSubmittedList().size()/VMS_NUMBER)*deadlineSpan)
        .setSensivityType(2);

//      if(30 <=cloudlet.getId() % 50 && cloudlet.getId() % 50 < 40){
//          cloudlet.setSensivityType(1);
//      }
//      if(cloudlet.getId() % 50 >= 40){
//          cloudlet.setSensivityType(0);
//      }
//      if(cloudlet.getId() % 50 < 40){
//          cloudlet.setSensivityType(2);
//      }

      return cloudlet;
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
      case CloudletToVM_GREEDY_Hy:
        setSheetName("RoundRobin_" + ValueName);
        return bindCloudletToVm_GREEDY_Hy(cloudlet, vmList);
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
            "deadline>=pretime都行 && sentype=0不敏感"
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
        int b = vmList.size() - 1;
        int a = vmList.size() / 2;
        b = Math.max(b, a);
        a = Math.min(b, a);
        return vmList.get(a + (int) (Math.random() * (b - a + 1)));
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
    } else if (deadline <= getPretime(cloudlet, vmList.get(0))) { // 所有的都不行，无论敏感度高低，都直接放在第一个
      System.out.println(
          "deadline<=pretime 都不行" + "cloudlet_" + cloudlet.getId() + "被分配到了" + "vm_" + (0) + "上");
      return vmList.get(0);
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
            int b = Math.max((i - 1), vmList.size() / 2);
            int a = Math.min((i - 1), vmList.size() / 2);
            return vmList.get(a + (int) (Math.random() * (b - a + 1)));
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
    return vmList.get((int) ((cloudlet.getId()) % (vmList.size())));
  }

  /* Place cloudlet to a vm based on greeay algorithm. */
  private Vm bindCloudletToVm_GREEDY(Cloudlet cloudlet, List<Vm> vmList) {
    Vm tmp = null;
    double minTime = 100000000000L;
    for (Vm vms : vmList) {
      if (vms.getPredictTime(cloudlet) < minTime) {
        minTime = vms.getPredictTime(cloudlet);
        tmp = vms;
      }
    }
    return tmp;
  }

    private Vm bindCloudletToVm_GREEDY_Hy(Cloudlet cloudlet, List<Vm> vmList) {
        Vm tmp = null;
        double minTime = 100000000000L;
        for (Vm vms : vmList) {
            if (getPretimeWithTrans(cloudlet,vms) < minTime) {
                minTime = vms.getPredictTime(cloudlet);
                tmp = vms;
            }
        }
        return tmp;
    }
    private double getPretimeWithTrans(Cloudlet cloudlet, Vm vm) {
        double preWaitTime = 0.0;
        List<Cloudlet> CloudletWaitingList = broker.getCloudletWaitingList();
        for (Cloudlet CL : CloudletWaitingList) {
            if (CL.getVm() == vm) {
                preWaitTime +=( CL.getLength()+CL.getFileSize()*90) / vm.getMips();
            }
        }
        double execTime = 1.0 * cloudlet.getLength() / vm.getMips();
        return preWaitTime + execTime;
    }
  private List<Vm> createVmList(int nums) {
    List<Vm> list = new ArrayList<>(VMS_NUMBER);
    list = createVms(nums, broker);
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
      Datacenter dc = new DatacenterSimple(simulation, hostList, new VmAllocationPolicySimple());
      dc.getCharacteristics()
          .setCostPerSecond(5)
          .setCostPerMem(0.01)
          .setCostPerStorage(0.001)
          .setCostPerBw(0.05);
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

  private void createAndSubmitCloudletsOnVmList(int nums) {
    for (int i = 0; i < nums; i++) {
      SubmitCloudlets(createCloudletsOnVmList());
    }
  }

  private void SubmitCloudlets(Cloudlet cloudlet) {
    cloudletList.add(cloudlet);
    broker.submitCloudlet(cloudlet);
  }

  public Cloudlet getCloudlet() {
    return cloudlet;
  }

  public void setCloudlet(Cloudlet cloudlet) {
    this.cloudlet = cloudlet;
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

  private void printFinishTime(List<Cloudlet> cloudletList) throws IOException {
    double maxFinishTime = 0.0;
    for (Cloudlet cloudlet : cloudletList) {
      if (cloudlet.getFinishTime() > maxFinishTime) {
        maxFinishTime = cloudlet.getFinishTime();
      }
    }
    finishTime += maxFinishTime;
  }
  private void printWaittingTime(List<Cloudlet> cloudletList) throws IOException {
    double prewaittingTime = 0.0;
    for (Cloudlet cloudlet : cloudletList) {
        prewaittingTime += cloudlet.getWaitingTime();
    }
    waittingTime += prewaittingTime /cloudletList.size();
  }

  private void printTotalVmsCost(List<Vm> vmList) {
    double totalCost = 0.0;
    int totalNonIdleVms = 0;
    double processingTotalCost = 0, memoryTotaCost = 0, storageTotalCost = 0, bwTotalCost = 0;
    for (final Vm vm : vmList) {
      final VmCost cost = new VmCost(vm);
      processingTotalCost += cost.getProcessingCost();
      memoryTotaCost += cost.getMemoryCost();
      storageTotalCost += cost.getStorageCost();
      bwTotalCost += cost.getBwCost();

      totalCost += cost.getTotalCost();
      totalNonIdleVms += vm.getTotalExecutionTime() > 0 ? 1 : 0;
      System.out.println(cost);
    }
    Cost[0] += processingTotalCost / vmList.size();
    Cost[1] += memoryTotaCost / vmList.size();
    Cost[2] += storageTotalCost / vmList.size();
    Cost[3] += bwTotalCost / vmList.size();
    Cost[4] += totalCost / vmList.size();
  }

  private double getPretime(Cloudlet cloudlet, Vm vm) {
    double preWaitTime = 0.0;
    List<Cloudlet> CloudletWaitingList = broker.getCloudletWaitingList();
    for (Cloudlet CL : CloudletWaitingList) {
      if (CL.getVm() == this) {
        preWaitTime += CL.getLength() / vm.getMips();
      }
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
    createAndSubmitCloudletsOnVmList(CLOUDLETS_NUMBER);
  }

  private void createRandomCloudlets(final EventInfo evt) {
    if (random1.sample() <= 0.3) {
      Cloudlet cloudlet = createCloudletsOnVmList();
      cloudletList.add(cloudlet);
      broker.submitCloudlet(cloudlet);
      System.out.printf(
          "%n# Randomly creating 1 Cloudlet_" + cloudlet.getId() + " at time %.2f%n",
          evt.getTime());
    }
  }

  // Cloudlet根据MI降序排列
  private class CloudletComparator implements Comparator<Cloudlet> {
    @Override
    public int compare(Cloudlet cl1, Cloudlet cl2) {
      return (int)
          (cl2.getLength()
              + cl2.getDeadline() * 50000
              - (cl1.getLength() + cl1.getDeadline() * 5000));
    }
  }

  // Vm根据PredictTime升序排列
  private class VmComparator implements Comparator<Vm> {
    @Override
    public int compare(Vm vm1, Vm vm2) {
      return (int) (vm2.getPredictTime(getCloudlet()) - vm1.getPredictTime(getCloudlet()));
    }
  }

  // Vm根据MIPS升序排列
  private class VmComparatorAwared implements Comparator<Vm> {
    @Override
    public int compare(Vm vm1, Vm vm2) {
      return (int) (vm1.getPredictTime(getCloudlet()) - vm2.getPredictTime(getCloudlet()));
    }
  }
    public static void dataToExcel(int SheetIndex,int rowIndex,int comChar, double value) throws IOException {
        rowIndex -= 1;
        int comid = comChar ;
//        int comid = comChar-'A';
        File file1 = new File("D:\\testData\\" + "deadline_Span.xlsx");
        if (!file1.exists()) {
            System.out.println("不存在");
            file1.createNewFile();
        }
        FileInputStream inputStream = new FileInputStream(file1);
        // 创建Excel文件薄
//        HSSFWorkbook workbook = new HSSFWorkbook(inputStream);
        XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
        // 创建工作表sheeet

        XSSFSheet sheet = workbook.getSheetAt(SheetIndex);

        // 创建第一行
        XSSFRow row = sheet.getRow(rowIndex);
        if(row == null){
            row = sheet.createRow(rowIndex);
        }
        XSSFCell cell = row.getCell(comid);
        if(cell == null){
            cell = row.createCell(comid);
        }
        cell.setCellValue(value);

        FileOutputStream stream = FileUtils.openOutputStream(file1);
        workbook.write(stream);
        stream.close();
    }

}
