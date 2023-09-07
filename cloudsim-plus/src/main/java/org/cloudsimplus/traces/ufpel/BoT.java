
package org.cloudsimplus.traces.ufpel;


public final class BoT {
    private String userId;
    private String jobId;
    private Long  numberOfTasks;
    private Double taskLength;
    private Double taskTime;
    private Double taskDiskUsage;
    private Double taskRamUsage;

    public BoT() {
    }

    public BoT(String userId, String jobId, Long numberOfTasks, Double taskLength, Double taskTime, Double taskDiskUsage, Double taskRamUsage, Double averageTaskCpu, Double taskCpuCores, Long schedulingClass, Long jobCreationTime, Long jobStartTime, Long jobEndTime, Long executionAttempts) {
        this.userId = userId;
        this.jobId = jobId;
        this.numberOfTasks = numberOfTasks;
        this.taskLength = taskLength;
        this.taskTime = taskTime;
        this.taskDiskUsage = taskDiskUsage;
        this.taskRamUsage = taskRamUsage;
        this.averageTaskCpu = averageTaskCpu;
        this.taskCpuCores = taskCpuCores;
        this.schedulingClass = schedulingClass;
        this.jobCreationTime = jobCreationTime;
        this.jobStartTime= jobStartTime;
        this.jobEndTime = jobEndTime;
        this.executionAttempts = executionAttempts;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public Long getNumberOfTasks() {
        return numberOfTasks;
    }

    public void setNumberOfTasks(Long numberOfTasks) {
        this.numberOfTasks = numberOfTasks;
    }

    public Double getTaskLength() {
        return taskLength;
    }

    public void setTaskLength(Double taskLength) {
        this.taskLength = taskLength;
    }

    public Double getTaskTime() {
        return taskTime;
    }

    public void setTaskTime(Double taskTime) {
        this.taskTime = taskTime;
    }

    public Double getTaskDiskUsage() {
        return taskDiskUsage;
    }

    public void setTaskDiskUsage(Double taskDiskUsage) {
        this.taskDiskUsage = taskDiskUsage;
    }

    public Double getTaskRamUsage() {
        return taskRamUsage;
    }

    public void setTaskRamUsage(Double taskRamUsage) {
        this.taskRamUsage = taskRamUsage;
    }

    public Double getTaskCpuCores() {
        return taskCpuCores;
    }

    public void setTaskCpuCores(Double taskCpuCores) {
        this.taskCpuCores = taskCpuCores;
    }

    public Long getSchedulingClass() {
        return schedulingClass;
    }

    public void setSchedulingClass(Long schedulingClass) {
        this.schedulingClass = schedulingClass;
    }

    public Long getJobCreationTime() {
        return jobCreationTime;
    }

    public void setJobCreationTime(Long jobCreationTime) {
        this.jobCreationTime = jobCreationTime;
    }

    public Long getJobEndTime() {
        return jobEndTime;
    }

    public void setJobEndTime(Long jobEndTime) {
        this.jobEndTime = jobEndTime;
    }

    public Long getExecutionAttempts() {
        return executionAttempts;
    }

    public void setExecutionAttempts(Long executionAttempts) {
        this.executionAttempts = executionAttempts;
    }

    public Long getEvictionAmounts() {
        return evictionAmounts;
    }

    public void setEvictionAmounts(Long evictionAmounts) {
        this.evictionAmounts = evictionAmounts;
    }

    private Double taskCpuCores;
    private Long schedulingClass;
    private Long jobCreationTime;
    private Long jobEndTime;

    private Long jobStartTime;

    public Long getJobStartTime() {
        return jobStartTime;
    }

    public void setJobStartTime(Long jobStartTime) {
        this.jobStartTime = jobStartTime;
    }

    private Long executionAttempts;
    private Long evictionAmounts;

    public Double getAverageTaskCpu() {
        return averageTaskCpu;
    }

    public void setAverageTaskCpu(Double averageTaskCpu) {
        this.averageTaskCpu = averageTaskCpu;
    }

    private Double averageTaskCpu;

    public long actualCpuCores(final long maxCpuCores){
        return (long)(taskCpuCores * maxCpuCores);
    }
}
