package com.lilin;

import java.util.concurrent.CountDownLatch;

public class APP implements Comparable<APP> {
    private int Appid;
    private long startTime, deadline;
    private long executionTime;
    private long offsetTime;
    private double inputsize;
    private double outputsize;
    private long lenth;
    private DAG dag;
    private long completeTime;
    private int mobileDeviceId;
    private long makeSpan;
    private long EstimateCompleteTime;

    public APP(int Appid,long executionTime,long offsetTime,
               double inputsize, double outputsize, DAG dag, int mobileDeviceId) {
        this.Appid = Appid;
        this.startTime = 0;
        this.deadline = 0;
        this.inputsize = inputsize;
        this.outputsize = outputsize;
        this.dag = dag;
        this.mobileDeviceId = mobileDeviceId;
        this.makeSpan = 0;
        this.completeTime = 0;
        this.executionTime = executionTime;
        this.offsetTime = offsetTime;
        this.EstimateCompleteTime = 0;
    }

    public int getAppid() {
        return Appid;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getDeadline() {
        return deadline;
    }

    public long getCompleteTime() {
        return completeTime;
    }

    public double getInputsize() {
        return inputsize;
    }

    public double getOutputsize() {
        return outputsize;
    }

    public long getLenth() {
        return lenth;
    }

    public DAG getDag() {
        return dag;
    }

    public void setAppid(int appid) {
        Appid = appid;
    }

    public void setCompleteTime(long completeTime) {
        this.completeTime = completeTime;
    }

    public void setDag(DAG dag) {
        this.dag = dag;
    }

    public void setDeadline(long deadline) {
        this.deadline = deadline;
    }

    public void setInputsize(long inputsize) {
        this.inputsize = inputsize;
    }

    public void setLenth(long lenth) {
        this.lenth = lenth;
    }

    public void setOutputsize(long outputsize) {
        this.outputsize = outputsize;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public int getMobileDeviceId() { return mobileDeviceId; }

    //public void setComplete(boolean complete) { isComplete = complete;}
    //public boolean isComplete() {return isComplete;}

    public void setMakeSpan(long makeSpan) {this.makeSpan = makeSpan;}
    public long getMakeSpan() {return makeSpan;}

    public long getExecutionTime() {return executionTime;}
    public void setExecutionTime(long excursionTime) { this.executionTime = excursionTime;}

    public long getOffsetTime() {return offsetTime;}
    public void setOffsetTime(long offsetTime) {this.offsetTime = offsetTime;}

    public long getEstimateCompleteTime() {
        return EstimateCompleteTime;
    }

    public void setEstimateCompleteTime(long estimateCompleteTime) {
        EstimateCompleteTime = estimateCompleteTime;
    }

    @Override
    public int compareTo(APP o) {
        return Long.compare(o.getDeadline(), this.getDeadline());
    }
}


