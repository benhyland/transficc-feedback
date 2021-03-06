package com.transficc.tools.feedback;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.transficc.functionality.Result;
import com.transficc.tools.feedback.ci.ContinuousIntegrationServer;
import com.transficc.tools.feedback.ci.JobService;
import com.transficc.tools.feedback.domain.Job;
import com.transficc.tools.feedback.domain.JobStatus;
import com.transficc.tools.feedback.domain.VersionControl;
import com.transficc.tools.feedback.web.messaging.MessageBus;
import com.transficc.tools.feedback.web.messaging.PublishableJob;
import com.transficc.tools.feedback.web.routes.websocket.OutboundWebSocketFrame;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

public class JobServiceTest
{
    private final ScheduledExecutorService scheduledExecutorService = Mockito.mock(ScheduledExecutorService.class);
    @SuppressWarnings("rawtypes")
    private final ScheduledFuture scheduledFuture = Mockito.mock(ScheduledFuture.class);
    private final LinkedBlockingQueue<OutboundWebSocketFrame> messageBusQueue = new LinkedBlockingQueue<>();
    private final MessageBus messageBus = new MessageBus(messageBusQueue);
    private final ContinuousIntegrationServer continuousIntegrationServer = Mockito.mock(ContinuousIntegrationServer.class);
    private final JobRepository jobRepository = new JobRepository(Collections.emptyMap());
    private final JobService jobService = new JobService(jobRepository, messageBus, scheduledExecutorService, continuousIntegrationServer, "job");

    @SuppressWarnings("unchecked")
    @Before
    public void setup()
    {
        BDDMockito.given(scheduledExecutorService.scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class))).willReturn(scheduledFuture);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldOnlyAddEachJobOnce()
    {
        BDDMockito.given(continuousIntegrationServer.getAllJobs())
                .willReturn(Result.success(Collections.singletonList(new Job("Tom", "stuff.com", JobStatus.DISABLED, VersionControl.GIT))),
                            Result.success(Collections.singletonList(new Job("Tom", "stuff.com", JobStatus.DISABLED, VersionControl.GIT))));

        jobService.run();
        jobService.run();

        final List<PublishableJob> publishableJobs = jobRepository.getPublishableJobs();

        MatcherAssert.assertThat(publishableJobs.size(), Is.is(1));
        MatcherAssert.assertThat(publishableJobs.get(0).getName(), Is.is("Tom"));
        MatcherAssert.assertThat(publishableJobs.get(0).getUrl(), Is.is("stuff.com"));
        MatcherAssert.assertThat(publishableJobs.get(0).getJobStatus(), Is.is(JobStatus.DISABLED));

    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldAddJobAsTheyAreCreated()
    {
        BDDMockito.given(continuousIntegrationServer.getAllJobs())
                .willReturn(Result.success(Collections.singletonList(new Job("Tom", "stuff.com", JobStatus.DISABLED, VersionControl.GIT))),
                            Result.success(Arrays.asList(new Job("Tom", "stuff.com", JobStatus.DISABLED, VersionControl.GIT),
                                                         new Job("Chinar", "stuff.com", JobStatus.DISABLED, VersionControl.GIT))));

        jobService.run();
        jobService.run();

        final List<PublishableJob> publishableJobs = jobRepository.getPublishableJobs();

        MatcherAssert.assertThat(publishableJobs.size(), Is.is(2));
        MatcherAssert.assertThat(publishableJobs.get(0).getName(), Is.is("Chinar"));
        MatcherAssert.assertThat(publishableJobs.get(0).getUrl(), Is.is("stuff.com"));
        MatcherAssert.assertThat(publishableJobs.get(0).getJobStatus(), Is.is(JobStatus.DISABLED));
        MatcherAssert.assertThat(publishableJobs.get(1).getName(), Is.is("Tom"));
        MatcherAssert.assertThat(publishableJobs.get(1).getUrl(), Is.is("stuff.com"));
        MatcherAssert.assertThat(publishableJobs.get(1).getJobStatus(), Is.is(JobStatus.DISABLED));
    }
}