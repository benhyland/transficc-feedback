package com.transficc.tools.feedback.domain;

public final class LatestBuildInformation
{
    private final String revision;
    private final JobStatus jobStatus;
    private final int number;
    private final double jobCompletionPercentage;
    private final String[] comments;
    private final boolean building;
    private final TestResults testResults;
    private final long duration;
    private final long timestamp;

    public LatestBuildInformation(final String revision,
                                  final JobStatus jobStatus,
                                  final int number,
                                  final long timestamp,
                                  final double jobCompletionPercentage,
                                  final String[] comments,
                                  final boolean building,
                                  final TestResults testResults,
                                  final long duration)
    {
        this.revision = revision;
        this.jobStatus = jobStatus;
        this.number = number;
        this.timestamp = timestamp;
        this.jobCompletionPercentage = jobCompletionPercentage;
        this.comments = comments;
        this.building = building;
        this.testResults = testResults;
        this.duration = duration;
    }

    public String getRevision()
    {
        return revision;
    }

    public JobStatus getJobStatus()
    {
        return jobStatus;
    }

    public int getNumber()
    {
        return number;
    }

    public double getJobCompletionPercentage()
    {
        return jobCompletionPercentage;
    }

    public String[] getComments()
    {
        return comments;
    }

    public boolean isBuilding()
    {
        return building;
    }

    public TestResults getTestResults()
    {
        return testResults;
    }

    public long getTimestamp()
    {
        return timestamp;
    }

    public long getDuration()
    {
        return duration;
    }
}
