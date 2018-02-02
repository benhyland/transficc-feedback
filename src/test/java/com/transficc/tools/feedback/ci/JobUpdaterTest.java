package com.transficc.tools.feedback.ci;

import java.util.Collections;

import com.transficc.functionality.Result;
import com.transficc.tools.feedback.JobRepository;
import com.transficc.tools.feedback.domain.Job;
import com.transficc.tools.feedback.domain.JobStatus;
import com.transficc.tools.feedback.domain.LatestBuildInformation;
import com.transficc.tools.feedback.domain.TestResults;
import com.transficc.tools.feedback.domain.VersionControl;
import com.transficc.tools.feedback.web.messaging.MessageBus;
import com.transficc.tools.feedback.web.messaging.PublishableJob;

import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@SuppressWarnings("unchecked")
public class JobUpdaterTest
{
    private static final String JOB_URL = "tom-url";
    private static final String JOB_NAME = "Tom is the best";
    private static final JobStatus CURRENT_JOB_STATUS = JobStatus.SUCCESS;
    private final ContinuousIntegrationServer continuousIntegrationServer = Mockito.mock(ContinuousIntegrationServer.class);
    private final MessageBus messageBus = Mockito.mock(MessageBus.class);
    private final JobRepository jobRepository = new JobRepository(Collections.emptyMap());
    private final FeedbackJob feedbackJob = new FeedbackJob(false, 1, new Job(JOB_NAME, JOB_URL, CURRENT_JOB_STATUS, VersionControl.GIT));
    private final JobUpdater jobUpdater = new JobUpdater(continuousIntegrationServer, messageBus, jobRepository);

    @Test
    public void shouldPushJobUpdateToMessageBus()
    {
        //Given
        jobRepository.add(feedbackJob);
        final JobStatus expectedStatus = JobStatus.SUCCESS;
        final String expectedRevision = "5435dsd";
        final int expectedBuildNumber = 0;
        final long expectedTimestamp = 5L;
        final double expectedCompletionPercentage = 50.0;
        final String[] comments = new String[0];
        final TestResults expectedTestResults = new TestResults(1, 1, 2);
        final LatestBuildInformation buildUpdate = new LatestBuildInformation(expectedRevision, expectedStatus, expectedBuildNumber, expectedTimestamp, expectedCompletionPercentage, comments,
                                                                              false, expectedTestResults, 0);
        given(continuousIntegrationServer.getLatestBuildInformation(JOB_NAME, CURRENT_JOB_STATUS)).willReturn(Result.success(buildUpdate));

        //When
        jobUpdater.run();

        //Then
        verify(messageBus).sendUpdate(feedbackJob);
        assertJob(feedbackJob, expectedRevision, expectedStatus, expectedBuildNumber, expectedTimestamp, expectedCompletionPercentage, comments, expectedTestResults);
    }

    @Test
    public void shouldNotPublishAnUpdateIfNothingHasChanged()
    {
        //Given
        jobRepository.add(feedbackJob);
        final LatestBuildInformation buildUpdate = new LatestBuildInformation("", JobStatus.SUCCESS, 0, 10L, 0, new String[0], false, null, 0);
        given(continuousIntegrationServer.getLatestBuildInformation(JOB_NAME, CURRENT_JOB_STATUS)).willReturn(Result.success(buildUpdate));

        //When
        jobUpdater.run();

        //Then
        verifyZeroInteractions(messageBus);
        assertJob(feedbackJob, "", JobStatus.SUCCESS, 0, 0, 0, new String[0], null);
    }

    @Test
    public void shouldRemoveJobIfNotFoundOnCIServer()
    {
        //Given
        jobRepository.add(feedbackJob);
        given(continuousIntegrationServer.getLatestBuildInformation(JOB_NAME, CURRENT_JOB_STATUS)).willReturn(Result.error(404));

        //When
        jobUpdater.run();

        //Then
        verify(messageBus).jobRemoved(JOB_NAME);
        assertFalse(jobRepository.contains(JOB_NAME));
    }

    @Test
    public void shouldDoNothingIfJobDoesNotHaveABuild()
    {
        //Given
        jobRepository.add(feedbackJob);
        given(continuousIntegrationServer.getLatestBuildInformation(JOB_NAME, CURRENT_JOB_STATUS)).willReturn(Result.error(400));

        //When
        jobUpdater.run();

        //Then
        assertTrue(jobRepository.contains(JOB_NAME));
        verifyZeroInteractions(messageBus);
    }

    private void assertJob(final FeedbackJob job,
                           final String expectedRevision,
                           final JobStatus expectedStatus,
                           final int expectedBuildNumber,
                           final long expectedTimeStamp,
                           final double expectedPercentageCompleted,
                           final String[] expectedComments,
                           final TestResults expectedResults)
    {
        final PublishableJob publishable = job.createPublishable();
        final TestResults jobsTestResults = publishable.getJobsTestResults();
        assertThat(publishable.getJobStatus(), is(expectedStatus));
        assertThat(publishable.getRevision(), is(expectedRevision));
        assertThat(publishable.getBuildNumber(), is(expectedBuildNumber));
        assertThat(publishable.getComments(), is(expectedComments));
        assertThat(publishable.getJobCompletionPercentage(), is(expectedPercentageCompleted));
        assertThat(publishable.getTimestamp(), is(expectedTimeStamp));

        if (jobsTestResults == null)
        {
            assertNull(expectedResults);
        }
        else
        {
            assertThat(jobsTestResults.getFailCount(), is(expectedResults.getFailCount()));
            assertThat(jobsTestResults.getPassCount(), is(expectedResults.getPassCount()));
            assertThat(jobsTestResults.getSkipCount(), is(expectedResults.getSkipCount()));
        }
    }
}