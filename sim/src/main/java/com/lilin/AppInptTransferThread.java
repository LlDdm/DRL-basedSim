package com.lilin;

public class AppInptTransferThread extends Thread {
    private double dataSize;
    private Task sucTask;

    public AppInptTransferThread(double dataSize, Task sucTask) {
        this.dataSize = dataSize;
        this.sucTask = sucTask;
    }

    @Override
    public void run() {
        long delay = SimManager.getInstance().getNetworkModel().calculate_MtoE_Delay(dataSize,sucTask.getDevice_Id());

        try {
            // 模拟网络传输延迟
            Thread.sleep(delay/20);  // 毫秒
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // 任务到达标志
        sucTask.wait_pre.countDown();
    }
}