package com.lilin;

import org.apache.commons.math3.distribution.ExponentialDistribution;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;

public class LoadGeneratorModel {
    private final Random rng;
    private long loadGenerate_Time;
    private long AppStart_time_offset;
    private int App_num;

    private List<MobileDevice> mobileDevices;

    public LoadGeneratorModel(long _loudGenerate_time, long AppStart_time_offset) {
        this.rng = new Random();
        this.mobileDevices = new ArrayList<>();
        this.loadGenerate_Time = _loudGenerate_time;
        this.AppStart_time_offset = AppStart_time_offset;
        this.App_num = 0;
    }

    public List<MobileDevice> getMobileDevices() {
        return mobileDevices;
    }

    private int generateNormalint(int mean, int stdDev){
        return (int) Math.round(rng.nextGaussian() * stdDev + mean);
    }

    private long generateNormalLong(long mean, long stdDev) {
        return Math.round(rng.nextGaussian() * stdDev + mean);
    }

    private double generateNormalDouble(double mean, double stdDev) {return rng.nextGaussian() * stdDev + mean;}

    private int generateUniformInt(int min, int max) {return rng.nextInt(max-min+1) + min;}

    public int getApp_num(){return App_num;}


    public void generatorAPPs() {
        double [] APPlookuptable = SimSettings.appLookUpTable;

        if (APPlookuptable == null || APPlookuptable.length == 0)
            throw new IllegalStateException("App lookup table is not initialized or is empty.");

        long base_line_start_time = System.currentTimeMillis();

        //Each mobile device utilizes an app type (task type)
        for(int i=0; i<1; i++) {
            List<APP> apps = new ArrayList<>();
            int app_id = 0;

            //随机连接方式：0,lan or 1,wlan or 2,GSM
            int randomConnectionType = 1;

            double poissonMean = APPlookuptable[0];
            double activePeriod = APPlookuptable[1] ;

            double virtualTime = SimUtils.getRandomDoubleNumber(
                    SimSettings.CLIENT_ACTIVITY_START_TIME ,
                    (SimSettings.CLIENT_ACTIVITY_START_TIME + activePeriod));  //active period starts shortly after the simulation started 5 - 45

            ExponentialDistribution ps = new ExponentialDistribution(poissonMean);

            while (virtualTime < loadGenerate_Time) {

                double interval = ps.sample();

                while (interval <= 0) {
                    System.out.println("Impossible is occurred! interval is " + interval + " for device " + i + " time " + virtualTime);
                    interval = ps.sample();
                }
                virtualTime += interval;
                System.out.println(virtualTime);

                double avg_task_size = APPlookuptable[2];

                int max_task_num = (int) APPlookuptable[4];
                int task_num = generateUniformInt(1,max_task_num);

                double avg_tra_size = APPlookuptable[3]; //单位KB

                double appinputSize = avg_tra_size * SimSettings.In_ratio;

                double appoutputSize = avg_tra_size * SimSettings.Out_ratio;

                DAG dag = generateDAG(task_num, max_task_num, avg_task_size,avg_tra_size, app_id,i,appinputSize,appoutputSize);

                // 设置任务前驱同步信号量
                int lastTaskNum = 0;
                for(Task task : dag.getTasks()){
                    if(task.getPredecessors().isEmpty())
                        task.wait_pre = new CountDownLatch(1);
                    else
                        task.wait_pre = new CountDownLatch(task.getPredecessors().size());
                    if(task.getSuccessors().isEmpty())
                        lastTaskNum++;
                }
                dag.wait_pre = new CountDownLatch(lastTaskNum);

                long offsetTime;

                offsetTime = System.currentTimeMillis() - base_line_start_time + (long) ((virtualTime + AppStart_time_offset) * 1000);

                long execution_time = generateUniformInt(15, 120) * 1000L;

                APP app = new APP(app_id, execution_time, offsetTime,appinputSize, appoutputSize, dag, i);
                apps.add(app);
                app_id++;
                App_num++;
            }

            //移动设备位置随机设置
            double[][] edgeDeviceLookUpTable = SimSettings.edgeDeviceLookUpTable;
            int random_edgedevice = generateUniformInt(1,edgeDeviceLookUpTable.length-1);

            double moniledevice_latitude = edgeDeviceLookUpTable[random_edgedevice][2]; // 纬度
            moniledevice_latitude = SimUtils.getRandomDoubleNumber(moniledevice_latitude - 1, moniledevice_latitude + 1);
            double mobiledevice_longitude = edgeDeviceLookUpTable[random_edgedevice][3];
            mobiledevice_longitude = SimUtils.getRandomDoubleNumber(mobiledevice_longitude - 1, mobiledevice_longitude + 1);
            int mobiledevice_attractiveness = ((int) edgeDeviceLookUpTable[random_edgedevice][4]);

            mobileDevices.add(new MobileDevice(apps, moniledevice_latitude, mobiledevice_longitude,
                    mobiledevice_attractiveness, i, edgeDeviceLookUpTable[random_edgedevice][5],
                    edgeDeviceLookUpTable[random_edgedevice][6], SimSettings.mobile_pow));

        }

    }

