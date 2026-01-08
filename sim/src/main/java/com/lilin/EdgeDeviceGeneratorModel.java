package com.lilin;

import java.util.*;

public class EdgeDeviceGeneratorModel {
    private final List<EdgeDevice> edge_devices;
    private final Map<Integer, EdgeDevice> NativeDevicesMap;
    private final Random rand = new Random();
    private final Map<Integer, List<EdgeDevice>> edgeDeviceCluster;

    public EdgeDeviceGeneratorModel() {
        this.edge_devices = new ArrayList<>();
        this.NativeDevicesMap = new HashMap<>();
        this.edgeDeviceCluster = new HashMap<>();
    }

    public void initialize(){
        double [][] EdgeDevicesLookUpTable = SimSettings.edgeDeviceLookUpTable;
        for(int i=0; i < EdgeDevicesLookUpTable.length; i++){
            if(i == 0)
                edge_devices.add(new Cloud((int)EdgeDevicesLookUpTable[i][0], EdgeDevicesLookUpTable[i][1], EdgeDevicesLookUpTable[i][2],
                        EdgeDevicesLookUpTable[i][3], (int) EdgeDevicesLookUpTable[i][4], EdgeDevicesLookUpTable[i][5], EdgeDevicesLookUpTable[i][6]));
            else{
                edge_devices.add(new EdgeDevice((int)EdgeDevicesLookUpTable[i][0], EdgeDevicesLookUpTable[i][1], EdgeDevicesLookUpTable[i][2],
                        EdgeDevicesLookUpTable[i][3], (int) EdgeDevicesLookUpTable[i][4], EdgeDevicesLookUpTable[i][5], EdgeDevicesLookUpTable[i][6]));
            }
        }

        int attractiveness_num = SimSettings.Attractiveness_NUM;
        for(int i=0; i<attractiveness_num; i++){
            List<EdgeDevice> Edges = new ArrayList<>();
            for(EdgeDevice edge : edge_devices){
                if(edge.getAttractiveness() == i)
                    Edges.add(edge);
            }
            edgeDeviceCluster.put(i, Edges);
        }

        for(int i=1; i<attractiveness_num; i++){
            int ran = rand.nextInt(edgeDeviceCluster.get(i).size());
            EdgeDevice edge =  edgeDeviceCluster.get(i).get(ran);
            NativeDevicesMap.put(i, edge);
        }
    }

    public Map<Integer, EdgeDevice> getNativeDevicesMap() {
        return NativeDevicesMap;
    }
    public Map<Integer, List<EdgeDevice>> getEdgeDeviceCluster() {return edgeDeviceCluster;}
    public List<EdgeDevice> getEdgeDevices() {return edge_devices;}
}
