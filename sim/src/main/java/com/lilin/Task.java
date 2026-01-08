package com.lilin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
//import java.util.concurrent.locks.ReentrantLock;

class Task {
    private int id; // 任务ID
    private double size; // 任务大小
    private int APPid;
    private Map<Task, Double> predecessors; // 前驱任务列表及边的大小
    private Map<Task, Double> successors;   // 后继任务列表及边的大小
    private Map<Task, Long> inDelay;
    private Map<Task, Long> outDelay;
    private long comDelay;
    private double input_size;
    private double output_size;
    private double R;
    private int device_Id;
    private long estimate_complete_time;
    private int mobileDeviceId;
    public Semaphore allocate_semaphore;
    public CountDownLatch wait_pre;

    Task(int id, double size, int APPid, int mobileDeviceId) {
        this.id = id;
        this.size = size;
        this.APPid = APPid;
        this.predecessors = new HashMap<>();
        this.successors = new HashMap<>();
        this.device_Id = -1;
        this.R = 0;
        this.estimate_complete_time = 0;
        this.mobileDeviceId = mobileDeviceId;
        this.inDelay = new HashMap<>();
        this.outDelay = new HashMap<>();
        this.allocate_semaphore = new Semaphore(0);
    }

    // 添加前驱任务并记录边的大小
    public void addPredecessor(Task task, double edgeSize) {
        this.predecessors.put(task, edgeSize);
    }
    // 添加后继任务并记录边的大小
    public void addSuccessor(Task task, double edgeSize) {
        this.successors.put(task, edgeSize);
    }

    public List<Task> getPredecessors() {
        return new ArrayList<>(predecessors.keySet());
    }
    public List<Task> getSuccessors() {
        return new ArrayList<>(successors.keySet());
    }

    public Map<Task, Double> getPredecessorsMap() {
        return predecessors;
    }
    public Map<Task, Double> getSuccessorsMap() {
        return successors;
    }

    public long getComDelay() {
        return comDelay;
    }

    public void setComDelay(long comDelay) {
        this.comDelay = comDelay;
    }
    public Map<Task, Long> getOutDelay() {
        return outDelay;
    }

    public void setOutDelay(Task task, long outDelay) {
        this.outDelay.put(task, outDelay);
    }
    public Map<Task, Long> getInDelay() {
        return inDelay;
    }

    public void setInDelay(Task task, long inDelay) {
        this.inDelay.put(task, inDelay);
    }

    public void setDevice_Id(int device_Id) {
        this.device_Id = device_Id;
    }
    public int getDevice_Id() {
        return device_Id;
    }

    public double getSize() {
        return size;
    }
    public void setSize(long size) {
        this.size = size;
    }

    public int getAppid() {
        return APPid;
    }
    public void setAppid(int appid) {
        this.APPid = appid;
    }

    public double getR() {
        return R;
    }
    public void setR(double time) {
        this.R = time;
    }

    public void set_task_Id(int id) {
        this.id = id;
    }
    public int get_taskId() {
        return id;
    }

    public  void setEstimate_complete_time(long complete_time) { this.estimate_complete_time = complete_time; }
    public long getEstimate_complete_time() { return estimate_complete_time; }

    public void setMobileDeviceId(int mobileDeviceId) { this.mobileDeviceId = mobileDeviceId; }
    public int getMobileDeviceId() { return mobileDeviceId; }

    public void setInput_size(double input_size) {this.input_size = input_size;}
    public double getInput_size() { return input_size; }

    public void setOutput_size(double output_size) {this.output_size = output_size;}
    public double getOutput_size() { return output_size; }


@Override
    public String toString() {
        return "Task{id=" + id + ", size=" + size + "}";
    }
}

