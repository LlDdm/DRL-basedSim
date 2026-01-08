/*
 * Title:        EdgeCloudSim - Main Application
 *
 * Description:  Main application for Sample App2
 *
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 * Copyright (c) 2017, Bogazici University, Istanbul, Turkey
 */

package com.lilin;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class Main {

    /**
     * Creates main() to run this example
     */
    public static void main(String[] args) {
        List<Double> Res;

        int iterationNumber = 1;
        String configFile = "";
        //String outputFolder = "";
        String edgeDevicesFile = "";
        String applicationsFile = "";
        if (args.length == 5) {
            configFile = args[0];
            edgeDevicesFile = args[1];
            applicationsFile = args[2];
            //outputFolder = args[3];
            iterationNumber = Integer.parseInt(args[4]);
        } else {
            System.out.println("Simulation setting file, output folder and iteration number are not provided! Using default ones...");
            configFile = "D:\\First _year_postgraduate\\任务依赖\\paper_me\\小论文\\DRL\\DRL-java\\sim\\src\\main\\resources\\scenario.properties";
            applicationsFile = "D:\\First _year_postgraduate\\任务依赖\\paper_me\\小论文\\DRL\\DRL-java\\sim\\src\\main\\resources\\applications.properties";
            edgeDevicesFile = "D:\\First _year_postgraduate\\任务依赖\\paper_me\\小论文\\DRL\\DRL-java\\sim\\src\\main\\resources\\devices.XML";
            //outputFolder = "sim_results/ite" + iterationNumber;
        }

        //load settings from configuration file
        if (!SimSettings.initialize(configFile, edgeDevicesFile, applicationsFile)) {
            System.out.println("cannot initialize simulation settings!");
            System.exit(0);
        }

        DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date SimulationStartDate = Calendar.getInstance().getTime();
        String now = df.format(SimulationStartDate);
        System.out.println("Simulation started at " + now);
        System.out.println("----------------------------------------------------------------------");

        // Training/Episode Parameters (Should ideally match Python's config)
        int maxEpisodes = 1000;

        JavaClient client = new JavaClient();

        System.out.println("Starting Java client simulation...");
        client.runSimulation(maxEpisodes);
        System.out.println("Java client simulation finished.");

        Res = client.getAvgResults();

        // 创建输出excel文件
        Workbook workbook = new XSSFWorkbook();

        // 写入文件
        try (workbook; FileOutputStream fileOut = new FileOutputStream("result.xlsx")) {
            try {
                create_CCRSheet(workbook, Res);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            workbook.write(fileOut);
            System.out.println("结果已保存到：result.xlsx！");
        } catch (IOException e) {
            e.printStackTrace();
        }
        Date SimulationEndDate = Calendar.getInstance().getTime();
        now = df.format(SimulationEndDate);
        System.out.println("Simulation finished at " + now + ". It took " + SimUtils.getTimeDifference(SimulationStartDate, SimulationEndDate));

    }

    private static void create_CCRSheet(Workbook workbook, List<Double> data) {

        // 创建工作表
        Sheet sheet = workbook.createSheet("resEpisode");

        // 创建标题行
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("episode");
        headerRow.createCell(1).setCellValue("result");

        // 填充数据
        int rowNum = 1;
        for (int i = 0; i < data.size(); i++) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue((i+1)*50);
            row.createCell(1).setCellValue(data.get(i));
        }
    }

}
