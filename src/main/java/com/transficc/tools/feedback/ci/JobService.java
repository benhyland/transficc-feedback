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

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.transficc.tools.feedback.JobRepository;
import com.transficc.tools.feedback.ci.jenkins.JenkinsFacade;
import com.transficc.tools.feedback.dao.JobTestResultsDao;
import com.transficc.tools.feedback.web.messaging.MessageBus;

public class JobService
{
    private final JobRepository jobRepository;
    private final CopyOnWriteArrayList<FeedbackJob> jobs;

    public JobService(final JobRepository jobRepository,
                      final MessageBus messageBus,
                      final ScheduledExecutorService scheduledExecutorService,
                      final JenkinsFacade jenkinsFacade,
                      final JobTestResultsDao jobTestResultsDao)
    {
        this.jobRepository = jobRepository;
        jobs = new CopyOnWriteArrayList<>();
        final JobUpdater jobUpdaterRunnable = new JobUpdater(jenkinsFacade, jobTestResultsDao, jobs, messageBus, jobRepository);
        scheduledExecutorService.scheduleAtFixedRate(jobUpdaterRunnable, 0, 5, TimeUnit.SECONDS);
    }

    public void add(final FeedbackJob job)
    {
        jobs.add(job);
        final String jobName = job.getName();
        jobRepository.put(jobName, job);
    }

    public boolean jobExists(final String jobName)
    {
        return jobRepository.contains(jobName);
    }

}
