/*
 * Title:        EdgeCloudSim - Simulation Manager
 *
 * Description:
 * SimManager is an singleton class providing many abstract classeses such as
 * Network Model, Mobility Model, Edge Orchestrator to other modules
 * Critical simulation related information would be gathered via this class
 *
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package com.lilin;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class SimManager {
    private String orchestratorPolicy;
    private int numOfMobileDevice;
    private ScenarioFactory scenarioFactory;
    private String useScenario;
    public double lastAvaCompleteTim;
    private List<Long> result;
    public double avaCompleteTim;
    public CountDownLatch wait_complete;
    private volatile boolean running;
    public int OverDeadline;
    private double timeout_tolerance;
    private APP currentApp;
    private final Lock lock = new ReentrantLock();

    private NetWork networkModel;
    private EdgeDeviceGeneratorModel edgeDeviceGeneratorModel;
    private LoadGeneratorModel loadGeneratorModel;

    private Scheduler scheduler;

    private static SimManager instance = null;

    public SimManager(ScenarioFactory _scenarioFactory) {
        scenarioFactory = _scenarioFactory;
        result = new ArrayList<>();
        instance = this;
        this.OverDeadline = 0;
        this.running = true;
    }

    public static SimManager getInstance(){
        return instance;
    }

    public void setEdgeDeviceGeneratorModel() {
        System.out.println("Creating Devices...");
        edgeDeviceGeneratorModel = scenarioFactory.getDeviceGeneratorModel();
        edgeDeviceGeneratorModel.initialize();
        System.out.println("Done.");
    }

    public void setLoadGeneratorModel() {
        System.out.println("Creating Loads...");
        loadGeneratorModel = scenarioFactory.getLoadGeneratorModel();
        loadGeneratorModel.generatorAPPs();
        wait_complete = new CountDownLatch(loadGeneratorModel.getApp_num());
        System.out.println("Done.");
    }

    public void setNetworkModel() {
        //Generate network model
        System.out.println("Creating Networks...");
        networkModel = scenarioFactory.getNetworkModel();
        networkModel.init(edgeDeviceGeneratorModel.getEdgeDevices(),loadGeneratorModel.getMobileDevices().get(0), edgeDeviceGeneratorModel.getNativeDevicesMap());
        System.out.println("Done.");
    }

    public void setScheduler() {
        System.out.println("Creating Scheduler...");
        scheduler = scenarioFactory.getScheduler();
        System.out.println("Done.");
    }

    /**
     * Triggering CloudSim to start simulation
     */


    public void startDevice() {
        //Starts the simulation

        System.out.println("start devices...");
        for (EdgeDevice edgeDevice : edgeDeviceGeneratorModel.getEdgeDevices()) {
            edgeDevice.startDevice();
        }
        loadGeneratorModel.getMobileDevices().get(0).startDevice();
        System.out.println("device start done.");
    }

    public void startSentApp() {
        System.out.println("start startSentApp...");
        for(MobileDevice mobileDevice : loadGeneratorModel.getMobileDevices()){
            mobileDevice.startGenApps();
        }
        System.out.println("mobile startSentApp done.");

    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public String getOrchestratorPolicy(){return orchestratorPolicy;}

    public ScenarioFactory getScenarioFactory(){return scenarioFactory;}

    public int getNumOfMobileDevice(){return numOfMobileDevice;}

    public String getUseScenario(){return useScenario;}

    public void setOrchestratorPolicy(String orchestratorPolicy){this.orchestratorPolicy = orchestratorPolicy;}

    public void setNumOfMobileDevice(int numOfMobileDevice){this.numOfMobileDevice = numOfMobileDevice;}

    public void setUseScenario(String useScenario){this.useScenario = useScenario;}

    public double getTimeout_tolerance(){return timeout_tolerance;}
    public void setTimeout_tolerance(double timeout_tolerance){this.timeout_tolerance = timeout_tolerance;}

    public NetWork getNetworkModel(){
        return networkModel;
    }

    public LoadGeneratorModel getLoadGeneratorModel(){
        return loadGeneratorModel;
    }

    public EdgeDeviceGeneratorModel getEdgeDeviceGeneratorModel(){
        return edgeDeviceGeneratorModel;
    }

    // 停止线程的公共方法
    public void stopRunning() {
        running = false;
    }

    public void Running(){
        running = true;
    }

    public boolean isRunning() { return running; }

    public Map<String,Object> getState(double diff, boolean episodeDone){
        if(!episodeDone)
            currentApp = scheduler.getApp();
        else
            currentApp = null;
        Map<String,Object> state = new HashMap<>();
        List<EdgeDevice> devices = edgeDeviceGeneratorModel.getEdgeDevices();
        MobileDevice mobile = loadGeneratorModel.getMobileDevices().get(0);

        if(currentApp != null) {
            //获取当前应用的依赖
            state.put("depMatrix",currentApp.getDag().getMatrix());

            //获取每个任务的计算量，输入量和输出量
            double[][] taskP = new double[(int)SimSettings.appLookUpTable[4]][3];
            for (int i = 0; i < taskP.length; i++) {
                if (i < currentApp.getDag().getTasks().size()) {
                    taskP[i][0] = currentApp.getDag().getTasks().get(i).getSize();
                    taskP[i][1] = currentApp.getDag().getTasks().get(i).getInput_size();
                    taskP[i][2] = currentApp.getDag().getTasks().get(i).getOutput_size();
                } else {
                    taskP[i][0] = 0;
                    taskP[i][1] = 0;
                    taskP[i][2] = 0;
                }
            }
            state.put("taskP",taskP);

            state.put("num_current_subtasks", currentApp.getDag().getTasks().size());
        }

        double[] diffArray = new double[1];
        diffArray[0] = diff;
        state.put("diff",diffArray);

        //获取当前服务器的处理能力和可用时间
        double[] mips = new double[devices.size()+1];
        long[] avTimes = new long[devices.size()+1];
        for(int i=0; i<devices.size()+1; i++){
            if(i!=devices.size()){
                mips[i] = devices.get(i).getMips();
                if(devices.get(i).getQueueTask_EstimateMaxComplete() ==0)
                    avTimes[i] = 0;
                else
                    avTimes[i] = devices.get(i).getQueueTask_EstimateMaxComplete() - System.currentTimeMillis();
            }else {
                mips[i] = mobile.getMips();
                if(mobile.getQueueTask_EstimateMaxComplete() ==0)
                    avTimes[i] = 0;
                else
                    avTimes[i] = mobile.getQueueTask_EstimateMaxComplete() - System.currentTimeMillis();
            }
        }
        state.put("mips",mips);
        state.put("avTimes",avTimes);

        //获取当前网络状态
        state.put("netWork",networkModel.getDelayMatrix());

        state.put("AppNum",loadGeneratorModel.getApp_num());

        return state;
    }

    public double[] applyAction(int[] actions){
        return  scheduler.scheduleApp(currentApp,actions,loadGeneratorModel.getMobileDevices().get(0));
    }

    public void setAvaCompleteTim(){
        lock.lock();
        try {
            lastAvaCompleteTim = avaCompleteTim;
            double x=0;
            for (Long aLong : result) {
                x += aLong;
            }
            avaCompleteTim = x/result.size();
        }finally {
            lock.unlock();
        }
    }

    public void addResult(long time){
        lock.lock();
        try {
            result.add(time);
        }finally {
            lock.unlock();
        }
    }

    public void clearResult(){
        lock.lock();
        try {
            result.clear();
        }finally {
            lock.unlock();
        }
    }

    public double getReward(){
        lock.lock();
        try {
            return lastAvaCompleteTim - avaCompleteTim;
        }finally {
            lock.unlock();
        }
    }

    public void updateApp(){
        for(MobileDevice device : loadGeneratorModel.getMobileDevices()){
            for(APP app : device.getApp()){
                long currentTime = System.currentTimeMillis();
                app.setStartTime(currentTime + app.getOffsetTime());
                app.setDeadline(app.getStartTime() + app.getExecutionTime());
                app.setCompleteTime(0);
                app.setMakeSpan(0);
                // 设置任务前驱同步信号量
                int lastTaskNum = 0;
                for(Task task : app.getDag().getTasks()){
                    if(task.getPredecessors().isEmpty())
                        task.wait_pre = new CountDownLatch(1);
                    else
                        task.wait_pre = new CountDownLatch(task.getPredecessors().size());
                    if(task.getSuccessors().isEmpty())
                        lastTaskNum++;
                    task.setDevice_Id(-1);
                    task.setEstimate_complete_time(0);
                    task.allocate_semaphore = new Semaphore(0);
                }
                app.getDag().wait_pre = new CountDownLatch(lastTaskNum);
            }
        }
    }

}

