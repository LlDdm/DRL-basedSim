package com.lilin;

import java.util.*;
import java.util.concurrent.CountDownLatch;

public class DAG {
    private List<Task> tasks;
    private HashSet<Task> criticalTasks;
    List<Task> tpoSort;
    private int[] matrix;
    private Task startTask;
    private Task endTask;
    public CountDownLatch wait_pre;

    DAG(List<Task> tasks, int[] matrix, Task startTask, Task endTask) {
        this.tasks = tasks;
        this.criticalTasks = new HashSet<>();
        this.matrix = matrix;
        setTaskPri();
        computeTpoSort();
        this.startTask = startTask;
        this.endTask = endTask;
    }

    void addTask(Task task) {
        tasks.add(task);
    }

    public Task getEndTask() {
        return endTask;
    }

    public Task getStartTask() {
        return startTask;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void setMatrix(int[] matrix) {
        this.matrix = matrix;
    }
    public int[] getMatrix() {
        return matrix;
    }

    public HashSet<Task> getCriticalTasks() { return criticalTasks; }

    public List<Task> getTpoSort() { return tpoSort; }

    private void computeTpoSort() {
        List<Task> tpoSort = new ArrayList<>();
        Queue<Task> taskQueue = new LinkedList<>();
        Map<Task, Integer> inDegree = new HashMap<>();

        // 拓扑排序
        for (Task task : tasks) {
            if (task.getPredecessors().isEmpty()) {
                taskQueue.add(task);
                inDegree.put(task, 0);
            } else {
                inDegree.put(task, task.getPredecessors().size());
            }
        }

        while (!taskQueue.isEmpty()) {
            Task task = taskQueue.poll();
            tpoSort.add(task);  // 保证任务按拓扑顺序处理
            for (Task suc : task.getSuccessors()) {
                if(suc != null) {
                    int in = inDegree.get(suc);
                    inDegree.replace(suc, in - 1);
                    if (inDegree.get(suc) == 0) {
                        taskQueue.add(suc);
                    }
                }
            }

        }

        this.tpoSort = tpoSort;
    }

    //计算任务优先级
    private void setTaskPri() {
        List<Task> Tasks = new ArrayList<>(tasks);
        Collections.reverse(Tasks);

        for (Task task : Tasks) {
            if (task.getSuccessors() == null || task.getSuccessors().isEmpty()) {
                task.setR(task.getSize());
            }else {
                List<Task> successors = task.getSuccessors();
                double R = Long.MIN_VALUE;
                for(Task successor : successors) {
                    if(successor.getR() + task.getSuccessorsMap().get(successor) > R) {
                        R = successor.getR() + task.getSuccessorsMap().get(successor);
                    }
                }
                task.setR(R+task.getSize());
            }
        }
    }


}
