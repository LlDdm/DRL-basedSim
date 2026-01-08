//package com.lilin;
//
//import java.util.ArrayList;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//
//public class Subtask {
//    public final int id;
//    public final int parentTaskId;
//    private double processingRequired; // Total workload units
//    private double remainingWorkload;
//    private int assignedServerId = -1; // -1 means not yet assigned
//    private int startTime = -1;
//    private int completionTime = -1;
//    private boolean isCompleted = false;
//    private final Set<Integer> dependencies; // IDs of subtasks this one depends on
//    private final Set<Integer> completedDependencies; // IDs of dependencies that have been met
//
//    public Subtask(int id, int parentTaskId, double processingRequired, List<Integer> dependencies) {
//        this.id = id;
//        this.parentTaskId = parentTaskId;
//        this.processingRequired = processingRequired;
//        this.remainingWorkload = processingRequired;
//        this.dependencies = new HashSet<>(dependencies);
//        this.completedDependencies = new HashSet<>();
//    }
//
//    public void assignServer(int serverId) {
//        this.assignedServerId = serverId;
//    }
//
//    public int getAssignedServerId() {
//        return assignedServerId;
//    }
//
//    public double getProcessingRequired() {
//        return processingRequired;
//    }
//
//    public double getRemainingWorkload() {
//        return remainingWorkload;
//    }
//
//    public void deductWorkload(double amount) {
//        this.remainingWorkload -= amount;
//        if (this.remainingWorkload < 0) {
//            this.remainingWorkload = 0;
//        }
//    }
//
//    public boolean isCompleted() {
//        return isCompleted;
//    }
//
//    public void markCompleted(int time) {
//        this.isCompleted = true;
//        this.completionTime = time;
//    }
//
//    public int getCompletionTime() {
//        return completionTime;
//    }
//
//    public void setStartTime(int startTime) {
//        this.startTime = startTime;
//    }
//
//    public int getStartTime() {
//        return startTime;
//    }
//
//    public List<Integer> getDependencies() {
//        return new ArrayList<>(dependencies);
//    }
//
//    public void dependencyCompleted(int dependentSubtaskId) {
//        this.completedDependencies.add(dependentSubtaskId);
//    }
//
//    public boolean isReadyToProcess() {
//        return completedDependencies.containsAll(dependencies);
//    }
//
//    public void reset() {
//        this.remainingWorkload = processingRequired;
//        this.assignedServerId = -1;
//        this.startTime = -1;
//        this.completionTime = -1;
//        this.isCompleted = false;
//        this.completedDependencies.clear();
//    }
//}
