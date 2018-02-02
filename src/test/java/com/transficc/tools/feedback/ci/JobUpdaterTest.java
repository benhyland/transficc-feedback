package com.transficc.tools.feedback.ci;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.concurrent.CopyOnWriteArrayList;

import com.transficc.functionality.Result;
import com.transficc.tools.feedback.JobRepository;
import com.transficc.tools.feedback.ci.jenkins.JenkinsFacade;
import com.transficc.tools.feedback.dao.JobTestResultsDao;
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@SuppressWarnings("unchecked")
public class JobUpdaterTest
{
    private static final String JOB_URL = "tom-url";
    private static final String JOB_NAME = "Tom is the best";
    private static final JobStatus CURRENT_JOB_STATUS = JobStatus.SUCCESS;
    private final JenkinsFacade jenkinsFacade = Mockito.mock(JenkinsFacade.class);
    private final JobTestResultsDao jobTestResultsDao = Mockito.mock(JobTestResultsDao.class);
    private final CopyOnWriteArrayList<FeedbackJob> jobs = new CopyOnWriteArrayList<>();
    private final MessageBus messageBus = Mockito.mock(MessageBus.class);
    private final JobRepository jobRepository = Mockito.mock(JobRepository.class);
    private final FeedbackJob feedbackJob = new FeedbackJob(1, false, new Job(JOB_NAME, JOB_URL, CURRENT_JOB_STATUS, false, VersionControl.GIT));
    private final JobUpdater jobUpdater = new JobUpdater(jenkinsFacade, jobTestResultsDao, jobs, messageBus, jobRepository);

    @Test
    public void shouldPushJobUpdateToMessageBus()
    {
        //Given
        jobs.add(feedbackJob);
        final JobStatus expectedStatus = JobStatus.SUCCESS;
        final String expectedRevision = "5435dsd";
        final int expectedBuildNumber = 0;
        final long expectedTimestamp = 5L;
        final double expectedCompletionPercentage = 50.0;
        final String[] comments = new String[0];
        final TestResults expectedTestResults = new TestResults(1, 1, 2);
        final LatestBuildInformation buildUpdate = new LatestBuildInformation(expectedRevision, expectedStatus, expectedBuildNumber, expectedTimestamp, expectedCompletionPercentage, comments,
                                                                              false, expectedTestResults, 0);
        given(jenkinsFacade.getLatestBuildInformation(JOB_NAME, CURRENT_JOB_STATUS)).willReturn(Result.success(buildUpdate));

        //When
        jobUpdater.run();

        //Then
        verify(messageBus).sendUpdate(feedbackJob);
        verifyZeroInteractions(jobTestResultsDao);
        assertJob(feedbackJob, expectedRevision, expectedStatus, expectedBuildNumber, expectedTimestamp, expectedCompletionPercentage, comments, expectedTestResults);
    }

    @Test
    public void shouldNotPublishAnUpdateIfNothingHasChanged()
    {
        //Given
        jobs.add(feedbackJob);
        final LatestBuildInformation buildUpdate = new LatestBuildInformation("", JobStatus.SUCCESS, 0, 10L, 0, new String[0], false, null, 0);
        given(jenkinsFacade.getLatestBuildInformation(JOB_NAME, CURRENT_JOB_STATUS)).willReturn(Result.success(buildUpdate));

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
        jobs.add(feedbackJob);
        given(jenkinsFacade.getLatestBuildInformation(JOB_NAME, CURRENT_JOB_STATUS)).willReturn(Result.error(404));

        //When
        jobUpdater.run();

        //Then
        verify(messageBus).jobRemoved(JOB_NAME);
        verify(jobRepository).remove(JOB_NAME);
    }

    @Test
    public void shouldDoNothingIfJobDoesNotHaveABuild()
    {
        //Given
        jobs.add(feedbackJob);
        given(jenkinsFacade.getLatestBuildInformation(JOB_NAME, CURRENT_JOB_STATUS)).willReturn(Result.error(400));

        //When
        jobUpdater.run();

        //Then
        verifyZeroInteractions(jobRepository, messageBus, jobTestResultsDao);
    }

