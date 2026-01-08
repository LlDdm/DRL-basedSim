package com.lilin;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


class EdgeDevice {
    private int deviceId;
    private double mips;   // 处理能力
    private final double[] location = new double[2];          // 纬度     // 经度
    private int attractiveness;
    private BlockingQueue<Task> taskQueue;    // 任务队列
    private double downloadspeed;
    private double uploadspeed;
    private long QueueTask_EstimateMaxComplete;


    public EdgeDevice(int deviceId, double mips, double latitude, double longitude,
                      int attractiveness, double downloadspeed, double uploadspeed) {
        this.deviceId = deviceId;
        this.mips = mips;
        this.location[0] = latitude;
        this.location[1] = longitude;
        this.attractiveness = attractiveness;
        this.taskQueue = new LinkedBlockingQueue<>();
        this.downloadspeed = downloadspeed;
        this.uploadspeed = uploadspeed;
        this.QueueTask_EstimateMaxComplete = System.currentTimeMillis();
    }

    public int getDeviceId() {
        return deviceId;
    }

    public double getMips() {
        return mips;
    }

    public void addTask(Task task) {
        try {
            taskQueue.put(task);
        }catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void setAttractiveness(int attractiveness) {
        this.attractiveness = attractiveness;
    }
    public int getAttractiveness() {
        return attractiveness;
    }

    public double[] getlocation() {
        return location;
    }

    public BlockingQueue<Task> getTaskQueue() {return taskQueue;}

    public double getDownloadspeed() {
        return downloadspeed;
    }
    public double getUploadspeed() {
        return uploadspeed;
    }

    public long getQueueTask_EstimateMaxComplete() { return QueueTask_EstimateMaxComplete; }
    public void setQueueTask_EstimateMaxComplete(long queueTask_EstimateMaxComplete) {
        if(queueTask_EstimateMaxComplete > QueueTask_EstimateMaxComplete)
            QueueTask_EstimateMaxComplete = queueTask_EstimateMaxComplete;
    }

    // 启动边缘设备，开始监听任务
    public void startDevice() {
        new Thread(() -> {
            while (SimManager.getInstance().isRunning()) {
                if(!taskQueue.isEmpty())
                    operatorTask();  // 持续监听任务
                else
                    Thread.yield();
            }
        }).start();
    }

    // 处理任务
    public  void operatorTask() {
        try {
            Task task = taskQueue.take();
            try {
                // 等待前驱任务到达
                task.wait_pre.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            startTask(task);  // 开始执行任务
        }catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // 执行任务
    public void startTask(Task task) {

        double delay =  task.getSize() * 1000 / mips;
        try {
            // 模拟任务执行
            Thread.sleep((long) delay);  // 假设每个任务执行的时间与任务长度成正比
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Task was interrupted,APP: " + task.getAppid() + " task: " + task.get_taskId());
            return;
        }
       // System.out.println("Device： " + deviceId + " complete mobile: " + task.getMobileDeviceId() + " APP: " + task.getAppid() + " task: " + task.get_taskId());
        sentOutputs(task); // 传输输出数据
    }

    // 发送任务输出数据到后继任务的设备
    public void sentOutputs(Task task) {
        if(task.getSuccessors().isEmpty()){
            EndTaskThread endTaskThread = new EndTaskThread(task, this.getDeviceId());
            endTaskThread.start();
        }else {
            List<Task> sucTasks = task.getSuccessors();
            for (Task sucTask : sucTasks) {
                if(sucTask.getDevice_Id() != deviceId)
                    new Thread(() -> startSent(task, sucTask)).start();
                else
                    sucTask.wait_pre.countDown();
            }
        }
    }

    public void startSent(Task currentTask, Task suctask) {
        if (suctask.getDevice_Id() == -1) {
            try {
                // 等待后继任务分配设备
                suctask.allocate_semaphore.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        OutputTransferThread outputTransferThread;
        if(suctask.getDevice_Id() == SimManager.getInstance().getEdgeDeviceGeneratorModel().getEdgeDevices().size())
            outputTransferThread = new OutputTransferThread(currentTask.getSuccessorsMap().get(suctask), suctask,this.getDeviceId(),0);//后继在本地
        else if(currentTask.getDevice_Id() == SimManager.getInstance().getEdgeDeviceGeneratorModel().getEdgeDevices().size())
            outputTransferThread = new OutputTransferThread(currentTask.getSuccessorsMap().get(suctask), suctask,this.getDeviceId(),1);//当前任务在本地
        else
            outputTransferThread = new OutputTransferThread(currentTask.getSuccessorsMap().get(suctask), suctask,this.getDeviceId(),2);//当前任务和后继都不在本地
        outputTransferThread.start();
    }

}

