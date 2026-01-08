package com.lilin;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Cloud extends EdgeDevice {
    private final ExecutorService executorService;

    public Cloud(int id , double mips, double latitude, double longitude, int attractiveness, double downloadSpeed, double uploadSpeed) {
        super(id, mips, latitude, longitude, attractiveness, downloadSpeed, uploadSpeed);
        this.executorService = Executors.newFixedThreadPool(4); // 创建线程池

    }

    @Override
    // 监听并处理任务
    public void operatorTask() {
        try {
            Task task = getTaskQueue().take();  // 等待队列中任务到来
            try {
                // 等待前驱任务到达
                task.wait_pre.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            executorService.submit(() -> startTask(task));  // 开始执行任务
        }catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public long getQueueTask_EstimateMaxComplete() { return System.currentTimeMillis(); }
}
