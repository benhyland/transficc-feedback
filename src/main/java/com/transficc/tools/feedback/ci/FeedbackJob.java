package com.transficc.tools.feedback.ci;

import com.transficc.tools.feedback.domain.Job;
import com.transficc.tools.feedback.domain.JobStatus;
import com.transficc.tools.feedback.domain.LatestBuildInformation;
import com.transficc.tools.feedback.web.messaging.PublishableJob;

public class FeedbackJob
{
    private final int priority;
    private final boolean shouldPersistTestResults;
    private final Job job;

    public FeedbackJob(final int priority, final boolean shouldPersistTestResults, final Job job)
    {
        this.priority = priority;
        this.shouldPersistTestResults = shouldPersistTestResults;
        this.job = job;
    }

    public String getName()
    {
        return job.getName();
    }

    public JobStatus getJobStatus()
    {
        return job.getJobStatus();
    }

    public PublishableJob createPublishable()
    {
        return job.createPublishable(priority);
    }

    public boolean wasUpdated(final LatestBuildInformation latestBuildInformation)
    {
        return job.wasUpdated(latestBuildInformation);
    }

    public boolean hasJustCompleted()
    {
        return job.hasJustCompleted();
    }

    public boolean shouldPersistTestResults()
    {
        return shouldPersistTestResults;
    }
}
