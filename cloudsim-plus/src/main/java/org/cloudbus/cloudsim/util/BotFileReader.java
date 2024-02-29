package org.cloudbus.cloudsim.util;

import org.cloudsimplus.traces.ufpel.BoT;

import java.io.*;
import java.util.*;

public class BotFileReader {


    private static final String delimiter = ",";
    private static final String fileLocation = "X:\\tcc\\cloudsimplus\\cloudsim-plus-examples\\src\\main\\resources\\workload\\ufpel\\sampleBoTs.csv";

    public static List<BoT> readBoTFile(String filepath, Long lineLimit) throws IOException {

        List<BoT> result = new ArrayList<>();

        File file = new File(filepath);
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);
        String line;
        Long firstNonZeroCreationTime = 0L;

        // Read the first line to get the headers and their indexes
        line = br.readLine();
        String[] headers = line.split(delimiter);
        Map<BoTFileColumnEnum, Integer> headerMap = new HashMap<>();

        // Map the headers to their indexes
        for (int i = 0; i < headers.length; i++) {
            BoTFileColumnEnum column = BoTFileColumnEnum.getByColumnName(headers[i]);
            if (column != BoTFileColumnEnum.UNKNOWN_COLUMN) {
                headerMap.put(column, i);
            }
        }

        // Read the remaining lines and construct BoT objects
        if(lineLimit == null){
            lineLimit = Long.MAX_VALUE;
        }
        while ((line = br.readLine()) != null && result.size() < lineLimit) {
            String[] lineValues = line.split(delimiter);

            // Create BoT object using header indexes
            BoT currentRow = new BoT(
                lineValues[headerMap.getOrDefault(BoTFileColumnEnum.USER, -1)],
                lineValues[headerMap.getOrDefault(BoTFileColumnEnum.JOB_ID, -1)],
                parseLongValue(lineValues[headerMap.getOrDefault(BoTFileColumnEnum.TASK_AMOUNT, -1)]),
                parseDoubleValue(lineValues[headerMap.getOrDefault(BoTFileColumnEnum.TASK_LENGTH, -1)]),
                parseDoubleValue(lineValues[headerMap.getOrDefault(BoTFileColumnEnum.TASK_TIME, -1)]) /  1_000_000,
                parseDoubleValue(lineValues[headerMap.getOrDefault(BoTFileColumnEnum.TASK_DISK_USAGE, -1)]),
                parseDoubleValue(lineValues[headerMap.getOrDefault(BoTFileColumnEnum.TASK_RAM, -1)]),
                parseDoubleValue(lineValues[headerMap.getOrDefault(BoTFileColumnEnum.AVERAGE_TASK_CPU, -1)]),
                parseDoubleValue(lineValues[headerMap.getOrDefault(BoTFileColumnEnum.TASK_CORES, -1)]),
                parseLongValue(lineValues[headerMap.getOrDefault(BoTFileColumnEnum.SCHEDULING_CLASS, -1)]),
                parseLongValue(lineValues[headerMap.getOrDefault(BoTFileColumnEnum.JOB_CREATION_TIME, -1)])/  1_000_000L,
                parseLongValue(lineValues[headerMap.getOrDefault(BoTFileColumnEnum.JOB_START_TIME, -1)]) / 1_000_000L,
                parseLongValue(lineValues[headerMap.getOrDefault(BoTFileColumnEnum.JOB_END_TIME, -1)])/  1_000_000L,
                parseLongValue(lineValues[headerMap.getOrDefault(BoTFileColumnEnum.EXECUTION_ATTEMPTS, -1)])
            );
            result.add(currentRow);



        }


        br.close();
        result.sort(Comparator.comparingLong(BoT::getJobStartTime));
        return result;
    }

    // Helper methods to parse values and handle unknown columns
    private static Long parseLongValue(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static Double parseDoubleValue(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    public static void main(String[] args) throws IOException {
        List<BoT> boTS = readBoTFile(fileLocation, null);
        System.out.println(boTS);
    }

}
