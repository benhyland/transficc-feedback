package com.transficc.tools.feedback.ci;

import com.transficc.tools.feedback.domain.Job;
import com.transficc.tools.feedback.domain.JobStatus;
import com.transficc.tools.feedback.domain.LatestBuildInformation;
import com.transficc.tools.feedback.domain.VersionControl;
import com.transficc.tools.feedback.web.messaging.PublishableJob;

import org.junit.Assert;
import org.junit.Test;

public class FeedbackJobTest
{
    @Test
    public void shouldIncludeCommentsIfMasterBuild()
    {
        //Given
        final FeedbackJob job = new FeedbackJob(true, 1, new Job("tom", "tom.com", JobStatus.DISABLED, VersionControl.GIT));
        final String[] comments = {"This is a comment"};
        job.wasUpdated(new LatestBuildInformation("1234", JobStatus.SUCCESS, 3, 42342L, 100, comments, true, null, 23));

        //When
        final PublishableJob publishable = job.createPublishable();

        //Then
        Assert.assertArrayEquals(comments, publishable.getComments());
    }

    @Test
    public void shouldNotIncludeCommentsIfNotMasterBuild()
    {
        //Given
        final FeedbackJob job = new FeedbackJob(false, 1, new Job("tom", "tom.com", JobStatus.DISABLED, VersionControl.GIT));
        final String[] comments = {"This is a comment"};
        job.wasUpdated(new LatestBuildInformation("1234", JobStatus.SUCCESS, 3, 42342L, 100, comments, true, null, 23));

        //When
        final PublishableJob publishable = job.createPublishable();

        //Then
        Assert.assertArrayEquals(new String[0], publishable.getComments());
    }
}