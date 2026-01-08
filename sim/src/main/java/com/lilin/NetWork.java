package com.lilin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class NetWork {
    private double WAN_BW;
    private double MAN_BW;
    private double WLAN_BW;
    private double LAN_BW;
    private double NoisePow;
    private double mobileTraPower;
    private double m2eDis;
    private double PathLoss;
    private double FadingFactor;
    private double InterPow;
    private double SNWire;
    private double SNWireLess;
    private double[][] delayMatrix;

    private static final double EARTH_RADIUS_METERS = 6371000;
    private double LOG2 = Math.log(2);

    public Map<EdgeDevice,Map<EdgeDevice,Double>> BWmap = new HashMap<>();
    public Map<MobileDevice,Double> mobileBW = new HashMap<>();
    private Random rand = new Random();

    public NetWork() {
        this.MAN_BW =  SimSettings.BANDWITH_MAN;
        this.WAN_BW =  SimSettings.BANDWITH_WAN;
        this.WLAN_BW =  SimSettings.BANDWITH_WLAN;
        this.LAN_BW = SimSettings.BANDWITH_LAN;
        this.SNWireLess = 15;
        this.SNWire = 30;
    }

    public double getWAN_BW() {return WAN_BW;}
    public void setWAN_BW(double WAN_BW) {
        this.WAN_BW = WAN_BW;
    }

    public double getMAN_BW() {
        return MAN_BW ;
    }
    public void setMAN_BW(double MAN_BW) {
        this.MAN_BW = MAN_BW;
    }

    public double getLAN_BW() {return LAN_BW ;}
    public void setLAN_BW(double LAN_BW) {
        this.LAN_BW = LAN_BW;
    }

    public double getWLAN_BW() {
        return WLAN_BW ;
    }
    public void setWLAN_BW(double wlanBw) {
        this.WLAN_BW = wlanBw;
    }

    public double[][] getDelayMatrix(){
        return delayMatrix;
    }
    public void setDelayMatrix(double[][] delayMatrix){
        this.delayMatrix = delayMatrix;
    }

    public void init(List<EdgeDevice> devices, MobileDevice mobileDevice, Map<Integer,EdgeDevice> NativeDevices) {
        delayMatrix = new double[devices.size()+1][devices.size()+1];
        int nativeID = NativeDevices.get(mobileDevice.getAttractiveness()).getDeviceId();

        for(int i=0 ;i<devices.size();i++) {
            for(int j=i ;j<devices.size();j++) {
                if(i==j){
                    delayMatrix[i][j] = 0;
                }else if(i==0){
                    delayMatrix[i][j] = 3;
                    delayMatrix[j][i] = 3;
                }else {
                    delayMatrix[i][j] = 1;
                    delayMatrix[j][i] = 1;
                }
            }
        }

        double mToEDelay = 10;
        for(int i=0 ;i<devices.size();i++) {
            if(i==nativeID){
                delayMatrix[i][devices.size()] = mToEDelay;
                delayMatrix[devices.size()][i] = mToEDelay;
            }else {
                delayMatrix[i][devices.size()] = mToEDelay + delayMatrix[i][nativeID];
                delayMatrix[devices.size()][i] = mToEDelay +  delayMatrix[i][nativeID];
            }
        }

        delayMatrix[devices.size()][devices.size()] = 0;
    }


//    public double[][] getDelayMatrix(){
//        List<EdgeDevice> edgeDevices = SimManager.getInstance().getEdgeDeviceGeneratorModel().getEdgeDevices();
//        MobileDevice mobile = SimManager.getInstance().getLoadGeneratorModel().getMobileDevices().get(0);
//        double[][] DelayMatrix = new double[edgeDevices.size()+1][edgeDevices.size()+1];
//        int NativeID = SimManager.getInstance().getEdgeDeviceGeneratorModel().getNativeDevicesMap().get(mobile.getAttractiveness()).getDeviceId();
//
//        for(int i=0;i<edgeDevices.size();i++){
//            for(int j=i;j<edgeDevices.size();j++){
//                if(i==j){
//                    DelayMatrix[i][j] = 0;
//                }else if(edgeDevices.get(i).getAttractiveness()==edgeDevices.get(j).getAttractiveness()) {
//                    double delay = 1/bwToSpeed(LAN_BW,SNWire);
//                    DelayMatrix[i][j] = delay;
//                    DelayMatrix[j][i] = delay;
//                }else{
//                    double delay1 = 1/bwToSpeed(LAN_BW,SNWire);
//                    double delay2 = 1/bwToSpeed(MAN_BW,SNWire);
//                    DelayMatrix[i][j] = delay1 + delay2;
//                    DelayMatrix[j][i] =  DelayMatrix[i][j];
//                }
//            }
//        }
//        double delayM = 1/bwToSpeed(WLAN_BW,SNWireLess);
//        for(int i=0;i<edgeDevices.size();i++){
//            if(i == NativeID) {
//                DelayMatrix[i][edgeDevices.size()] = delayM;
//                DelayMatrix[edgeDevices.size()][i] = delayM;
//            }
//            else {
//                DelayMatrix[i][edgeDevices.size()] = delayM + DelayMatrix[i][NativeID];
//                DelayMatrix[edgeDevices.size()][i] = delayM + DelayMatrix[i][NativeID];
//            }
//        }
//        return DelayMatrix;
//    }

//    public double bwToSpeedForWireless(double BW, double MLa, double Mlo,double ELa, double Elo,double PathLoss,
//                                       double FadingFactor,double mobileTraPower,double InterPow){
//        double dis = calculateDistance(MLa,Mlo,ELa,Elo);
//        return BW * Math.log(1 + (mobileTraPower * Math.pow(dis,-PathLoss) * FadingFactor)/(NoisePow + InterPow))/LOG2;
//    }
//
//    //MB/MS
//    public double bwToSpeed(double bw,double SN){
//        return (bw * Math.log(1 + SN) / LOG2) / 8 ;
//    }

    public long calculate_MtoE_Delay(double dataSize,int disID){
        return (long) (dataSize * delayMatrix[delayMatrix.length-1][disID]);
    }

    public long calculate_EtoE_Delay(double dataSize,int srcID,int disID){
        return (long) (dataSize * delayMatrix[srcID][disID]);
    }

    public long calculate_EtoM_Delay(double dataSize,int srcID){
        return (long) (dataSize * delayMatrix[srcID][delayMatrix.length-1]);
    }

//    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
//        // 1. 将经纬度从度数转换为弧度
//        double lat1Rad = Math.toRadians(lat1);
//        double lon1Rad = Math.toRadians(lon1);
//        double lat2Rad = Math.toRadians(lat2);
//        double lon2Rad = Math.toRadians(lon2);
//
//        // 2. 计算经纬度差值
//        double deltaLat = lat2Rad - lat1Rad;
//        double deltaLon = lon2Rad - lon1Rad;
//
//        // 3. 应用 Haversine 公式计算 'a'
//        // a = sin²(Δφ/2) + cos φ1 ⋅ cos φ2 ⋅ sin²(Δλ/2)
//        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
//                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
//                        Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
//
//        // 4. 应用 Haversine 公式计算 'c' (中心角)
//        // c = 2 ⋅ atan2(√a, √(1−a))
//        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
//
//        // 5. 计算最终距离
//        // d = R ⋅ c
//
//        return EARTH_RADIUS_METERS * c;
//    }
}

