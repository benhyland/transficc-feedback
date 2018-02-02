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
import java.util.Set;

import com.transficc.functionality.Result;
import com.transficc.tools.feedback.ci.jenkins.JenkinsFacade;
import com.transficc.tools.feedback.domain.Job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobFinder implements Runnable
{
    private static final Logger LOGGER = LoggerFactory.getLogger(JobFinder.class);
    private final JobService jobService;
    private final JenkinsFacade jenkinsFacade;
    private final JobPrioritiesRepository jobPrioritiesRepository;
    private final Set<String> jobNamesForTestResultsToPersist;

    public JobFinder(final JobService jobService,
                     final JenkinsFacade jenkinsFacade,
                     final JobPrioritiesRepository jobPrioritiesRepository,
                     final Set<String> jobNamesForTestResultsToPersist)
    {
        this.jobService = jobService;
        this.jenkinsFacade = jenkinsFacade;
        this.jobPrioritiesRepository = jobPrioritiesRepository;
        this.jobNamesForTestResultsToPersist = jobNamesForTestResultsToPersist;
    }

    @Override
    public void run()
    {
        final Result<Integer, List<Job>> result = jenkinsFacade.getAllJobs(name -> !jobService.jobExists(name));
        result.consume(statusCode -> LOGGER.error("Received status code {} when trying to obtain jobs", statusCode),
                       jobs -> jobs.forEach(job ->
                                            {
                                                jobService.add(new FeedbackJob(jobPrioritiesRepository.getPriorityForJob(job.getName()),
                                                                               jobNamesForTestResultsToPersist.contains(job.getName()), job));
                                            }));
    }
}
