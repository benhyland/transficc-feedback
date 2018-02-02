/*
 * Copyright 2017 TransFICC Ltd.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.  The ASF licenses this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the specific language governing permissions and limitations under the License.
 */
package com.transficc.tools.feedback.ci;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.transficc.functionality.Result;
import com.transficc.tools.feedback.JobRepository;
import com.transficc.tools.feedback.domain.Job;
import com.transficc.tools.feedback.web.messaging.MessageBus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobService implements Runnable
{
    private static final Logger LOGGER = LoggerFactory.getLogger(JobService.class);
    private final JobRepository jobRepository;
    private final ContinuousIntegrationServer continuousIntegrationServer;

    public JobService(final JobRepository jobRepository,
                      final MessageBus messageBus,
                      final ScheduledExecutorService scheduledExecutorService,
                      final ContinuousIntegrationServer continuousIntegrationServer)
    {
        this.jobRepository = jobRepository;
        this.continuousIntegrationServer = continuousIntegrationServer;
        final JobUpdater jobUpdaterRunnable = new JobUpdater(continuousIntegrationServer, messageBus, jobRepository);
        scheduledExecutorService.scheduleAtFixedRate(jobUpdaterRunnable, 0, 5, TimeUnit.SECONDS);
    }

    @Override
    public void run()
    {
        final Result<Integer, List<Job>> result = continuousIntegrationServer.getAllJobs();
        result.consume(statusCode -> LOGGER.error("Received status code {} when trying to obtain jobs", statusCode),
                       jobs -> jobs.stream()
                               .filter(job -> !jobExists(job.getName()))
                               .forEach(job -> add(new FeedbackJob(jobRepository.getPriorityForJob(job.getName()), job))));
    }

    private void add(final FeedbackJob job)
    {
        jobRepository.add(job);
    }

    private boolean jobExists(final String jobName)
    {
        return jobRepository.contains(jobName);
    }
}
