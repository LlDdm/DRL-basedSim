package com.lilin;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class Scheduler {
    private PriorityQueue<APP> apps;
    private final Lock lock;

    public Scheduler() {
        this.apps = new PriorityQueue<>(new AppComparator());
        this.lock = new ReentrantLock();
    }

    public void addApp(APP app) {
        lock.lock();
        try {
            apps.add(app);  // 添加元素
        } finally {
            lock.unlock();  // 释放锁
        }
    }

    public APP getApp() {
        lock.lock();
        try {
            return apps.poll();  // 添加元素
        } finally {
            lock.unlock();  // 释放锁
        }
    }

    // 获取队列的大小
    public int size() {
        lock.lock();  // 获取锁
        try {
            return apps.size();  // 获取大小
        } finally {
            lock.unlock();  // 释放锁
        }
    }

    // 判断队列是否为空
    public boolean isEmpty() {
        lock.lock();  // 获取锁
        try {
            return apps.isEmpty();  // 检查是否为空
        } finally {
            lock.unlock();  // 释放锁
        }
    }

    // 将app调度
    public double[] scheduleApp(APP app, int[] actions,MobileDevice mobile) {
        Map<Integer,List<Task>> map;
        Map<Integer,Long> estDevAvTim = new HashMap<>();
        NetWork netWork = SimManager.getInstance().getNetworkModel();
        List<EdgeDevice> devices = SimManager.getInstance().getEdgeDeviceGeneratorModel().getEdgeDevices();
        List<Task> top = app.getDag().getTpoSort();
        int[] newActions = Arrays.copyOf(actions, actions.length);
        double[] diff = new double[1 + 1];//返回action变得前后app的完成时间差异和变得前后action的欧氏距离
        List<Task> taskSortR = app.getDag().getTasks();
        taskSortR.sort(Comparator.comparing(Task::getR).reversed());

        //获取当前map
        map = getScheduleMap(app, actions);

        //保存此刻各设备的可用时间
        for (int i=0;i<devices.size()+1;i++) {
            if(i!=devices.size()) {
                estDevAvTim.put(i,devices.get(i).getQueueTask_EstimateMaxComplete());
            }else
                estDevAvTim.put(i,mobile.getQueueTask_EstimateMaxComplete());
        }

        //设置当前action下每个任务的估计完成时间和应用的估计完成时间
        setTaskEstComplete(map, app,actions,devices,mobile,netWork,estDevAvTim,0,taskSortR);

        //调度任务
        for(int k : map.keySet()){
            if(k!= devices.size()) {
                for (Task task : map.get(k)) {
                    devices.get(k).addTask(task);
                    task.allocate_semaphore.release();
                    devices.get(k).setQueueTask_EstimateMaxComplete(task.getEstimate_complete_time());
                }
            }else {
                for (Task task : map.get(k)) {
                    mobile.addTask(task);
                    task.allocate_semaphore.release();
                    mobile.setQueueTask_EstimateMaxComplete(task.getEstimate_complete_time());
                }
            }
        }

        //获取此次action的关键路径
        List<Task> criticalPath = computeCriticalPath(app, top, devices, mobile, netWork);
        criticalPath.sort(Comparator.comparing(Task::getR).reversed());

        //获得优化action后的惩罚
        for(Task task : criticalPath){
            Task pTask = null;
            long inDelay;
            long inArr;
            long devAva;
            long comTim;
            long newCompleteTim;
            int exchangeFlag=0;//是否需要变更action的标志

            for(int i=0;i<devices.size()+1;i++) {
                if (task.getDevice_Id() != i) {//与action选中设备不同的设备
                    if(map.containsKey(i)) {
                        for (Task t : map.get(i)) {
                            if (t.getR() > task.getR()) {
                                pTask = t;
                            } else
                                break;
                        }
                    }

                    if (i == devices.size()) {//当前位置在mobile
                        //变更位置后的计算时间
                        comTim = (long) (task.getSize() / mobile.getMips());

                        //计算更变位到当前置后的最大传输数据到达时间
                        if(task.getPredecessors().isEmpty()){
                            inDelay = 0;
                            inArr = app.getStartTime() + inDelay;
                        }else {
                            long MaxIn = Long.MIN_VALUE;
                            for(Task pre : task.getPredecessors()) {
                                if(criticalPath.contains(pre)){
                                    if(pre.getDevice_Id() == devices.size()+1)
                                        inDelay = 0;
                                    else
                                        inDelay = netWork.calculate_EtoM_Delay(task.getPredecessorsMap().get(pre),devices.get(pre.getDevice_Id()).getDeviceId());
                                    inArr = pre.getEstimate_complete_time() + inDelay;
                                    if (inArr > MaxIn)
                                        MaxIn = inArr;
                                }
                            }
                            inArr = MaxIn;
                        }

                        //计算更变到当前位置后的服务器可用时间
                        if(pTask==null)
                            devAva = estDevAvTim.get(i);
                        else
                            devAva = pTask.getEstimate_complete_time();
                    }else {//当前位置为边缘或云
                        //变更位置后的计算时间
                        comTim = (long) (task.getSize() / devices.get(i).getMips());

                        //计算更变位到当前置后的最大传输数据到达时间
                        if(task.getPredecessors().isEmpty()){
                            inDelay = netWork.calculate_MtoE_Delay(app.getInputsize(), devices.get(i).getDeviceId());
                            inArr = app.getStartTime() + inDelay;
                        }else {
                            long MaxIn = Long.MIN_VALUE;
                            for(Task pre : task.getPredecessors()) {
                                if(criticalPath.contains(pre)){
                                    if(pre.getDevice_Id() == devices.size()+1)
                                        inDelay = netWork.calculate_MtoE_Delay(pre.getPredecessorsMap().get(pre), devices.get(i).getDeviceId());
                                    else
                                        inDelay = netWork.calculate_EtoE_Delay(task.getPredecessorsMap().get(pre),devices.get(pre.getDevice_Id()).getDeviceId(),mobile.getDeviceId());
                                    inArr = pre.getEstimate_complete_time() + inDelay;
                                    if (inArr > MaxIn)
                                        MaxIn = inArr;
                                }
                            }
                            inArr = MaxIn;
                        }

                        //计算更变到当前位置后的服务器可用时间
                        if(pTask==null)
                            devAva = estDevAvTim.get(i);
                        else
                            devAva = pTask.getEstimate_complete_time();
                    }

                    //计算变更后任务的完成时间
                    newCompleteTim = Math.max(devAva,inArr) + comTim;

                    //计算更新位置后app的估计完成时间
                    if(task.getSuccessors().isEmpty()){
                        long outDelay;
                        if(i == devices.size())
                            outDelay = 0;
                        else
                            outDelay = netWork.calculate_EtoM_Delay(app.getOutputsize(),devices.get(i).getDeviceId());
                        if(newCompleteTim + outDelay < app.getEstimateCompleteTime()) {
                            diff[1] = app.getEstimateCompleteTime() - newCompleteTim + outDelay;
                            app.setEstimateCompleteTime(newCompleteTim + outDelay);
                            exchangeFlag = 1;
                        }
                    }else {
                        if(newCompleteTim<task.getEstimate_complete_time())
                           exchangeFlag=1;
                    }

                    //如果变更后能更快完成就更新action,目前先试一试只增加惩罚，不真正变迁任务
                    if(exchangeFlag == 1) {
                        newActions[task.get_taskId()] = i;
                        map = getScheduleMap(app,newActions);
                        setTaskEstComplete(map, app, newActions,devices,mobile,netWork,estDevAvTim,task.get_taskId(),taskSortR);
                    }
                }
            }
        }

        //获取新旧action的欧式距离
        diff[0] = comEuDis(actions,newActions);
        return diff;
    }

    //根据action获取调度map
    public Map<Integer,List<Task>> getScheduleMap(APP app, int[] actions) {
        Map<Integer,List<Task>> map = new HashMap<>();
        for(Task task : app.getDag().getTasks()){
            //将任务放入对应action的优先级队列中等待调度
            int id = actions[task.get_taskId()];
            task.setDevice_Id(id);
            map.computeIfAbsent(id, k -> new ArrayList<>()).add(task);
        }

        //按照任务的优先级对相应的调度队列排序
        for (int k : map.keySet()) {
            if(!map.get(k).isEmpty())
                map.get(k).sort(Comparator.comparing(Task::getR).reversed());
        }
        return map;
    }

    //根据action计算每个任务的估计完成时间
    public void setTaskEstComplete(Map<Integer,List<Task>> map, APP app, int[] actions, List<EdgeDevice> devices,
                                   MobileDevice mobile, NetWork netWork, Map<Integer,Long> estDevAvTim,int curTaskId, List<Task> taskSortR) {

        //获取每个任务的估计开始时间
        for(int i=curTaskId;i<taskSortR.size();i++) {
            Task task = taskSortR.get(i);
            int deviceId = task.getDevice_Id();
            long deviceAva = estDevAvTim.get(deviceId);
            int index = map.get(deviceId).indexOf(task);
            long inArr;
            long inDelay;
            long comDelay;

            //获取任务开始时间
            if (deviceId == devices.size()) {//如果任务在mobile上执行
                comDelay = (long) (task.getSize() / mobile.getMips());
                if (task.getPredecessors().isEmpty())
                    inArr = app.getStartTime();
                else {
                    //获取最大前驱数据传输延迟
                    long MaxIn = Long.MIN_VALUE;
                    for (Task pre : task.getPredecessors()) {
                        int preDeviceId = actions[pre.get_taskId()];
                        if (preDeviceId == deviceId) {
                            inDelay = 0;
                        } else {
                            inDelay = netWork.calculate_EtoM_Delay(task.getPredecessorsMap().get(pre), devices.get(preDeviceId).getDeviceId());
                        }
                        inArr = pre.getEstimate_complete_time() + inDelay;
                        ;
                        if (inArr > MaxIn)
                            MaxIn = inArr;
                    }
                    inArr = MaxIn;
                }
            } else {//如果任务在边缘或云服务器上执行
                comDelay = (long) (task.getSize() / devices.get(deviceId).getMips());
                if (task.getPredecessors().isEmpty()) {
                    inArr = app.getStartTime() + netWork.calculate_MtoE_Delay(app.getInputsize(), devices.get(deviceId).getDeviceId());
                } else {
                    long MaxIn = Long.MIN_VALUE;
                    for (Task pre : task.getPredecessors()) {
                        int preDeviceId = actions[pre.get_taskId()];
                        if (preDeviceId == deviceId) {
                            inDelay = 0;
                        } else if (preDeviceId == devices.size()) {
                            inDelay = netWork.calculate_MtoE_Delay(task.getPredecessorsMap().get(pre), devices.get(deviceId).getDeviceId());
                        } else {
                            inDelay = netWork.calculate_EtoE_Delay(task.getPredecessorsMap().get(pre), devices.get(preDeviceId).getDeviceId(), devices.get(deviceId).getDeviceId());
                        }
                        inArr = pre.getEstimate_complete_time() + inDelay;
                        if (inArr > MaxIn)
                            MaxIn = inArr;
                    }
                    inArr = MaxIn;
                }
            }
            //计算所在服务器的可以时间
            for (int j = 0; j < index; j++) {
                deviceAva += map.get(deviceId).get(j).getEstimate_complete_time();
            }

            task.setEstimate_complete_time(Math.max(deviceAva, inArr) + comDelay);

            //计算app的估计完成时间
            if (task.getSuccessors().isEmpty()) {
                long outDelay;
                if (deviceId == devices.size())
                    outDelay = 0;
                else
                    outDelay = netWork.calculate_EtoM_Delay(app.getOutputsize(), devices.get(deviceId).getDeviceId());
                if (task.getEstimate_complete_time() + outDelay > app.getEstimateCompleteTime())
                    app.setEstimateCompleteTime(task.getEstimate_complete_time() + outDelay);
            }
        }
    }

    public List<Task> computeCriticalPath(APP app, List<Task> tpoSort, List<EdgeDevice> devices, MobileDevice mobile, NetWork netWork) {
        Map<Task, Long> earliestStart = new HashMap<>();
        Map<Task, Long> latestStart = new HashMap<>();
        List<Task> criticalTasks = new ArrayList<>();

        // Step 1: 计算每个任务的最早开始时间
        for (Task task : tpoSort) {
            int deviceId = task.getDevice_Id();

            if(deviceId == devices.size()){
                task.setComDelay((long) (task.getSize()/mobile.getMips()));
                if(task.getPredecessors().isEmpty()){
                    earliestStart.put(task, 0L);
                }else {
                    long MAX_start = Long.MIN_VALUE;
                    int preDeviceId;
                    long inDelay;
                    for (Task pre : task.getPredecessors()) {
                        preDeviceId = pre.get_taskId();
                        if(preDeviceId == deviceId){
                            inDelay = 0L;
                            task.setInDelay(pre,0L);
                            pre.setOutDelay(task,0L);
                        }else {
                            inDelay = netWork.calculate_EtoM_Delay(task.getPredecessorsMap().get(pre), devices.get(preDeviceId).getDeviceId());
                            task.setInDelay(pre,inDelay);
                            pre.setOutDelay(task,inDelay);
                        }
                        if(earliestStart.get(pre) + pre.getComDelay() + inDelay > MAX_start)
                            MAX_start = earliestStart.get(pre) + pre.getComDelay() + inDelay;
                    }
                    earliestStart.put(task, MAX_start);
                }
            }else {
                task.setComDelay((long) (task.getSize()/devices.get(deviceId).getMips()));
                if(task.getPredecessors().isEmpty())
                    earliestStart.put(task, 0L);
                else {
                    long MAX_start = Long.MIN_VALUE;
                    int preDeviceId;
                    long inDelay;
                    for (Task pre : task.getPredecessors()) {
                        preDeviceId = pre.get_taskId();
                        if(preDeviceId == deviceId){
                            inDelay = 0L;
                            task.setInDelay(pre,0L);
                            pre.setOutDelay(task,0L);
                        }else if(preDeviceId == devices.size()){
                            inDelay = netWork.calculate_MtoE_Delay(task.getPredecessorsMap().get(pre), devices.get(preDeviceId).getDeviceId());
                            task.setInDelay(pre,inDelay);
                            pre.setOutDelay(task,inDelay);
                        }else {
                            inDelay = netWork.calculate_EtoE_Delay(task.getPredecessorsMap().get(pre), devices.get(preDeviceId).getDeviceId(), devices.get(deviceId).getDeviceId());
                            task.setInDelay(pre,inDelay);
                            pre.setOutDelay(task,inDelay);
                        }
                        if(earliestStart.get(pre) + pre.getComDelay() + inDelay > MAX_start)
                            MAX_start = earliestStart.get(pre) + pre.getComDelay() + inDelay;
                    }
                    earliestStart.put(task, MAX_start);
                }
            }
        }

        List<Task> revisedTasks = new ArrayList<>(tpoSort);
        Collections.reverse(revisedTasks);

        // Step 2: 计算每个任务的最迟开始时间
        for (Task task : revisedTasks) {
            int deviceId = task.getDevice_Id();
            if (task.getSuccessors().isEmpty()) {
                latestStart.put(task, earliestStart.get(task));
            } else {
                long Min_Start = Long.MAX_VALUE;
                for (Task suc : task.getSuccessors()) {
                    if (latestStart.get(suc) - task.getOutDelay().get(suc) - task.getComDelay() < Min_Start) {
                        Min_Start = latestStart.get(suc) - task.getOutDelay().get(suc) - task.getComDelay();
                    }
                }
                latestStart.put(task, Min_Start);
            }
        }

        // Step 3: 确定关键任务
        for (Task task : tpoSort) {
            if (earliestStart.get(task) != null && latestStart.get(task) != null &&
                    Objects.equals(earliestStart.get(task), latestStart.get(task))) {
                criticalTasks.add(task);
            }
        }
        return criticalTasks;
    }

    //计算新旧action向量之间的欧式距离
    public double comEuDis(int[] oldAct, int[] newAct ) {
        double dis=0;
        for(int i=0;i<oldAct.length;i++) {
            dis += Math.pow(oldAct[i]-newAct[i],2);
        }
        return Math.sqrt(dis);
    }

}