    @Test
    public void shouldPersistTestResultsIfJobJustCompletedAndHasPersistenceEnabled()
    {
        //Given
        final FeedbackJob feedbackJob = new FeedbackJob(1, true, new Job(JOB_NAME, JOB_URL, CURRENT_JOB_STATUS, false, VersionControl.GIT));
        jobs.add(feedbackJob);
        final JobStatus expectedStatus = JobStatus.SUCCESS;
        final String expectedRevision = "5435dsd";
        final int expectedBuildNumber = 0;
        final long expectedTimestamp = 5L;
        final double expectedCompletionPercentage = 100.0;
        final String[] comments = new String[0];
        final TestResults expectedTestResults = new TestResults(1, 1, 2);
        final LatestBuildInformation buildUpdate1 = new LatestBuildInformation(expectedRevision, expectedStatus, expectedBuildNumber, expectedTimestamp, expectedCompletionPercentage, comments,
                                                                               true, expectedTestResults, 0);
        final LatestBuildInformation buildUpdate2 = new LatestBuildInformation(expectedRevision, expectedStatus, expectedBuildNumber, expectedTimestamp, expectedCompletionPercentage, comments,
                                                                               false, expectedTestResults, 0);
        given(jenkinsFacade.getLatestBuildInformation(JOB_NAME, CURRENT_JOB_STATUS)).willReturn(Result.success(buildUpdate1),
                                                                                                Result.success(buildUpdate2));

        //When
        jobUpdater.run();
        jobUpdater.run();

        //Then
        verify(messageBus, times(2)).sendUpdate(feedbackJob);
        verify(jobTestResultsDao).addTestResults(JOB_NAME, expectedRevision, 4, 1, 1, ZonedDateTime.of(LocalDateTime.ofInstant(Instant.ofEpochMilli(expectedTimestamp),
                                                                                                                               ZoneOffset.UTC), ZoneOffset.UTC), 0);
        assertJob(feedbackJob, expectedRevision, expectedStatus, expectedBuildNumber, expectedTimestamp, expectedCompletionPercentage, comments, expectedTestResults);
    }

    @Test
    public void shouldNotPersistTestResultsIfJobJustCompletedAndDoesNotPersistenceEnabled()
    {
        //Given
        jobs.add(feedbackJob);
        final JobStatus expectedStatus = JobStatus.SUCCESS;
        final String expectedRevision = "5435dsd";
        final int expectedBuildNumber = 0;
        final long expectedTimestamp = 5L;
        final double expectedCompletionPercentage = 100.0;
        final String[] comments = new String[0];
        final TestResults expectedTestResults = new TestResults(1, 1, 2);
        final LatestBuildInformation buildUpdate1 = new LatestBuildInformation(expectedRevision, expectedStatus, expectedBuildNumber, expectedTimestamp, expectedCompletionPercentage, comments,
                                                                               true, expectedTestResults, 0);
        final LatestBuildInformation buildUpdate2 = new LatestBuildInformation(expectedRevision, expectedStatus, expectedBuildNumber, expectedTimestamp, expectedCompletionPercentage, comments,
                                                                               false, expectedTestResults, 0);
        given(jenkinsFacade.getLatestBuildInformation(JOB_NAME, CURRENT_JOB_STATUS)).willReturn(Result.success(buildUpdate1),
                                                                                                Result.success(buildUpdate2));

        //When
        jobUpdater.run();
        jobUpdater.run();

        //Then
        verify(messageBus, times(2)).sendUpdate(feedbackJob);
        verifyZeroInteractions(jobTestResultsDao);
        assertJob(feedbackJob, expectedRevision, expectedStatus, expectedBuildNumber, expectedTimestamp, expectedCompletionPercentage, comments, expectedTestResults);
    }

    @Test
    public void shouldNotPersistTestResultsIfJobHasNotJustCompletedAndHasPersistenceEnabled()
    {
        //Given
        final FeedbackJob feedbackJob = new FeedbackJob(1, true, new Job(JOB_NAME, JOB_URL, CURRENT_JOB_STATUS, false, VersionControl.GIT));
        jobs.add(feedbackJob);
        final JobStatus expectedStatus = JobStatus.SUCCESS;
        final String expectedRevision = "5435dsd";
        final int expectedBuildNumber = 0;
        final long expectedTimestamp = 5L;
        final double expectedCompletionPercentage = 100.0;
        final String[] comments = new String[0];
        final TestResults expectedTestResults = new TestResults(1, 1, 2);
        final LatestBuildInformation buildUpdate1 = new LatestBuildInformation(expectedRevision, expectedStatus, expectedBuildNumber, expectedTimestamp, expectedCompletionPercentage, comments,
                                                                               false, expectedTestResults, 0);
        final LatestBuildInformation buildUpdate2 = new LatestBuildInformation(expectedRevision, expectedStatus, expectedBuildNumber, expectedTimestamp, expectedCompletionPercentage, comments,
                                                                               false, expectedTestResults, 0);
        given(jenkinsFacade.getLatestBuildInformation(JOB_NAME, CURRENT_JOB_STATUS)).willReturn(Result.success(buildUpdate1),
                                                                                                Result.success(buildUpdate2));

        //When
        jobUpdater.run();
        jobUpdater.run();

        //Then
        verify(messageBus).sendUpdate(feedbackJob);
        verifyZeroInteractions(jobTestResultsDao);
        assertJob(feedbackJob, expectedRevision, expectedStatus, expectedBuildNumber, expectedTimestamp, expectedCompletionPercentage, comments, expectedTestResults);
    }

