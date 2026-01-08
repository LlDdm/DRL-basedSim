package com.lilin;

public class OutputTransferThread extends Thread{
    private double dataSize;
    private Task sucTask;
    private int srcDeviceId;
    private int f;

    public OutputTransferThread(double dataSize, Task sucTask,  int srcDeviceId, int f) {
        this.dataSize = dataSize;
        this.sucTask = sucTask;
        this.srcDeviceId = srcDeviceId;
        this.f = f;
    }

    @Override
    public void run() {
        long delay;
        if(f==0)
            delay = SimManager.getInstance().getNetworkModel().calculate_EtoM_Delay(dataSize, srcDeviceId);
        else if(f==1)
            delay = SimManager.getInstance().getNetworkModel().calculate_MtoE_Delay(dataSize, sucTask.getDevice_Id());
        else
            delay = SimManager.getInstance().getNetworkModel().calculate_EtoE_Delay(dataSize, srcDeviceId,sucTask.getDevice_Id());

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
