package com.transficc.tools.feedback;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Job;
import com.transficc.tools.feedback.ci.JobService;
import com.transficc.tools.feedback.ci.jenkins.JenkinsFacade;
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
    private final JenkinsServer jenkins = Mockito.mock(JenkinsServer.class);
    private final ScheduledExecutorService scheduledExecutorService = Mockito.mock(ScheduledExecutorService.class);
    @SuppressWarnings("rawtypes")
    private final ScheduledFuture scheduledFuture = Mockito.mock(ScheduledFuture.class);
    private final LinkedBlockingQueue<OutboundWebSocketFrame> messageBusQueue = new LinkedBlockingQueue<>();
    private final MessageBus messageBus = new MessageBus(messageBusQueue);
    private final JenkinsFacade jenkinsFacade = new JenkinsFacade(jenkins, "", () -> 10, VersionControl.GIT);
    private final JobRepository jobRepository = new JobRepository(Collections.emptyMap());
    private final JobService jobService = new JobService(jobRepository, messageBus, scheduledExecutorService,
                                                         jenkinsFacade);

    @SuppressWarnings("unchecked")
    @Before
    public void setup()
    {
        BDDMockito.given(scheduledExecutorService.scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class))).willReturn(scheduledFuture);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldOnlyAddEachJobOnce() throws IOException
    {
        final Map<String, Job> result1 = new HashMap<>();
        result1.put("Tom", new MessageBuilder<>(com.offbytwo.jenkins.model.Job.class).setField("name", "Tom").setField("url", "stuff.com").build());
        result1.put("Chinar", new MessageBuilder<>(com.offbytwo.jenkins.model.Job.class).setField("name", "Chinar").setField("url", "stuff.com").build());
        final Map<String, com.offbytwo.jenkins.model.Job> result2 = new HashMap<>();
        result2.put("Tom", new MessageBuilder<>(com.offbytwo.jenkins.model.Job.class).setField("name", "Tom").setField("url", "stuff.com").build());
        BDDMockito.given(jenkins.getJobs()).willReturn(result1, result2);

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

    @Test
    @SuppressWarnings("unchecked")
    public void shouldAddJobAsTheyAreCreated() throws IOException
    {
        final Map<String, com.offbytwo.jenkins.model.Job> result1 = new HashMap<>();
        result1.put("Tom", new MessageBuilder<>(com.offbytwo.jenkins.model.Job.class).setField("name", "Chinar").setField("url", "stuff.com").build());
        final Map<String, com.offbytwo.jenkins.model.Job> result2 = new HashMap<>();
        result2.put("Tom", new MessageBuilder<>(com.offbytwo.jenkins.model.Job.class).setField("name", "Tom").setField("url", "stuff.com").build());
        BDDMockito.given(jenkins.getJobs()).willReturn(result1, result2);

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