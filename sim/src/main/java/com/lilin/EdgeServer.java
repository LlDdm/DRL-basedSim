//package com.lilin;
//
//import java.util.ArrayList;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Queue;
//
//public class EdgeServer {
//    public final int id;
//    public final double processingCapacity; // Workload units per time slot
//    private final Queue<Subtask> queue; // FIFO queue
//    private final List<Subtask> currentlyProcessing; // Tasks being processed in current slot
//    private final List<Subtask> completedThisSlot; // Tasks completed and removed in current slot
//
//    public EdgeServer(int id, double processingCapacity) {
//        this.id = id;
//        this.processingCapacity = processingCapacity;
//        this.queue = new LinkedList<>();
//        this.currentlyProcessing = new ArrayList<>();
//        this.completedThisSlot = new ArrayList<>();
//    }
//
//    public void addSubtask(Subtask subtask) {
//        this.queue.offer(subtask);
//    }
//
//    public List<Subtask> processTasks(int currentTime) {
//        double remainingCapacity = processingCapacity;
//        completedThisSlot.clear();
//
//        // Move tasks from queue to currentlyProcessing if capacity allows
//        // This is a simplification: in a real system, tasks might start mid-slot.
//        // Here, we assume tasks from the queue can start processing at the beginning of the slot.
//        List<Subtask> toRemoveFromQueue = new ArrayList<>();
//        for (Subtask st : queue) {
//            if (st.isReadyToProcess() && st.getRemainingWorkload() > 0) {
//                currentlyProcessing.add(st);
//                toRemoveFromQueue.add(st);
//            }
//        }
//        queue.removeAll(toRemoveFromQueue);
//
//
//        List<Subtask> tasksFinished = new ArrayList<>();
//        // Process tasks already in 'currentlyProcessing' and newly added ones
//        List<Subtask> nextCurrentlyProcessing = new ArrayList<>();
//        for (Subtask subtask : currentlyProcessing) {
//            if (remainingCapacity > 0) {
//                double workDone = Math.min(subtask.getRemainingWorkload(), remainingCapacity);
//                subtask.deductWorkload(workDone);
//                remainingCapacity -= workDone;
//
//                if (subtask.getRemainingWorkload() <= 0) {
//                    subtask.markCompleted(currentTime);
//                    tasksFinished.add(subtask);
//                    completedThisSlot.add(subtask);
//                } else {
//                    nextCurrentlyProcessing.add(subtask);
//                }
//            } else {
//                nextCurrentlyProcessing.add(subtask); // No capacity left for this subtask in this slot
//            }
//        }
//        currentlyProcessing.clear();
//        currentlyProcessing.addAll(nextCurrentlyProcessing); // Update currently processing list
//
//        return tasksFinished;
//    }
//
//    public int getQueueLength() {
//        // Count subtasks that are actually waiting (not yet processing)
//        return queue.size();
//    }
//
//    public List<Subtask> getCurrentlyProcessingSubtasks() {
//        return new ArrayList<>(currentlyProcessing);
//    }
//
//    public List<Subtask> getCompletedThisSlot() {
//        return new ArrayList<>(completedThisSlot);
//    }
//
//    public void reset() {
//        queue.clear();
//        currentlyProcessing.clear();
//        completedThisSlot.clear();
//    }
//}
