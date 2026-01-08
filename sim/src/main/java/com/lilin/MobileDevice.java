package com.lilin;

import java.util.List;

public class MobileDevice extends EdgeDevice{
    private List<APP> appList;

    public MobileDevice(List<APP> appList, double latitude, double longitude, int attractiveness, int deviceId,
                        double downloadSpeed, double uploadSpeed,double pow) {
        super(deviceId,pow,latitude,longitude,attractiveness,downloadSpeed,uploadSpeed);
        this.appList = appList;
    }

    public List<APP> getApp() {
        return appList;
    }

    // 发送任务输出数据到后继任务的设备
    private void startSentAppInputs(APP app){
        for(Task sucTask : app.getDag().getTasks()){
            if(sucTask.getPredecessors().isEmpty())
                new Thread(() -> sent(app.getInputsize(), sucTask)).start(); // 使用线程池来处理传输任务
        }
    }

    private void sent(double inputSize,Task sucTask) {
        if(sucTask.getDevice_Id() == -1) {
            try {
                // 等待后继任务分配设备
                sucTask.allocate_semaphore.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        AppInptTransferThread firstTaskTransferThread = new AppInptTransferThread(inputSize, sucTask);
        firstTaskTransferThread.start();
    }

    // 启动移动设备，开始生成app
    public void startGenApps() {
        new Thread(() -> {
            if (appList == null || appList.isEmpty()) {
                System.out.println("appList is empty!");
            }else {
                for (APP app : appList) {
                    long waitTime = System.currentTimeMillis() - app.getStartTime();
                    if (waitTime < 0) {
                        try {
                            Thread.sleep(-waitTime);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    new Thread(() -> scheduleApp(app)).start();
                    Thread.yield();
                }
            }
        }).start();
    }

    // 处理当前app
    private void scheduleApp(APP app) {
        app.setStartTime(System.currentTimeMillis());
        SimManager.getInstance().getScheduler().addApp(app);//将app交给调度器
        startSentAppInputs(app);
        new Thread(() -> is_complete(app)).start();
    }

    public void is_complete(APP app){
        try {
            // 等待前驱任务到达
            app.getDag().wait_pre.await();
            System.out.println("mobile_" + getDeviceId() +  " app_" + app.getAppid() + " 完成");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        //app完成，设置相应完成量
        long time = System.currentTimeMillis();
        app.setCompleteTime(time);
        long makeSpan = time - app.getStartTime();
        app.setMakeSpan(makeSpan);

        if(time > app.getDeadline())
            SimManager.getInstance().OverDeadline++;
        SimManager.getInstance().addResult(makeSpan);
        SimManager.getInstance().setAvaCompleteTim();
        SimManager.getInstance().wait_complete.countDown();
    }

}