    public DAG generateDAG(int task_num, int maxWidth, double avg_task_size, double avg_task_tra_size,
                           int appid, int mobileDeviceId,double appInput,double appOutput) {
        List<Task> taskList = new ArrayList<>();

        // 1. 创建虚拟节点
        Task startTask = new Task(-1, 0, appid, mobileDeviceId);  // 起点
        Task endTask = new Task(-2, 0, appid, mobileDeviceId);    // 汇点

        double task_sizeBias = 100;
        // 2. 创建任务节点
        for (int i = 0; i < task_num; i++) {
            double task_size = rng.nextGaussian() * task_sizeBias + avg_task_size;
            Task task = new Task(i, task_size, appid, mobileDeviceId);
            taskList.add(task);
        }

        // 3. 在每层之间生成随机的依赖关系
        List<List<Task>> layers = new ArrayList<>();
        int currentTask_num = 0;

        // 在DAG的深度范围内生成层次
        while (currentTask_num < task_num) {
            List<Task> layer = new ArrayList<>();
            int layerWidth = generateUniformInt(1, task_num);; // 每层的宽度随机
            for (int i = 0; i < layerWidth; i++) {
                Task task = taskList.get(currentTask_num + i);
                layer.add(task);
                if(currentTask_num + i + 1 == task_num)
                    break;
            }
            currentTask_num += layerWidth;
            layers.add(layer);
        }

        List<List<Task>> suctasks= new ArrayList<>(layers);
        // 4. 生成依赖关系：确保每个节点有至少1个,最多有3个前驱节点
        for (int i = 0; i < layers.size()-1 ; i++) {
            List<Task> currentLayerTasks = layers.get(i);
            List<Task> sucLayerTasks = layers.get(i+1);
            suctasks.remove(currentLayerTasks);
            suctasks.remove(sucLayerTasks);

            // 为当前层的任务添加依赖于下一层任务的关系
            for (Task currTask : currentLayerTasks) {
                int suc_num = 0;
                while (suc_num == 0) {
                        for (Task sucTask : sucLayerTasks) {
                            if (rng.nextBoolean()) {
                                double bias = 10;
                                double edgeSize = rng.nextGaussian() * bias + avg_task_tra_size;
                                sucTask.addPredecessor(currTask, edgeSize);
                                currTask.addSuccessor(sucTask, edgeSize);
                                suc_num++;
                            }
                            if(suc_num == 2)
                                break;
                        }
                    if(suc_num >= 1)
                        break;
                }

                // 按概率为当前任务添加一个跨层的后继
                if(!suctasks.isEmpty()) {
                    if (suc_num < 3) {
                        for (List<Task> sucTask_layer : suctasks) {
                            for (Task sucTask : sucTask_layer) {
                                if (rng.nextBoolean()) {
                                    double bias = 10;
                                    double edgeSize = rng.nextGaussian() * bias + avg_task_tra_size;
                                    sucTask.addPredecessor(currTask, edgeSize);
                                    currTask.addSuccessor(sucTask, edgeSize);
                                    suc_num++;
                                }
                                if (suc_num == 3)
                                    break;
                            }
                            if (suc_num == 3)
                                break;
                        }
                    }
                }
            }
        }

        //将依赖表示为压缩后的邻接矩阵
        int k = (maxWidth * (maxWidth-1))/2;
        int[] matrix = new int[k];
        // 初始化矩阵
        Arrays.fill(matrix, 0);
        // 根据依赖填值
        k=0;
        for (int i = 0; i < task_num-1; i++) { // i 代表行索引
            for (int j = i+1; j < task_num; j++) { // j 代表列索引
                if(taskList.get(i).getSuccessors().contains(taskList.get(j))){
                    matrix[k]=1;
                }
                k++;
            }
        }

        //初始化每个任务的输入数据大小和输出数据大小
        for(Task task : taskList){
            double input=0;
            double output=0;
            if(task.getPredecessors().isEmpty()){
                input += appInput;
            }else {
                for(Task pre : task.getPredecessors()){
                    input += task.getPredecessorsMap().get(pre);
                }
            }

            if(task.getSuccessors().isEmpty()){
                output += appOutput;
            }else {
                for(Task suc : task.getSuccessors()){
                    output += task.getSuccessorsMap().get(suc);
                }
            }

            task.setInput_size(input);
            task.setOutput_size(output);
        }

        // 返回DAG及任务列表
        return new DAG(taskList,matrix,startTask,endTask);
    }
}