//package com.lilin;
//
//import com.lilin.Subtask;
//import com.lilin.Task;
//import com.google.gson.Gson;
//
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//
//public class SimulationEnvironment {
//    private long currentTimeSlot;
//    private List<EdgeServer> servers;
//    private Map<Integer, Task> runningTasks; // Tasks that have subtasks running or pending
//    private List<Task> completedTasks;
//    private Task currentScheduledTask; // The task whose subtasks PPO is currently deciding on
//
//    private int numServers;
//    private int maxSubtasksPerTask; // Max subtasks in any task
//    private double minServerCapacity;
//    private double maxServerCapacity;
//    private double[][] networkCommRates; // For state, though not used in execution logic for this problem
//
//    private Random random;
//
//    public SimulationEnvironment(int numServers, int maxSubtasksPerTask, double minServerCapacity, double maxServerCapacity) {
//        this.numServers = numServers;
//        this.maxSubtasksPerTask = maxSubtasksPerTask;
//        this.minServerCapacity = minServerCapacity;
//        this.maxServerCapacity = maxServerCapacity;
//        this.random = new Random();
//        initializeEnvironment();
//    }
//
//    public void initializeEnvironment() {
//        currentTimeSlot = 0;
//        servers = new ArrayList<>(numServers);
//        for (int i = 0; i < numServers; i++) {
//            double capacity = minServerCapacity + random.nextDouble() * (maxServerCapacity - minServerCapacity);
//            servers.add(new EdgeServer(i, capacity));
//        }
//        runningTasks = new ConcurrentHashMap<>(); // Concurrent for potential multi-threaded updates if needed
//        completedTasks = new LinkedList<>();
//        currentScheduledTask = null;
//
//        // Initialize a dummy network communication rates matrix
//        // For this problem, it's part of the state but not actively used in execution time calculations
//        // as scheduling is instantaneous and subtask execution is self-contained once scheduled.
//        // If data dependencies *between* subtasks on *different* servers were explicitly modeled, this would be critical.
//        networkCommRates = new double[numServers][numServers];
//        for (int i = 0; i < numServers; i++) {
//            for (int j = 0; j < numServers; j++) {
//                if (i == j) {
//                    networkCommRates[i][j] = 0; // No comm cost to self
//                } else {
//                    networkCommRates[i][j] = 1.0 + random.nextDouble() * 5.0; // Example latency/bandwidth
//                }
//            }
//        }
//        System.out.println("Environment initialized. Num Servers: " + numServers);
//    }
//
//    public void generateNewTask() {
//        Task newTask = new Task(currentTimeSlot, maxSubtasksPerTask);
//        runningTasks.put(newTask.getId(), newTask);
//        currentScheduledTask = newTask; // This is the task for which PPO will decide
//        System.out.println("Time: " + currentTimeSlot + ", Generated new task: " + newTask.getId() + " with " + newTask.getSubtasks().size() + " subtasks.");
//    }
//
//    /**
//     * Applies the action (server assignments for currentScheduledTask's subtasks) received from PPO.
//     */
//    public void applyAction(int[] action) {
//        if (currentScheduledTask == null) {
//            System.err.println("Error: No current task to apply action to.");
//            return;
//        }
//
//        List<Subtask> subtasks = currentScheduledTask.getSubtasks();
//        if (action.length != subtasks.size()) {
//            System.err.println("Error: Action length does not match current task's subtask count. Task: " + subtasks.size() + ", Action: " + action.length);
//            // This is a critical error, the Python side should respect the actual subtask count.
//            // For robustness, we'll try to apply what we can.
//        }
//
//        for (int i = 0; i < Math.min(action.length, subtasks.size()); i++) {
//            Subtask subtask = subtasks.get(i);
//            int serverId = action[i];
//            if (serverId >= 0 && serverId < numServers) {
//                servers.get(serverId).addSubtaskToQueue(subtask);
//                subtask.setAssignedServerId(serverId);
//            } else {
//                System.err.println("Invalid server ID in action: " + serverId + " for subtask " + subtask.getId());
//                // Handle invalid server ID, maybe assign to a default or penalize.
//            }
//        }
//        System.out.println("Task " + currentScheduledTask.getId() + " subtasks assigned to servers: " + Arrays.toString(action));
//        currentScheduledTask = null; // Task has been scheduled, waiting for completion
//    }
//
//    /**
//     * Advances the simulation by one time slot.
//     * @return List of newly completed tasks (if any).
//     */
//    public List<Task> step() {
//        currentTimeSlot++;
//        List<Task> newlyCompletedTasks = new LinkedList<>();
//
//        // Process each server for one time slot
//        List<Subtask> newlyCompletedSubtasks = new LinkedList<>();
//        for (EdgeServer server : servers) {
//            newlyCompletedSubtasks.addAll(server.process(currentTimeSlot));
//        }
//
//        // Update dependencies for other subtasks and check for task completion
//        for (Subtask completedSubtask : newlyCompletedSubtasks) {
//            // Find its parent task
//            Task parentTask = runningTasks.get(completedSubtask.getTaskId());
//            if (parentTask != null) {
//                // Mark this subtask's completion for all other subtasks that depend on it
//                for (Subtask otherSubtask : parentTask.getSubtasks()) {
//                    if (parentTask.getDependencyMatrix()[completedSubtask.getId()][otherSubtask.getId()]) {
//                        otherSubtask.setDependencyMet(completedSubtask.getId());
//                        // If it makes it ready, mark its status
//                        if (otherSubtask.getStatus() == Subtask.Status.PENDING && otherSubtask.areAllDependenciesMet(parentTask.getDependencyMatrix())) {
//                            otherSubtask.setStatus(Subtask.Status.READY);
//                        }
//                    }
//                }
//                // Check if the entire task is completed
//                if (parentTask.isAllSubtasksCompleted()) {
//                    parentTask.setCompletionTime(currentTimeSlot);
//                    completedTasks.add(parentTask);
//                    runningTasks.remove(parentTask.getId()); // Remove from running
//                    newlyCompletedTasks.add(parentTask);
//                    System.out.println("Task " + parentTask.getId() + " completed at time: " + parentTask.getCompletionTime());
//                }
//            }
//        }
//        return newlyCompletedTasks;
//    }
//
//    public double getAverageTaskCompletionTime() {
//        if (completedTasks.isEmpty()) {
//            return 0.0;
//        }
//        long totalCompletionTime = 0;
//        for (Task task : completedTasks) {
//            totalCompletionTime += (task.getCompletionTime() - task.getArrivalTime());
//        }
//        return (double) totalCompletionTime / completedTasks.size();
//    }
//
//    public Map<String, Object> getState() {
//        Map<String, Object> state = new HashMap<>();
//        state.put("current_time_slot", currentTimeSlot);
//
//        // Current task to be scheduled by PPO
//        if (currentScheduledTask != null) {
//            Map<String, Object> taskInfo = new HashMap<>();
//            taskInfo.put("id", currentScheduledTask.getId());
//            taskInfo.put("arrivalTime", currentScheduledTask.getArrivalTime());
//            List<Map<String, Object>> subtaskInfos = new ArrayList<>();
//            for (Subtask st : currentScheduledTask.getSubtasks()) {
//                Map<String, Object> stInfo = new HashMap<>();
//                stInfo.put("id", st.getId());
//                stInfo.put("computationCycles", st.getComputationCycles());
//                stInfo.put("status", st.getStatus().name());
//                subtaskInfos.add(stInfo);
//            }
//            taskInfo.put("subtasks", subtaskInfos);
//            state.put("current_task", taskInfo);
//            state.put("subtask_dependency_matrix", currentScheduledTask.getDependencyMatrix());
//        } else {
//            // If no task is currently waiting for scheduling (e.g., first step of episode, or after scheduling)
//            // We need a consistent representation for PPO. Python side should handle this padding.
//            state.put("current_task", Collections.emptyMap());
//            state.put("subtask_dependency_matrix", new boolean[0][0]);
//        }
//
//        List<Double> serverCapacities = new ArrayList<>();
//        List<Integer> serverQueueLengths = new ArrayList<>();
//        //List<Double> serverProcessingRemainingCycles = new ArrayList<>(); // Python's side can derive this if needed
//
//        for (EdgeServer server : servers) {
//            serverCapacities.add(server.getProcessingCapacity());
//            serverQueueLengths.add(server.getQueueLength() + (server.getCurrentProcessingSubtask() != null ? 1 : 0)); // Include currently processing task
//        }
//        state.put("server_capacities", serverCapacities);
//        state.put("server_queue_lengths", serverQueueLengths);
//        state.put("network_comm_rates", networkCommRates); // Assuming it's a fixed matrix or updated externally
//        state.put("avg_completion_time_all_tasks", getAverageTaskCompletionTime());
//
//        return state;
//    }
//
//    public List<Task> getCompletedTasks() {
//        return completedTasks;
//    }
//
//    public int getNumServers() {
//        return numServers;
//    }
//}
