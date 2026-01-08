package com.lilin;

public class EndTaskThread extends Thread {
    private Task task;
    private int srcDeviceId;

    public EndTaskThread(Task task,  int srcDeviceId) {
        this.task = task;
        this.srcDeviceId = srcDeviceId;
    }

    @Override
    public void run() {
        APP app = SimManager.getInstance().getLoadGeneratorModel().getMobileDevices().get(0).getApp().get(task.getAppid());

        long delay = SimManager.getInstance().getNetworkModel().calculate_EtoM_Delay(app.getOutputsize(), srcDeviceId);

        try {
            // 模拟网络传输延迟
            Thread.sleep(delay);  // 毫秒
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // 任务到达标志
        app.getDag().wait_pre.countDown();
    }
}
