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
package com.transficc.tools.feedback;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.transficc.tools.feedback.dao.JobTestResultsDao;
import com.transficc.tools.feedback.domain.Job;
import com.transficc.tools.feedback.jenkins.JenkinsFacade;
import com.transficc.tools.feedback.messaging.MessageBus;

public class JobService
{
    private final Map<String, ScheduledFuture<?>> jobNameToScheduledRunnable = new ConcurrentHashMap<>();
    private final JobRepository jobRepository;
    private final MessageBus messageBus;
    private final ScheduledExecutorService scheduledExecutorService;
    private final JenkinsFacade jenkinsFacade;
    private final JobTestResultsDao jobTestResultsDao;
    private final Set<String> jobNamesForTestResultsToPersist;

    JobService(final JobRepository jobRepository,
               final MessageBus messageBus,
               final ScheduledExecutorService scheduledExecutorService,
               final JenkinsFacade jenkinsFacade,
               final String[] jobNamesForTestResultsToPersist, final JobTestResultsDao jobTestResultsDao)
    {
        this.jobRepository = jobRepository;
        this.messageBus = messageBus;
        this.scheduledExecutorService = scheduledExecutorService;
        this.jenkinsFacade = jenkinsFacade;
        this.jobNamesForTestResultsToPersist = new HashSet<>(jobNamesForTestResultsToPersist.length);
        this.jobTestResultsDao = jobTestResultsDao;
        Collections.addAll(this.jobNamesForTestResultsToPersist, jobNamesForTestResultsToPersist);
    }

    public void onJobNotFound(final String jobName)
    {
        final ScheduledFuture<?> future = jobNameToScheduledRunnable.remove(jobName);
        if (future != null)
        {
            future.cancel(true);
        }
        jobRepository.remove(jobName);
        messageBus.jobRemoved(jobName);
    }

    public void add(final Job job)
    {
        final GetLatestJobBuildInformation statusChecker = new GetLatestJobBuildInformation(messageBus, this, job, jenkinsFacade, jobNamesForTestResultsToPersist.contains(job.getName()),
                                                                                            jobTestResultsDao);
        final String jobName = job.getName();
        jobRepository.put(jobName, job);
        final ScheduledFuture<?> scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(statusChecker, 0, 5, TimeUnit.SECONDS);
        jobNameToScheduledRunnable.put(jobName, scheduledFuture);
    }

    public boolean jobExists(final String jobName)
    {
        return jobRepository.contains(jobName);
    }
}
