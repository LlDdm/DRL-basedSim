/*
 * Title:        EdgeCloudSim - Simulation Settings class
 *
 * Description:
 * SimSettings provides system wide simulation settings. It is a
 * singleton class and provides all necessary information to other modules.
 * If you need to use another simulation setting variable in your
 * config file, add related getter method in this class.
 *
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package com.lilin;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public abstract class SimSettings {
    public static double mobile_pow = 500;

    public static final double CLIENT_ACTIVITY_START_TIME = 5;

    public static int Attractiveness_NUM;

    public static double In_ratio = 2;
    public static double Out_ratio = 0.5;

    public static long LOAD_GENERATE_TIME;
    public static long WARM_UP_PERIOD; //minutes unit in properties file

    public static double BANDWITH_WLAN; //Mbps unit in properties file
    public static double BANDWITH_MAN; //Mbps unit in properties file
    public static double BANDWITH_WAN; //Mbps unit in properties file
    public static double BANDWITH_LAN;

    public static double[] appLookUpTable = null;

    public static double[][] edgeDeviceLookUpTable = null;

    public static boolean initialize(String propertiesFile, String edgeDevicesFile, String applicationsFile){
        boolean result = false;
        InputStream input = null;
        try {
            input = new FileInputStream(propertiesFile);

            // load a properties file
            Properties prop = new Properties();
            prop.load(input);

            //SIMULATION_TIME = 60 * Long.parseLong(prop.getProperty("simulation_time")); //seconds
            LOAD_GENERATE_TIME = Long.parseLong(prop.getProperty("load_generate_time"));
            WARM_UP_PERIOD =  Long.parseLong(prop.getProperty("warm_up_period")); //seconds

            BANDWITH_WLAN =  Double.parseDouble(prop.getProperty("wlan_bandwidth"));
            BANDWITH_MAN =  Double.parseDouble(prop.getProperty("man_bandwidth", "0"));
            BANDWITH_WAN =  Double.parseDouble(prop.getProperty("wan_bandwidth", "0"));
            BANDWITH_LAN =  Double.parseDouble(prop.getProperty("lan_bandwidth", "0"));

        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
        finally {
            if (input != null) {
                try {
                    input.close();
                    result = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        parseApplications(applicationsFile);
        parseEdgeDevicesXML(edgeDevicesFile);

        return result;
    }


    /**
     * returns warm up period (in seconds unit) from properties file
     */
    public static long getWarmUpPeriod()
    {
        return WARM_UP_PERIOD;
    }

    public static long getLOAD_GENERATE_TIME(){ return LOAD_GENERATE_TIME; }

    private static void isElementPresent(Element element, String key) {
        try {
            String value = element.getElementsByTagName(key).item(0).getTextContent();
            if (value.isEmpty() || value == null){
                throw new IllegalArgumentException("Element '" + key + "' is not found in '" + element.getNodeName() +"'");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Element '" + key + "' is not found in '" + element.getNodeName() +"'");
        }
    }

    private static void parseApplications(String filePath)
    {
        // 使用Properties类来读取properties文件
        Properties properties = new Properties();

        try (FileInputStream fis = new FileInputStream(filePath)) {
            // 加载properties文件
            properties.load(fis);

            String[] mandatoryAttributes = {
                    "poisson_interval", // poisson mean (sec)
                    "active_period",        // active period (sec)
                    "avg_task_size",           // avg task length (MI)
                    "avg_task_tra_size",
                    "max_num_task",         // app的最大任务数量
            };

            // 初始化appLookUpTable，确保其大小与强制属性的数量匹配
            appLookUpTable = new double[mandatoryAttributes.length];

            // 遍历强制属性，从properties文件中获取并解析它们
            for (int m = 0; m < mandatoryAttributes.length; m++) {
                String propertyName = mandatoryAttributes[m];
                String propertyValue = properties.getProperty(propertyName);

                // 检查属性是否存在且不为空
                if (propertyValue == null || propertyValue.trim().isEmpty()) {
                    System.err.println("Error: Mandatory property '" + propertyName + "' not found or is empty in file: " + filePath);
                    // 可以选择抛出异常而不是退出，以便调用者处理
                    throw new IllegalArgumentException("Missing or empty mandatory property: " + propertyName);
                }

                try {
                    // 将属性值解析为double类型
                    appLookUpTable[m] = Double.parseDouble(propertyValue);
                } catch (NumberFormatException e) {
                    System.err.println("Error: Property '" + propertyName + "' has an invalid numeric value '" + propertyValue + "' in file: " + filePath);
                    // 可以选择抛出异常而不是退出
                    throw new IllegalArgumentException("Invalid number format for property: " + propertyName, e);
                }
            }

        } catch (IOException e) {
            // 处理文件读写错误
            System.err.println("Error reading application properties file: " + filePath + ". Terminating simulation...");
            e.printStackTrace();
            System.exit(1);
        } catch (IllegalArgumentException e) {
            // 处理配置错误（如缺少属性或格式错误）
            System.err.println("Configuration error in application properties: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            // 捕获任何其他未知错误
            System.err.println("An unexpected error occurred while parsing application properties from: " + filePath);
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void parseEdgeDevicesXML(String filePath) {
        Document doc = null;
        try {
            File devicesFile = new File(filePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(devicesFile);
            doc.getDocumentElement().normalize();

            String[] mandatoryAttributes = {
                    "id",
                    "mips",
                    "latitude",
                    "longitude",
                    "attractiveness",
                    "downloadspeed",
                    "uploadspeed"
            };

            NodeList deviceList = doc.getElementsByTagName("device");
            edgeDeviceLookUpTable = new double[deviceList.getLength()]
                    [mandatoryAttributes.length];

            Set<Double> attractivenessType = new HashSet<>();

            for (int i = 0; i < deviceList.getLength(); i++) {
                Node deviceNode = deviceList.item(i);

                Element deviceElement = (Element) deviceNode;

                for (int m = 0; m < mandatoryAttributes.length; m++) {
                    isElementPresent(deviceElement, mandatoryAttributes[m]);
                    edgeDeviceLookUpTable[i][m] = Double.parseDouble(deviceElement.
                            getElementsByTagName(mandatoryAttributes[m]).item(0).getTextContent());
                }
                if(!attractivenessType.contains(edgeDeviceLookUpTable[i][4])){
                    attractivenessType.add(edgeDeviceLookUpTable[i][4]);
                    Attractiveness_NUM++;
                }
            }
        } catch (Exception e) {
            System.out.println("Edge Devices XML cannot be parsed! Terminating simulation...");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
