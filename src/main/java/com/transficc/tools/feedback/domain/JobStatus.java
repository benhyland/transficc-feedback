package com.transficc.tools.feedback.domain;

public enum JobStatus
{
    //This order is important (Enum.compareTo is used in JobRepository)
    ERROR(1),
    BUILDING(2),
    DISABLED(3),
    SUCCESS(3);

    private final int priority;

    JobStatus(final int priority)
    {
        this.priority = priority;
    }

    public int getPriority()
    {
        return priority;
    }


}