    @Test
    public void shouldNotPersistTestResultsIfJobHasNotJustCompletedAndDoesNotHavePersistenceEnabled()
    {
        //Given
        jobs.add(feedbackJob);
        final JobStatus expectedStatus = JobStatus.SUCCESS;
        final String expectedRevision = "5435dsd";
        final int expectedBuildNumber = 0;
        final long expectedTimestamp = 5L;
        final double expectedCompletionPercentage = 100.0;
        final String[] comments = new String[0];
        final TestResults expectedTestResults = new TestResults(1, 1, 2);
        final LatestBuildInformation buildUpdate1 = new LatestBuildInformation(expectedRevision, expectedStatus, expectedBuildNumber, expectedTimestamp, expectedCompletionPercentage, comments,
                                                                               false, expectedTestResults, 0);
        final LatestBuildInformation buildUpdate2 = new LatestBuildInformation(expectedRevision, expectedStatus, expectedBuildNumber, expectedTimestamp, expectedCompletionPercentage, comments,
                                                                               false, expectedTestResults, 0);
        given(jenkinsFacade.getLatestBuildInformation(JOB_NAME, CURRENT_JOB_STATUS)).willReturn(Result.success(buildUpdate1),
                                                                                                Result.success(buildUpdate2));

        //When
        jobUpdater.run();
        jobUpdater.run();

        //Then
        verify(messageBus).sendUpdate(feedbackJob);
        verifyZeroInteractions(jobTestResultsDao);
        assertJob(feedbackJob, expectedRevision, expectedStatus, expectedBuildNumber, expectedTimestamp, expectedCompletionPercentage, comments, expectedTestResults);
    }

    @Test
    public void shouldNotPersistTestResultsIfJobHasJustStartedAndHasPersistenceEnabled()
    {
        //Given
        final FeedbackJob feedbackJob = new FeedbackJob(1, false, new Job(JOB_NAME, JOB_URL, CURRENT_JOB_STATUS, false, VersionControl.GIT));
        jobs.add(feedbackJob);
        final JobStatus expectedStatus = JobStatus.SUCCESS;
        final String expectedRevision = "5435dsd";
        final int expectedBuildNumber = 0;
        final long expectedTimestamp = 5L;
        final double expectedCompletionPercentage = 100.0;
        final String[] comments = new String[0];
        final TestResults expectedTestResults = new TestResults(1, 1, 2);
        final LatestBuildInformation buildUpdate1 = new LatestBuildInformation(expectedRevision, expectedStatus, expectedBuildNumber, expectedTimestamp, expectedCompletionPercentage, comments,
                                                                               false, expectedTestResults, 0);
        final LatestBuildInformation buildUpdate2 = new LatestBuildInformation(expectedRevision, expectedStatus, expectedBuildNumber, expectedTimestamp, expectedCompletionPercentage, comments,
                                                                               true, expectedTestResults, 0);
        given(jenkinsFacade.getLatestBuildInformation(JOB_NAME, CURRENT_JOB_STATUS)).willReturn(Result.success(buildUpdate1),
                                                                                                Result.success(buildUpdate2));

        //When
        jobUpdater.run();
        jobUpdater.run();

        //Then
        verify(messageBus, times(2)).sendUpdate(feedbackJob);
        verifyZeroInteractions(jobTestResultsDao);
        assertJob(feedbackJob, expectedRevision, expectedStatus, expectedBuildNumber, expectedTimestamp, expectedCompletionPercentage, comments, expectedTestResults);
    }

    @Test
    public void shouldNotPersistTestResultsIfJobHasJustStartedAndDoesNotHavePersistenceEnabled()
    {
        //Given
        jobs.add(feedbackJob);
        final JobStatus expectedStatus = JobStatus.SUCCESS;
        final String expectedRevision = "5435dsd";
        final int expectedBuildNumber = 0;
        final long expectedTimestamp = 5L;
        final double expectedCompletionPercentage = 100.0;
        final String[] comments = new String[0];
        final TestResults expectedTestResults = new TestResults(1, 1, 2);
        final LatestBuildInformation buildUpdate1 = new LatestBuildInformation(expectedRevision, expectedStatus, expectedBuildNumber, expectedTimestamp, expectedCompletionPercentage, comments,
                                                                               false, expectedTestResults, 0);
        final LatestBuildInformation buildUpdate2 = new LatestBuildInformation(expectedRevision, expectedStatus, expectedBuildNumber, expectedTimestamp, expectedCompletionPercentage, comments,
                                                                               true, expectedTestResults, 0);
        given(jenkinsFacade.getLatestBuildInformation(JOB_NAME, CURRENT_JOB_STATUS)).willReturn(Result.success(buildUpdate1),
                                                                                                Result.success(buildUpdate2));

        //When
        jobUpdater.run();
        jobUpdater.run();

        //Then
        verify(messageBus, times(2)).sendUpdate(feedbackJob);
        verifyZeroInteractions(jobTestResultsDao);
        assertJob(feedbackJob, expectedRevision, expectedStatus, expectedBuildNumber, expectedTimestamp, expectedCompletionPercentage, comments, expectedTestResults);
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