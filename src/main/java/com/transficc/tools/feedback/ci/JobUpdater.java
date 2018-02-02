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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

import com.transficc.functionality.Result;
import com.transficc.tools.feedback.JobRepository;
import com.transficc.tools.feedback.ci.jenkins.JenkinsFacade;
import com.transficc.tools.feedback.dao.JobTestResultsDao;
import com.transficc.tools.feedback.domain.LatestBuildInformation;
import com.transficc.tools.feedback.domain.TestResults;
import com.transficc.tools.feedback.web.messaging.MessageBus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class JobUpdater implements Runnable
{
    private static final Logger LOGGER = LoggerFactory.getLogger(JobUpdater.class);
    private final JenkinsFacade jenkinsFacade;
    private final JobTestResultsDao jobTestResultsDao;
    private final CopyOnWriteArrayList<FeedbackJob> jobs;
    private final MessageBus messageBus;
    private final JobRepository jobRepository;

    JobUpdater(final JenkinsFacade jenkinsFacade,
               final JobTestResultsDao jobTestResultsDao,
               final CopyOnWriteArrayList<FeedbackJob> jobs,
               final MessageBus messageBus,
               final JobRepository jobRepository)
    {
        this.jenkinsFacade = jenkinsFacade;
        this.jobTestResultsDao = jobTestResultsDao;
        this.jobs = jobs;
        this.messageBus = messageBus;
        this.jobRepository = jobRepository;
    }

    @Override
    public void run()
    {
        final Iterator<FeedbackJob> iterator = jobs.iterator();
        while (iterator.hasNext())
        {
            final FeedbackJob job = iterator.next();
            try
            {
                final Result<Integer, LatestBuildInformation> latestBuildInformation = jenkinsFacade.getLatestBuildInformation(job.getName(), job.getJobStatus());
                latestBuildInformation.consume(statusCode -> handleErrorStatus(iterator, job, statusCode),
                                               buildInformation ->
                                               {
                                                   if (job.wasUpdated(buildInformation))
                                                   {
                                                       messageBus.sendUpdate(job);
                                                   }

                                                   if (job.hasJustCompleted() && job.shouldPersistTestResults())
                                                   {
                                                       recordJobInformation(job, buildInformation);
                                                   }
                                               });
            }
            catch (final RuntimeException e)
            {
                LOGGER.error("An exception occurred whilst trying to gather build information", e);
            }
        }
    }

    private void recordJobInformation(final FeedbackJob job, final LatestBuildInformation buildInformation)
    {
        final TestResults testResults = buildInformation.getTestResults();
        final int total = testResults.getFailCount() + testResults.getPassCount() + testResults.getSkipCount();
        final ZonedDateTime startTime = ZonedDateTime.of(LocalDateTime.ofInstant(Instant.ofEpochMilli(buildInformation.getTimestamp()),
                                                                                 ZoneOffset.UTC), ZoneOffset.UTC);
        jobTestResultsDao.addTestResults(job.getName(),
                                         buildInformation.getRevision(),
                                         total,
                                         testResults.getPassCount(),
                                         testResults.getFailCount(),
                                         startTime,
                                         buildInformation.getDuration());
    }

    private void handleErrorStatus(final Iterator<FeedbackJob> iterator, final FeedbackJob job, final Integer statusCode)
    {
        if (statusCode == 404)
        {
            jobRepository.remove(job.getName());
            messageBus.jobRemoved(job.getName());
            iterator.remove();
        }
        else
        {
            LOGGER.error("Received status code {} whilst trying to get build information for job: {}", statusCode, job.getName());
        }
    }
}
