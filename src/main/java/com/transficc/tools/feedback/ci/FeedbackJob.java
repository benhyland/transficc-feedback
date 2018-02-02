package com.transficc.tools.feedback.ci;

import com.transficc.tools.feedback.domain.Job;
import com.transficc.tools.feedback.domain.JobStatus;
import com.transficc.tools.feedback.domain.LatestBuildInformation;
import com.transficc.tools.feedback.web.messaging.PublishableJob;

public class FeedbackJob
{
    private final boolean shouldDisplayCommentsForJob;
    private final int priority;
    private final Job job;

    public FeedbackJob(final boolean shouldDisplayCommentsForJob, final int priority, final Job job)
    {
        this.shouldDisplayCommentsForJob = shouldDisplayCommentsForJob;
        this.priority = priority;
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
        return job.createPublishable(priority, shouldDisplayCommentsForJob);
    }

    public boolean wasUpdated(final LatestBuildInformation latestBuildInformation)
    {
        return job.wasUpdated(latestBuildInformation);
    }

}
