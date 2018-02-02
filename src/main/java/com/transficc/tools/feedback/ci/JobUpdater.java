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

import com.transficc.functionality.Result;
import com.transficc.tools.feedback.JobRepository;
import com.transficc.tools.feedback.ci.jenkins.JenkinsFacade;
import com.transficc.tools.feedback.domain.LatestBuildInformation;
import com.transficc.tools.feedback.web.messaging.MessageBus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class JobUpdater implements Runnable
{
    private static final Logger LOGGER = LoggerFactory.getLogger(JobUpdater.class);
    private final JenkinsFacade jenkinsFacade;
    private final MessageBus messageBus;
    private final JobRepository jobRepository;

    JobUpdater(final JenkinsFacade jenkinsFacade,
               final MessageBus messageBus,
               final JobRepository jobRepository)
    {
        this.jenkinsFacade = jenkinsFacade;
        this.messageBus = messageBus;
        this.jobRepository = jobRepository;
    }

    @Override
    public void run()
    {
        for (final FeedbackJob job : jobRepository.getAllJobs())
        {
            try
            {
                final Result<Integer, LatestBuildInformation> latestBuildInformation = jenkinsFacade.getLatestBuildInformation(job.getName(), job.getJobStatus());
                latestBuildInformation.consume(statusCode -> handleErrorStatus(job, statusCode),
                                               buildInformation ->
                                               {
                                                   if (job.wasUpdated(buildInformation))
                                                   {
                                                       messageBus.sendUpdate(job);
                                                   }
                                               });
            }
            catch (final RuntimeException e)
            {
                LOGGER.error("An exception occurred whilst trying to gather build information", e);
            }
        }
    }

    private void handleErrorStatus(final FeedbackJob job, final Integer statusCode)
    {
        if (statusCode == 404)
        {
            jobRepository.remove(job.getName());
            messageBus.jobRemoved(job.getName());
        }
        else
        {
            LOGGER.error("Received status code {} whilst trying to get build information for job: {}", statusCode, job.getName());
        }
    }
}
