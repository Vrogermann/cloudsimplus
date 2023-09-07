package org.cloudbus.cloudsim.util;

public enum BoTFileColumnEnum {
    USER("user"),
    JOB_ID("jobId"),
    TASK_AMOUNT("taskAmount"),
    TASK_LENGTH("taskLength"),
    TASK_TIME("taskTime"),
    TASK_DISK_USAGE("taskDiskUsage"),
    TASK_RAM("taskRam"),
    AVERAGE_TASK_CPU("averageTaskCpu"),
    AVERAGE_TASK_LENGTH("averageTaskLength"),
    TASK_CORES("taskCores"),
    SCHEDULING_CLASS("schedulingClass"),
    JOB_CREATION_TIME("jobCreationTime"),
    JOB_START_TIME("jobStartTime"),
    JOB_END_TIME("jobEndTime"),
    EXECUTION_ATTEMPTS("executionAttempts"),
    EVICTION_AMOUNTS("evictionAmounts"),
    UNKNOWN_COLUMN("unknown");

    private final String columnName;

    BoTFileColumnEnum(String columnName) {
        this.columnName = columnName;
    }

    public String getColumnName() {
        return columnName;
    }

    public static BoTFileColumnEnum getByColumnName(String columnName) {
        for (BoTFileColumnEnum column : BoTFileColumnEnum.values()) {
            if (column.getColumnName().equals(columnName)) {
                return column;
            }
        }
        return UNKNOWN_COLUMN;
    }
}
